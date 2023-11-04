package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingPeriodRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        reportingPeriodRepository.save(reportingPeriod);
    }
}
