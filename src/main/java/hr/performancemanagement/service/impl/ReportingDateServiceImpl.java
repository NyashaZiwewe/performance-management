package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.repository.ReportingPeriodRepository;
import hr.performancemanagement.repository.ScorecardReportingDateStageRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Service
public class ReportingDateServiceImpl implements hr.performancemanagement.service.api.ReportingDateService {
    @Autowired
    ReportingDateRepository reportingDateRepository;
    @Autowired
    ReportingPeriodRepository reportingPeriodRepository;
    @Autowired
    ScorecardReportingDateStageRepository scorecardReportingDateStageRepository;
    @Autowired
    CommonService cs;
    @Autowired
    ScorecardLifecycleService scorecardLifecycleService;
    @Autowired
    ReportingDateActivityPeriodService reportingDateActivityPeriodService;

    @Override
    public ReportingDate getReportingDateById(long id){

        ReportingDate reportingDate = reportingDateRepository.findReportingDateById(id);
        return reportingDate;
    }

    @Override
    public ReportingDate getActiveReportingDate(){
        Account loggedUser = cs.getLoggedUser();
        if (loggedUser == null || loggedUser.getClientId() <= 0) {
            return null;
        }
        List<ReportingDate> activeReportingDates = listOpenOrActiveReportingDates(loggedUser.getClientId());
        if (activeReportingDates.size() != 1) {
            return null;
        }
        return activeReportingDates.get(0);
    }

    @Override
    public List<ReportingDate> listOpenOrActiveReportingDates(long clientId) {
        if (clientId <= 0) {
            return Collections.emptyList();
        }
        List<ReportingDate> reportingDates = listOpenOrActiveReportingDatesForClient(clientId);
        List<ReportingDate> validReportingDates = new ArrayList<>();
        for (ReportingDate reportingDate : reportingDates) {
            if (isCurrentActiveReportingPeriod(reportingDate == null ? null : reportingDate.getReportingPeriod())) {
                validReportingDates.add(reportingDate);
            }
        }
        return validReportingDates;
    }

    @Override
    public boolean hasMultipleOpenOrActiveReportingDates(long clientId) {
        if (clientId <= 0) {
            return false;
        }
        return listOpenOrActiveReportingDates(clientId).size() > 1;
    }

    @Override
    public void validateSingleOpenOrActiveReportingDate(long clientId) {
        if (clientId <= 0) {
            return;
        }
        List<ReportingDate> reportingDates = listOpenOrActiveReportingDatesForClient(clientId);
        if (reportingDates.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple OPEN/ACTIVE reporting dates were detected: "
                            + summarizeReportingDates(reportingDates)
                            + ". Only one reporting date may be OPEN/ACTIVE."
            );
        }
    }

    @Override
    public boolean isReportingDateOpen(ReportingDate reportingDate) {
        return reportingDate != null
                && reportingDate.getStatus() != null
                && PMConstants.REPORTING_DATE_STATUS_OPEN.equalsIgnoreCase(reportingDate.getStatus().trim());
    }

    @Override
    @Transactional
    public List<ReportingDate> listAllReportingDates(ReportingPeriod reportingPeriod){
        List<ReportingDate> reportingDateList = new ArrayList<>();
        reportingDateRepository.findReportingDatesByReportingPeriod(reportingPeriod).forEach(reportingDate -> reportingDateList.add(reportingDate));
        closeInvalidOpenReportingDates(reportingDateList);
        return reportingDateList;
    }

    @Override
    public long countReportingDates(long reportingPeriodId) {
        return reportingDateRepository.countByReportingPeriod_Id(reportingPeriodId);
    }

    private void closeOpenReportingDates(ReportingPeriod reportingPeriod, long keepId){
        List<ReportingDate> reportingDateList = reportingDateRepository.findReportingDatesByReportingPeriodAndStatusIn(
                reportingPeriod,
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        );
        closeReportingDates(reportingDateList, keepId);
    }

    private void closeOpenReportingDatesForClient(ReportingPeriod reportingPeriod, long keepId) {
        if (reportingPeriod == null || reportingPeriod.getClientId() <= 0) {
            closeOpenReportingDates(reportingPeriod, keepId);
            return;
        }
        List<ReportingDate> reportingDateList =
                listOpenOrActiveReportingDatesForClient(reportingPeriod.getClientId());
        closeReportingDates(reportingDateList, keepId);
    }

    private List<ReportingDate> listOpenOrActiveReportingDatesForClient(long clientId) {
        if (clientId <= 0) {
            return Collections.emptyList();
        }
        List<ReportingDate> reportingDates =
                reportingDateRepository.findReportingDatesByReportingPeriod_ClientIdAndStatusInOrderByDateDescIdDesc(
                        clientId,
                        Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
                );
        return reportingDates == null ? Collections.<ReportingDate>emptyList() : reportingDates;
    }

    private void closeReportingDates(List<ReportingDate> reportingDateList, long keepId) {
        if (reportingDateList == null || reportingDateList.isEmpty()) {
            return;
        }
        for(ReportingDate reportingDate: reportingDateList){
            if (keepId > 0 && reportingDate.getId() == keepId) {
                continue;
            }
            reportingDate.setStatus(PMConstants.REPORTING_DATE_STATUS_CLOSED);
            reportingDateRepository.save(reportingDate);
            scorecardLifecycleService.closeScorecardReportingDateStages(reportingDate);
        }
    }

    private String summarizeReportingDates(List<ReportingDate> reportingDates) {
        if (reportingDates == null || reportingDates.isEmpty()) {
            return "none";
        }
        StringBuilder summary = new StringBuilder();
        int displayed = 0;
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate == null) {
                continue;
            }
            if (displayed > 0) {
                summary.append("; ");
            }
            summary.append(formatReportingDate(reportingDate));
            displayed++;
            if (displayed == 3 && reportingDates.size() > displayed) {
                summary.append("; and ").append(reportingDates.size() - displayed).append(" more");
                break;
            }
        }
        return summary.length() == 0 ? "none" : summary.toString();
    }

    private String formatReportingDate(ReportingDate reportingDate) {
        String endDate = StringUtils.hasText(reportingDate.getEndDate())
                ? reportingDate.getEndDate().trim()
                : "missing end date";
        ReportingPeriod reportingPeriod = reportingDate.getReportingPeriod();
        String period = reportingPeriod == null
                ? ""
                : ", period ID " + reportingPeriod.getId();
        return endDate + " (ID " + reportingDate.getId() + period + ")";
    }

    @Override
    @Transactional
    public void deleteReportingDate(ReportingDate reportingDate) {
        if (reportingDate == null || reportingDate.getId() <= 0) {
            throw new IllegalArgumentException("Reporting date is required.");
        }
        ReportingDate resolvedReportingDate = reportingDateRepository.findReportingDateById(reportingDate.getId());
        if (resolvedReportingDate == null) {
            throw new IllegalArgumentException("Reporting date could not be found.");
        }

        scorecardLifecycleService.closeScorecardReportingDateStages(resolvedReportingDate);
        scorecardReportingDateStageRepository.deleteScorecardReportingDateStagesByReportingDate(resolvedReportingDate);
        reportingDateActivityPeriodService.deleteActivityPeriods(resolvedReportingDate);
        reportingDateRepository.delete(resolvedReportingDate);
    }

    @Override
    @Transactional
    public void saveReportingDate(ReportingDate reportingDate) {
        if (reportingDate == null) {
            throw new IllegalArgumentException("Reporting date is required.");
        }
        String status = normalizeStatus(reportingDate.getStatus());
        reportingDate.setStatus(status);
        ReportingPeriod reportingPeriod = resolveReportingPeriod(reportingDate.getReportingPeriod());
        reportingDate.setReportingPeriod(reportingPeriod);

        if(PMConstants.REPORTING_DATE_STATUS_OPEN.equalsIgnoreCase(status)){
            validateOpenReportingDatePeriod(reportingPeriod);
            closeOpenReportingDatesForClient(reportingPeriod, reportingDate.getId());
        }
        ReportingDate savedReportingDate = reportingDateRepository.save(reportingDate);
        if (!PMConstants.REPORTING_DATE_STATUS_OPEN.equalsIgnoreCase(status)) {
            scorecardLifecycleService.closeScorecardReportingDateStages(savedReportingDate);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return PMConstants.REPORTING_DATE_STATUS_CLOSED;
        }
        String value = status.trim().toUpperCase();
        if (PMConstants.STATUS_ACTIVE.equals(value)) {
            return PMConstants.REPORTING_DATE_STATUS_OPEN;
        }
        if (PMConstants.STATUS_IN_ACTIVE.equals(value)) {
            return PMConstants.REPORTING_DATE_STATUS_CLOSED;
        }
        if (PMConstants.REPORTING_DATE_STATUS_OPEN.equals(value)) {
            return PMConstants.REPORTING_DATE_STATUS_OPEN;
        }
        return PMConstants.REPORTING_DATE_STATUS_CLOSED;
    }

    private ReportingPeriod resolveReportingPeriod(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null || reportingPeriod.getId() <= 0) {
            throw new IllegalArgumentException("Reporting period is required for a reporting date.");
        }
        ReportingPeriod resolvedReportingPeriod = reportingPeriodRepository.findReportingPeriodById(reportingPeriod.getId());
        if (resolvedReportingPeriod == null) {
            throw new IllegalArgumentException("Reporting period could not be found.");
        }
        return resolvedReportingPeriod;
    }

    private void validateOpenReportingDatePeriod(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null
                || reportingPeriod.getStatus() == null
                || !PMConstants.STATUS_ACTIVE.equalsIgnoreCase(reportingPeriod.getStatus().trim())) {
            throw new IllegalArgumentException("A reporting date can only be opened under an active reporting period.");
        }
        validatePeriodIncludesToday(reportingPeriod);
    }

    private void closeInvalidOpenReportingDates(List<ReportingDate> reportingDates) {
        if (reportingDates == null || reportingDates.isEmpty()) {
            return;
        }
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate == null || !isOpenOrActiveStatus(reportingDate.getStatus())) {
                continue;
            }
            if (isCurrentActiveReportingPeriod(reportingDate.getReportingPeriod())) {
                continue;
            }
            reportingDate.setStatus(PMConstants.REPORTING_DATE_STATUS_CLOSED);
            reportingDateRepository.save(reportingDate);
            scorecardLifecycleService.closeScorecardReportingDateStages(reportingDate);
        }
    }

    private boolean isOpenOrActiveStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return PMConstants.REPORTING_DATE_STATUS_OPEN.equals(normalized)
                || PMConstants.STATUS_ACTIVE.equals(normalized);
    }

    private boolean isCurrentActiveReportingPeriod(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null
                || reportingPeriod.getStatus() == null
                || !PMConstants.STATUS_ACTIVE.equalsIgnoreCase(reportingPeriod.getStatus().trim())) {
            return false;
        }
        try {
            validatePeriodIncludesToday(reportingPeriod);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void validatePeriodIncludesToday(ReportingPeriod reportingPeriod) {
        LocalDate startDate = parsePeriodDate(reportingPeriod.getStartDate(), "start");
        LocalDate endDate = parsePeriodDate(reportingPeriod.getEndDate(), "end");
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Reporting period start date cannot be after the end date.");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate) || today.isAfter(endDate)) {
            throw new IllegalArgumentException("A reporting date can only be opened when its active reporting period includes today's date (" + today + ").");
        }
    }

    private LocalDate parsePeriodDate(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Reporting period " + label + " date is required.");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Reporting period " + label + " date must be in YYYY-MM-DD format.");
        }
    }
}
