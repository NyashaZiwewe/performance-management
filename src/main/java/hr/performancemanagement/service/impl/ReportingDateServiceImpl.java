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
        ReportingDate reportingDate = reportingDateRepository.findReportingDateByStatusAndAndReportingPeriod_ClientId(PMConstants.REPORTING_DATE_STATUS_OPEN, loggedUser.getClientId());
        if (reportingDate == null) {
            reportingDate = reportingDateRepository.findReportingDateByStatusAndAndReportingPeriod_ClientId(PMConstants.STATUS_ACTIVE, loggedUser.getClientId());
        }
        return reportingDate;
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
