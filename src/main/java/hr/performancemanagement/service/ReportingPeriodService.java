package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingPeriodRepository;
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
    HttpSession session;

    public ReportingPeriod getReportingPeriodById(long id){

        ReportingPeriod reportingPeriod = reportingPeriodRepository.findReportingPeriodById(id);
        return reportingPeriod;
    }

    public ReportingPeriod getActiveReportingPeriod(){

        ReportingPeriod reportingPeriod = reportingPeriodRepository.findReportingPeriodByStatus("ACTIVE");
        return reportingPeriod;
    }

    public List<ReportingPeriod> listAllReportingPeriods(){
        List<ReportingPeriod> reportingPeriodList = new ArrayList<>();
        Long clientId = (Long) session.getAttribute("clientId");
        reportingPeriodRepository.findAllReportingPeriodsByClientId(clientId).forEach(reportingPeriod -> reportingPeriodList.add(reportingPeriod));
        return reportingPeriodList;
    }

    public void saveReportingPeriod(ReportingPeriod reportingPeriod) {

        reportingPeriodRepository.save(reportingPeriod);
    }
}
