package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportingDateService {
    @Autowired
    ReportingDateRepository reportingDateRepository;
    @Autowired
    CommonService cs;

    public ReportingDate getReportingDateById(long id){

        ReportingDate reportingDate = reportingDateRepository.findReportingDateById(id);
        return reportingDate;
    }

    public ReportingDate getActiveReportingDate(){

        Account loggedUser = cs.getLoggedUser();
        ReportingDate reportingDate = reportingDateRepository.findReportingDateByStatusAndAndReportingPeriod_ClientId(PMConstants.STATUS_ACTIVE, loggedUser.getClientId());
        return reportingDate;
    }

    public List<ReportingDate> listAllReportingDates(ReportingPeriod reportingPeriod){
        List<ReportingDate> reportingDateList = new ArrayList<>();
        reportingDateRepository.findReportingDatesByReportingPeriod(reportingPeriod).forEach(reportingDate -> reportingDateList.add(reportingDate));
        return reportingDateList;
    }

    private void deactivateReportingDates(ReportingPeriod reportingPeriod){
        List<ReportingDate> reportingDateList = reportingDateRepository.findReportingDatesByReportingPeriod(reportingPeriod);
        for(ReportingDate reportingDate: reportingDateList){
            reportingDate.setStatus(PMConstants.STATUS_IN_ACTIVE);
            saveReportingDate(reportingDate);
        }
    }

    public void saveReportingDate(ReportingDate reportingDate) {
        String status = reportingDate.getStatus();
        if(PMConstants.STATUS_ACTIVE.equalsIgnoreCase(status)){
            deactivateReportingDates(reportingDate.getReportingPeriod());
        }
        reportingDateRepository.save(reportingDate);
    }

    @Transactional
    public void deleteReportingDate(ReportingDate reportingDate){
        reportingDateRepository.delete(reportingDate);
    }
}
