package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingPeriodRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportingPeriodService {

    @Autowired
    ReportingPeriodRepository reportingPeriodRepository;
    @Autowired
    CommonService cs;

    public ReportingPeriod getReportingPeriodById(long id){

        ReportingPeriod reportingPeriod = reportingPeriodRepository.findReportingPeriodById(id);
        return reportingPeriod;
    }

    public ReportingPeriod getActiveReportingPeriod(){

        ReportingPeriod reportingPeriod = reportingPeriodRepository.findReportingPeriodByStatus(PMConstants.STATUS_ACTIVE);
        return reportingPeriod;
    }

    public List<ReportingPeriod> listAllReportingPeriods(){
        List<ReportingPeriod> reportingPeriodList = new ArrayList<>();
        Account loggedUser = cs.getLoggedUser();
        Long clientId = loggedUser.getClientId();
        reportingPeriodRepository.findAllReportingPeriodsByClientId(clientId).forEach(reportingPeriod -> reportingPeriodList.add(reportingPeriod));
        return reportingPeriodList;
    }

    public void saveReportingPeriod(ReportingPeriod reportingPeriod) {
        if(PMConstants.STATUS_ACTIVE.equalsIgnoreCase(reportingPeriod.getStatus())){
            deactivateReportingPeriods(reportingPeriod);
        }
        reportingPeriodRepository.save(reportingPeriod);
    }

    private void deactivateReportingPeriods(ReportingPeriod reportingPeriod){
        Long clientId = reportingPeriod.getClientId();
        List<ReportingPeriod> reportingPeriodList = reportingPeriodRepository.findAllReportingPeriodsByClientId(clientId);
        for(ReportingPeriod period: reportingPeriodList){
            period.setStatus(PMConstants.STATUS_IN_ACTIVE);
            reportingPeriodRepository.save(period);
        }
    }

    @Transactional
    public void deleteReportingPeriod(long id){
        ReportingPeriod reportingPeriod = reportingPeriodRepository.findReportingPeriodById(id);
        reportingPeriodRepository.delete(reportingPeriod);
    }
}
