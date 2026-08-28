package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.repository.ReportingPeriodRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Service
public class ReportingPeriodServiceImpl implements hr.performancemanagement.service.api.ReportingPeriodService {

    @Autowired
    ReportingPeriodRepository reportingPeriodRepository;
    @Autowired
    ReportingDateRepository reportingDateRepository;
    @Autowired
    CommonService cs;
    @Autowired
    ScorecardLifecycleService scorecardLifecycleService;

    @Override
    public ReportingPeriod getReportingPeriodById(long id){

        ReportingPeriod reportingPeriod = reportingPeriodRepository.findReportingPeriodById(id);
        return reportingPeriod;
    }

    @Override
    public ReportingPeriod getActiveReportingPeriod(){
        long clientId = cs.getConfiguredClientId();
        List<ReportingPeriod> activeReportingPeriods = listActiveReportingPeriodsForClientOrFallback(clientId);
        if (activeReportingPeriods.size() != 1) {
            return null;
        }
        ReportingPeriod reportingPeriod = activeReportingPeriods.get(0);
        return isCurrentReportingPeriod(reportingPeriod) ? reportingPeriod : null;
    }

    @Override
    public void validateSingleActiveReportingPeriod(long clientId) {
        List<ReportingPeriod> activeReportingPeriods = listActiveReportingPeriodsForClientOrFallback(clientId);
        if (activeReportingPeriods.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple active reporting periods were detected: "
                            + summarizeReportingPeriods(activeReportingPeriods)
                            + ". Only one reporting period may be ACTIVE."
            );
        }
    }

    @Override
    public List<ReportingPeriod> listAllReportingPeriods(){
        List<ReportingPeriod> reportingPeriodList = new ArrayList<>();
        long clientId = cs.getConfiguredClientId();
        reportingPeriodRepository.findAllReportingPeriodsByClientId(clientId).forEach(reportingPeriodList::add);
        if (reportingPeriodList.isEmpty()) {
            reportingPeriodRepository.findAll().forEach(reportingPeriodList::add);
        }
        return reportingPeriodList;
    }

    @Override
    public List<ReportingPeriod> listAllReportingPeriods(long clientId){
        List<ReportingPeriod> reportingPeriodList = new ArrayList<>();
        long resolvedClientId = clientId > 0 ? clientId : cs.getConfiguredClientId();
        reportingPeriodRepository.findAllReportingPeriodsByClientId(resolvedClientId).forEach(reportingPeriodList::add);
        if (reportingPeriodList.isEmpty()) {
            reportingPeriodRepository.findAll().forEach(reportingPeriodList::add);
        }
        return reportingPeriodList;
    }

    @Override
    @Transactional
    public void saveReportingPeriod(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null) {
            throw new IllegalArgumentException("Reporting period is required.");
        }
        if (!StringUtils.hasText(reportingPeriod.getModel())) {
            reportingPeriod.setModel("standard");
        } else {
            reportingPeriod.setModel(reportingPeriod.getModel().trim().toLowerCase());
        }
        reportingPeriod.setStatus(normalizeStatus(reportingPeriod.getStatus()));
        if (PMConstants.STATUS_ACTIVE.equalsIgnoreCase(reportingPeriod.getStatus())) {
            validatePeriodIncludesToday(reportingPeriod);
            validateNoOtherActiveReportingPeriod(reportingPeriod);
        }
        ReportingPeriod savedReportingPeriod = reportingPeriodRepository.save(reportingPeriod);
        if (!PMConstants.STATUS_ACTIVE.equalsIgnoreCase(savedReportingPeriod.getStatus())) {
            closeOpenOrActiveReportingDates(savedReportingPeriod);
            scorecardLifecycleService.closeScorecardsForInactiveReportingPeriod(savedReportingPeriod);
        }
    }

    @Transactional
    @Override
    public void deleteReportingPeriod(ReportingPeriod reportingPeriod) {
        reportingPeriodRepository.delete(reportingPeriod);
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return PMConstants.STATUS_IN_ACTIVE;
        }
        String normalized = status.trim().toUpperCase(Locale.ENGLISH);
        if ("INACTIVE".equals(normalized)) {
            return PMConstants.STATUS_IN_ACTIVE;
        }
        return normalized;
    }

    private void validateNoOtherActiveReportingPeriod(ReportingPeriod reportingPeriod) {
        long clientId = reportingPeriod.getClientId() > 0
                ? reportingPeriod.getClientId()
                : cs.getConfiguredClientId();
        List<ReportingPeriod> activeReportingPeriods = listActiveReportingPeriodsForClient(clientId);
        List<ReportingPeriod> conflictingReportingPeriods = new ArrayList<>();
        for (ReportingPeriod activeReportingPeriod : activeReportingPeriods) {
            if (activeReportingPeriod == null) {
                continue;
            }
            if (reportingPeriod.getId() > 0 && activeReportingPeriod.getId() == reportingPeriod.getId()) {
                continue;
            }
            conflictingReportingPeriods.add(activeReportingPeriod);
        }
        if (!conflictingReportingPeriods.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot activate this reporting period because another active reporting period already exists: "
                            + summarizeReportingPeriods(conflictingReportingPeriods)
                            + ". Deactivate the existing active reporting period before activating another one."
            );
        }
    }

    private List<ReportingPeriod> listActiveReportingPeriodsForClientOrFallback(long clientId) {
        long resolvedClientId = clientId > 0 ? clientId : cs.getConfiguredClientId();
        List<ReportingPeriod> activeReportingPeriods = listActiveReportingPeriodsForClient(resolvedClientId);
        if (activeReportingPeriods.isEmpty()) {
            activeReportingPeriods = reportingPeriodRepository.findReportingPeriodsByStatusOrderByStartDateDescEndDateDescIdDesc(
                    PMConstants.STATUS_ACTIVE
            );
        }
        return activeReportingPeriods == null ? new ArrayList<ReportingPeriod>() : activeReportingPeriods;
    }

    private List<ReportingPeriod> listActiveReportingPeriodsForClient(long clientId) {
        if (clientId <= 0) {
            return new ArrayList<ReportingPeriod>();
        }
        List<ReportingPeriod> activeReportingPeriods =
                reportingPeriodRepository.findReportingPeriodsByClientIdAndStatusOrderByStartDateDescEndDateDescIdDesc(
                        clientId,
                        PMConstants.STATUS_ACTIVE
                );
        return activeReportingPeriods == null ? new ArrayList<ReportingPeriod>() : activeReportingPeriods;
    }

    private String summarizeReportingPeriods(List<ReportingPeriod> reportingPeriods) {
        if (reportingPeriods == null || reportingPeriods.isEmpty()) {
            return "none";
        }
        StringBuilder summary = new StringBuilder();
        int displayed = 0;
        for (ReportingPeriod reportingPeriod : reportingPeriods) {
            if (reportingPeriod == null) {
                continue;
            }
            if (displayed > 0) {
                summary.append("; ");
            }
            summary.append(formatReportingPeriod(reportingPeriod));
            displayed++;
            if (displayed == 3 && reportingPeriods.size() > displayed) {
                summary.append("; and ").append(reportingPeriods.size() - displayed).append(" more");
                break;
            }
        }
        return summary.length() == 0 ? "none" : summary.toString();
    }

    private String formatReportingPeriod(ReportingPeriod reportingPeriod) {
        String startDate = StringUtils.hasText(reportingPeriod.getStartDate())
                ? reportingPeriod.getStartDate().trim()
                : "missing start";
        String endDate = StringUtils.hasText(reportingPeriod.getEndDate())
                ? reportingPeriod.getEndDate().trim()
                : "missing end";
        String client = reportingPeriod.getClientId() > 0
                ? ", client " + reportingPeriod.getClientId()
                : "";
        return startDate + " - " + endDate + " (ID " + reportingPeriod.getId() + client + ")";
    }

    private void validatePeriodIncludesToday(ReportingPeriod reportingPeriod) {
        LocalDate startDate = parsePeriodDate(reportingPeriod.getStartDate(), "start");
        LocalDate endDate = parsePeriodDate(reportingPeriod.getEndDate(), "end");
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Reporting period start date cannot be after the end date.");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate) || today.isAfter(endDate)) {
            throw new IllegalArgumentException("An active reporting period must include today's date (" + today + ").");
        }
    }

    private boolean isCurrentReportingPeriod(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null
                || reportingPeriod.getStatus() == null
                || !PMConstants.STATUS_ACTIVE.equalsIgnoreCase(reportingPeriod.getStatus().trim())) {
            return false;
        }
        try {
            LocalDate startDate = parsePeriodDate(reportingPeriod.getStartDate(), "start");
            LocalDate endDate = parsePeriodDate(reportingPeriod.getEndDate(), "end");
            LocalDate today = LocalDate.now();
            return !startDate.isAfter(endDate) && !today.isBefore(startDate) && !today.isAfter(endDate);
        } catch (IllegalArgumentException exception) {
            return false;
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

    private void closeOpenOrActiveReportingDates(ReportingPeriod reportingPeriod) {
        List<ReportingDate> reportingDates = reportingDateRepository.findReportingDatesByReportingPeriodAndStatusIn(
                reportingPeriod,
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        );
        for (ReportingDate reportingDate : reportingDates) {
            reportingDate.setStatus(PMConstants.REPORTING_DATE_STATUS_CLOSED);
            reportingDateRepository.save(reportingDate);
        }
    }
}
