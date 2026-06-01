package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Service
public class ReportingDateServiceImpl implements hr.performancemanagement.service.api.ReportingDateService {
    @Autowired
    ReportingDateRepository reportingDateRepository;
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
        return reportingDateRepository.findReportingDatesByReportingPeriod_ClientIdAndStatusInOrderByDateDescIdDesc(
                clientId,
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        );
    }

    @Override
    public boolean hasMultipleOpenOrActiveReportingDates(long clientId) {
        if (clientId <= 0) {
            return false;
        }
        return reportingDateRepository.countByReportingPeriod_ClientIdAndStatusIn(
                clientId,
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        ) > 1;
    }

    @Override
    public boolean isReportingDateOpen(ReportingDate reportingDate) {
        return reportingDate != null
                && reportingDate.getStatus() != null
                && PMConstants.REPORTING_DATE_STATUS_OPEN.equalsIgnoreCase(reportingDate.getStatus().trim());
    }

    @Override
    public List<ReportingDate> listAllReportingDates(ReportingPeriod reportingPeriod){
        List<ReportingDate> reportingDateList = new ArrayList<>();
        reportingDateRepository.findReportingDatesByReportingPeriod(reportingPeriod).forEach(reportingDate -> reportingDateList.add(reportingDate));
        return reportingDateList;
    }

    @Override
    public long countReportingDates(long reportingPeriodId) {
        return reportingDateRepository.countByReportingPeriod_Id(reportingPeriodId);
    }

    private void closeOpenReportingDates(ReportingPeriod reportingPeriod, long keepId){
        List<ReportingDate> reportingDateList = reportingDateRepository.findReportingDatesByReportingPeriodAndStatus(
                reportingPeriod,
                PMConstants.REPORTING_DATE_STATUS_OPEN
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
    public void saveReportingDate(ReportingDate reportingDate) {
        String status = normalizeStatus(reportingDate.getStatus());
        reportingDate.setStatus(status);

        if(PMConstants.REPORTING_DATE_STATUS_OPEN.equalsIgnoreCase(status)){
            closeOpenReportingDates(reportingDate.getReportingPeriod(), reportingDate.getId());
        }
        reportingDateRepository.save(reportingDate);
    }

    @Transactional
    @Override
    public void deleteReportingDate(ReportingDate reportingDate){
        reportingDateRepository.delete(reportingDate);
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
}
