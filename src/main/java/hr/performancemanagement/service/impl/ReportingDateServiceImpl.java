package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
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
    CommonService cs;

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
        if (activeReportingDates.isEmpty()) {
            return null;
        }
        return activeReportingDates.get(0);
    }

    @Override
    public List<ReportingDate> listOpenOrActiveReportingDates(long clientId) {
        if (clientId <= 0) {
            return Collections.emptyList();
        }
        List<ReportingDate> reportingDates = reportingDateRepository.findReportingDatesByReportingPeriod_ClientIdAndStatusInOrderByDateDescIdDesc(
                clientId,
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        );
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
        for(ReportingDate reportingDate: reportingDateList){
            if (keepId > 0 && reportingDate.getId() == keepId) {
                continue;
            }
            reportingDate.setStatus(PMConstants.REPORTING_DATE_STATUS_CLOSED);
            reportingDateRepository.save(reportingDate);
        }
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
            closeOpenReportingDates(reportingPeriod, reportingDate.getId());
        }
        reportingDateRepository.save(reportingDate);
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
