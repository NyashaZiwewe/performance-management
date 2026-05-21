package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingPeriodRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;


@Service
public class ReportingPeriodServiceImpl implements hr.performancemanagement.service.api.ReportingPeriodService {

    @Autowired
    ReportingPeriodRepository reportingPeriodRepository;
    @Autowired
    CommonService cs;

    @Override
    public ReportingPeriod getReportingPeriodById(long id){

        ReportingPeriod reportingPeriod = reportingPeriodRepository.findReportingPeriodById(id);
        return reportingPeriod;
    }

    @Override
    public ReportingPeriod getActiveReportingPeriod(){

        ReportingPeriod reportingPeriod = reportingPeriodRepository.findReportingPeriodByStatus(PMConstants.STATUS_ACTIVE);
        return reportingPeriod;
    }

    @Override
    public List<ReportingPeriod> listAllReportingPeriods(){
        List<ReportingPeriod> reportingPeriodList = new ArrayList<>();
        Account loggedUser = cs.getLoggedUser();
        Long clientId = loggedUser.getClientId();
        reportingPeriodRepository.findAllReportingPeriodsByClientId(clientId).forEach(reportingPeriodList::add);
        return reportingPeriodList;
    }

    @Override
    public List<ReportingPeriod> listAllReportingPeriods(long clientId){
        List<ReportingPeriod> reportingPeriodList = new ArrayList<>();
        reportingPeriodRepository.findAllReportingPeriodsByClientId(clientId).forEach(reportingPeriodList::add);
        return reportingPeriodList;
    }

    @Override
    public void saveReportingPeriod(ReportingPeriod reportingPeriod) {

        reportingPeriodRepository.save(reportingPeriod);
    }

    @Transactional
    @Override
    public void deleteReportingPeriod(ReportingPeriod reportingPeriod) {
        reportingPeriodRepository.delete(reportingPeriod);
    }
}
