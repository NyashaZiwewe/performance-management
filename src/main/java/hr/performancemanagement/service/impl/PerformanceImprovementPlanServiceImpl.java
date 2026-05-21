package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.PerformanceImprovementPlan;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.PerformanceImprovementPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;


@Service
public class PerformanceImprovementPlanServiceImpl implements hr.performancemanagement.service.api.PerformanceImprovementPlanService {

    @Autowired
    PerformanceImprovementPlanRepository performanceImprovementPlanRepository;
    @Autowired
    HttpSession session;
    @Autowired
    CommonService cs;

    @Override
    public List<PerformanceImprovementPlan> listAllPerformanceImprovementPlans(){
        Account loggedUser = cs.getLoggedUser();
        List<PerformanceImprovementPlan> performanceImprovementPlanList = new ArrayList<>();
        performanceImprovementPlanRepository.findPerformanceImprovementPlansByClientId(loggedUser.getClientId()).forEach(performanceImprovementPlan -> performanceImprovementPlanList.add(performanceImprovementPlan));
        return performanceImprovementPlanList;
    }

    @Override
    public List<PerformanceImprovementPlan> listAllPerformanceImprovementPlansByClientId(long clientId){
        List<PerformanceImprovementPlan> performanceImprovementPlanList = new ArrayList<>();
        performanceImprovementPlanRepository.findPerformanceImprovementPlansByClientId(clientId).forEach(performanceImprovementPlanList::add);
        return performanceImprovementPlanList;
    }

    @Override
    public List<PerformanceImprovementPlan> listAllPerformanceImprovementPlans(ReportingPeriod period){
        Account loggedUser = cs.getLoggedUser();
        List<PerformanceImprovementPlan> performanceImprovementPlanList = new ArrayList<>();
        performanceImprovementPlanRepository.findPerformanceImprovementPlansByClientIdAndReportingPeriod(loggedUser.getClientId(), period).forEach(performanceImprovementPlan -> performanceImprovementPlanList.add(performanceImprovementPlan));
        return performanceImprovementPlanList;
    }

    @Override
    public List<PerformanceImprovementPlan> listAllPerformanceImprovementPlans(long clientId, ReportingPeriod period){
        List<PerformanceImprovementPlan> performanceImprovementPlanList = new ArrayList<>();
        performanceImprovementPlanRepository.findPerformanceImprovementPlansByClientIdAndReportingPeriod(clientId, period).forEach(performanceImprovementPlanList::add);
        return performanceImprovementPlanList;
    }

    @Override
    public List<PerformanceImprovementPlan> listPerformanceImprovementPlansByEmployee(Account employee, ReportingPeriod reportingPeriod){
        List<PerformanceImprovementPlan> performanceImprovementPlanList = new ArrayList<>();
        performanceImprovementPlanRepository.findPerformanceImprovementPlansByEmployeeAndReportingPeriod(employee, reportingPeriod).forEach(performanceImprovementPlan -> performanceImprovementPlanList.add(performanceImprovementPlan));
        return performanceImprovementPlanList;
    }

    @Override
    public List<PerformanceImprovementPlan> listAllPerformanceImprovementPlansByEmployee(Account employee){
        List<PerformanceImprovementPlan> performanceImprovementPlanList = new ArrayList<>();
        performanceImprovementPlanRepository.findPerformanceImprovementPlansByEmployee(employee).forEach(performanceImprovementPlan -> performanceImprovementPlanList.add(performanceImprovementPlan));
        return performanceImprovementPlanList;
    }

    @Override
    public PerformanceImprovementPlan getPerformanceImprovementPlanById(long id){

        PerformanceImprovementPlan plan = performanceImprovementPlanRepository.findPerformanceImprovementPlanById(id);
        return plan;
    }

    @Override
    public void addPerformanceImprovementPlan(PerformanceImprovementPlan plan) {

        performanceImprovementPlanRepository.save(plan);
    }

    @Override
    public PerformanceImprovementPlan savePerformanceImprovementPlan(PerformanceImprovementPlan plan){
        PerformanceImprovementPlan savedPerformanceImprovementPlan = performanceImprovementPlanRepository.save(plan);
        return savedPerformanceImprovementPlan;
    }

    @Transactional
    @Override
    public void deletePerformanceImprovementPlan(PerformanceImprovementPlan performanceImprovementPlan){
        performanceImprovementPlanRepository.delete(performanceImprovementPlan);
    }
}
