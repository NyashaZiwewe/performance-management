package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.PerformanceImprovementPlan;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.PerformanceImprovementPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PerformanceImprovementPlanService {

    @Autowired
    PerformanceImprovementPlanRepository performanceImprovementPlanRepository;

    public List<PerformanceImprovementPlan> listAllPerformanceImprovementPlans(long clientId){
        List<PerformanceImprovementPlan> performanceImprovementPlanList = new ArrayList<>();
        performanceImprovementPlanRepository.findPerformanceImprovementPlansByClientId(clientId).forEach(performanceImprovementPlan -> performanceImprovementPlanList.add(performanceImprovementPlan));
        return performanceImprovementPlanList;
    }

    public List<PerformanceImprovementPlan> listPerformanceImprovementPlansByEmployee(Account employee, ReportingPeriod reportingPeriod){
        List<PerformanceImprovementPlan> performanceImprovementPlanList = new ArrayList<>();
        performanceImprovementPlanRepository.findPerformanceImprovementPlansByEmployeeAndReportingPeriod(employee, reportingPeriod).forEach(performanceImprovementPlan -> performanceImprovementPlanList.add(performanceImprovementPlan));
        return performanceImprovementPlanList;
    }

    public PerformanceImprovementPlan getPerformanceImprovementPlanById(long id){

        PerformanceImprovementPlan plan = performanceImprovementPlanRepository.findPerformanceImprovementPlanById(id);
        return plan;
    }

    public void addPerformanceImprovementPlan(PerformanceImprovementPlan plan) {

        performanceImprovementPlanRepository.save(plan);
    }

    public PerformanceImprovementPlan savePerformanceImprovementPlan(PerformanceImprovementPlan plan){
        PerformanceImprovementPlan savedPerformanceImprovementPlan = performanceImprovementPlanRepository.save(plan);
        return savedPerformanceImprovementPlan;
    }

    @Transactional
    public void deletePerformanceImprovementPlan(PerformanceImprovementPlan performanceImprovementPlan){
        performanceImprovementPlanRepository.delete(performanceImprovementPlan);
    }
}