package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ActionPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;


import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;


@Service
public class ActionPlanServiceImpl implements hr.performancemanagement.service.api.ActionPlanService {
    @Autowired
    ActionPlanRepository actionPlanRepository;
    @Autowired
    HttpSession session;
    @Autowired
    CommonService cs;

    @Override
    public List<ActionPlan> listAllActionPlans(ReportingPeriod reportingPeriod){

        List<ActionPlan> actionPlanList = new ArrayList<>();
        Account loggedUser = cs.getLoggedUser();

        if(cs.isAdmin() || cs.hasSpecialRights()){
            actionPlanRepository.findActionPlansByReportingPeriodAndClientId(reportingPeriod, loggedUser.getClientId()).forEach(actionPlan -> actionPlanList.add(actionPlan));
        }
        else if(loggedUser.getAccountType().equalsIgnoreCase("Employee")){

            actionPlanRepository.findActionPlansByReportingPeriodAndManager(reportingPeriod, loggedUser).forEach(actionPlan -> actionPlanList.add(actionPlan));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("Supervisor")){

            actionPlanRepository.findActionPlansByReportingPeriodAndManager(reportingPeriod, loggedUser).forEach(actionPlan -> actionPlanList.add(actionPlan));
            actionPlanRepository.findActionPlansByReportingPeriodAndManager_Supervisor(reportingPeriod, loggedUser).forEach(actionPlan -> actionPlanList.add(actionPlan));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("DEPARTMENT_MANAGER") || loggedUser.getAccountType().equalsIgnoreCase("DIVISIONAL_DIRECTOR")){

            actionPlanRepository.findActionPlansByReportingPeriodAndManager_Department(reportingPeriod, loggedUser.getDepartment()).forEach(actionPlan -> actionPlanList.add(actionPlan));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("ACTING_CEO") || loggedUser.getAccountType().equalsIgnoreCase("CEO") ){

            actionPlanRepository.findActionPlansByReportingPeriodAndClientId(reportingPeriod, loggedUser.getClientId()).forEach(actionPlan -> actionPlanList.add(actionPlan));

        }else {

        }

        return actionPlanList;
    }

    @Override
    public List<ActionPlan> listAllActionPlansByClientId(long clientId) {
        List<ActionPlan> actionPlanList = new ArrayList<>();
        actionPlanRepository.findActionPlansByClientId(clientId).forEach(actionPlanList::add);
        return actionPlanList;
    }

    @Override
    public List<ActionPlan> listAllActionPlansByReportingPeriod(long clientId, ReportingPeriod reportingPeriod) {
        List<ActionPlan> actionPlanList = new ArrayList<>();
        actionPlanRepository.findActionPlansByReportingPeriodAndClientId(reportingPeriod, clientId).forEach(actionPlanList::add);
        return actionPlanList;
    }

    @Override
    public List<ActionPlan> listAllUserActionPlans(Account manager){

        List<ActionPlan> actionPlanList = new ArrayList<>();

        actionPlanRepository.findActionPlansByManager(manager).forEach(actionPlan -> actionPlanList.add(actionPlan));

        return actionPlanList;
    }

    @Override
    public ActionPlan getActionPlanById(long id){

        ActionPlan actionPlan = actionPlanRepository.findActionPlanById(id);
        return actionPlan;
    }

    @Override
    public void addActionPlan(ActionPlan actionPlan) {

            actionPlanRepository.save(actionPlan);
    }

    @Override
    public ActionPlan saveActionPlan(ActionPlan actionPlan){
        ActionPlan savedActionPlan = actionPlanRepository.save(actionPlan);
        return savedActionPlan;
    }
    @Transactional
    @Override
    public void deleteActionPlan(ActionPlan actionPlan){
        actionPlanRepository.delete(actionPlan);
    }
}
