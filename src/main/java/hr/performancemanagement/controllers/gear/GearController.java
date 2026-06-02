package hr.performancemanagement.controllers.gear;
import hr.performancemanagement.entities.*;
import hr.performancemanagement.service.GearService;
import hr.performancemanagement.service.OutcomeService;
import hr.performancemanagement.service.PillarService;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.GoalService;
import hr.performancemanagement.service.api.ReportingPeriodService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.constants.Pages;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value = "/gears")
public class GearController {

    @Autowired
    GearService gearService;
    @Autowired
    CommonService commonService;
    @Autowired
    GoalService goalService;
    @Autowired
    ReportingPeriodService reportingPeriodService;
    @Autowired
    OutcomeService outcomeService;
    @Autowired
    private PillarService pillarService;

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {

        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Key Focus Area");
        modelAndView = addTerminology(modelAndView);
        PortletUtils.addMessagesToPage(modelAndView, request);
    }

    private boolean canManagePredefinedHierarchy() {
        Account loggedUser = commonService.getLoggedUser();
        return loggedUser != null
                && (PMConstants.IS_ADMIN.equalsIgnoreCase(loggedUser.getAdmin())
                || PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(loggedUser.getSpecial()));
    }

    private ModelAndView buildUnauthorizedView(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.BLANK_PAGE);
        PortletUtils.addErrorMsg("You are not allowed to manage predefined outputs/outcomes.", request);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    private String unauthorizedRedirect(HttpServletRequest request) {
        PortletUtils.addErrorMsg("You are not allowed to manage predefined outputs/outcomes.", request);
        return "redirect:/scorecards";
    }


    @RequestMapping
    public ModelAndView viewgears(@RequestParam(value = "reportingPeriodId", required = false) Long reportingPeriodId,
                                  HttpServletRequest request) {
        if (!canManagePredefinedHierarchy()) {
            return buildUnauthorizedView(request);
        }
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_GEARS);
        modelAndView.addObject("pageTitle", "View Metrics");
        long clientId = commonService.getConfiguredClientId();
        List<Gear> gears;
        if (reportingPeriodId != null && reportingPeriodId > 0) {
            ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
            if (reportingPeriod != null) {
                gears = gearService.listGearsByReportingPeriod(clientId, reportingPeriod);
                modelAndView.addObject("selectedReportingPeriodId", reportingPeriodId);
            } else {
                gears = gearService.listAllGears(clientId);
                PortletUtils.addErrorMsg("Reporting period not found. Showing all metrics.", request);
            }
        } else {
            gears = gearService.listAllGears(clientId);
        }
        modelAndView.addObject("reportingPeriodsList", reportingPeriodService.listAllReportingPeriods(clientId));
        modelAndView.addObject("gears", gears);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/add-gear")
    public ModelAndView addGear(HttpServletRequest request) {
        if (!canManagePredefinedHierarchy()) {
            return buildUnauthorizedView(request);
        }

        ModelAndView modelAndView = new ModelAndView(Pages.ADD_GEAR);
        modelAndView.addObject("pageTitle", "New Metric");
        modelAndView.addObject("gear", new Gear());
        modelAndView.addObject("reportingPeriodsList", reportingPeriodService.listAllReportingPeriods(commonService.getConfiguredClientId()));
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-gear", method = RequestMethod.POST)
    public String saveGear(HttpServletRequest request, Gear newGear, Long reportingPeriodId) {
        if (!canManagePredefinedHierarchy()) {
            return unauthorizedRedirect(request);
        }

        if (newGear == null || reportingPeriodId == null || reportingPeriodId <= 0) {
            PortletUtils.addErrorMsg("Reporting period is required.", request);
            return newGear != null && newGear.getId() > 0 ? "redirect:/gears/edit-gear/" + newGear.getId() : "redirect:/gears/add-gear";
        }

        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
        if (reportingPeriod == null) {
            PortletUtils.addErrorMsg("Selected reporting period was not found.", request);
            return newGear.getId() > 0 ? "redirect:/gears/edit-gear/" + newGear.getId() : "redirect:/gears/add-gear";
        }

        newGear.setReportingPeriod(reportingPeriod);
        newGear.setClientId(commonService.getConfiguredClientId());
        gearService.addGear(newGear);
        PortletUtils.addInfoMsg("Metric was successfully created or updated.", request);
        return "redirect:/gears";
    }

    @RequestMapping(value = "/delete-gear", method = RequestMethod.POST)
    public String deleteGear(HttpServletRequest request, long gearId) {
        if (!canManagePredefinedHierarchy()) {
            return unauthorizedRedirect(request);
        }
        gearService.deleteGear(gearId);
        PortletUtils.addInfoMsg("Record was successfully deleted.", request);
        return "redirect:/gears";
    }


    @RequestMapping("/edit-gear/{id}")
    public ModelAndView editGear(@PathVariable("id") long id, HttpServletRequest request) {
        if (!canManagePredefinedHierarchy()) {
            return buildUnauthorizedView(request);
        }
        ModelAndView modelAndView = new ModelAndView(Pages.EDIT_GEAR);
        modelAndView.addObject("pageTitle", "Update Metric");
        Gear gear = gearService.getGearById(id);
        modelAndView.addObject("gear", gear);
        modelAndView.addObject("reportingPeriodsList", reportingPeriodService.listAllReportingPeriods(commonService.getConfiguredClientId()));
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/view-gear/{id}")
    public ModelAndView viewGear(@PathVariable("id") long id, HttpServletRequest request) {
        if (!canManagePredefinedHierarchy()) {
            return buildUnauthorizedView(request);
        }
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_GEAR);
        modelAndView.addObject("pageTitle", "Update Metric");
        Gear gear = gearService.getGearById(id);
        modelAndView.addObject("gear", gear);
        modelAndView.addObject("gears", gearService.listAllGears(commonService.getConfiguredClientId()));
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/add-goal", method = RequestMethod.POST)
    public String addGoal(HttpServletRequest request, Goal newGoal) {
        if (!canManagePredefinedHierarchy()) {
            return unauthorizedRedirect(request);
        }
        try {
            goalService.saveGoal(newGoal);
            PortletUtils.addInfoMsg("Strategic goal successfully added.", request);
        } catch (Exception e) {
            PortletUtils.addErrorMsg("Failed to add strategic goal.", request);
        }

        return "redirect:/gears/view-gear/" + newGoal.getGear().getId();
    }

    @RequestMapping(value = "/save-goal", method = RequestMethod.POST)
    public String saveGoal( HttpServletRequest request, Goal updatedGoal, String gearId) {
        if (!canManagePredefinedHierarchy()) {
            return unauthorizedRedirect(request);
        }

        Goal goal = goalService.getGoalById(updatedGoal.getId());
        if (goal == null) {
            PortletUtils.addErrorMsg("Strategic goal not found.", request);
            return "redirect:/gears/view-gear/" + gearId;
        }
        goal.setName(updatedGoal.getName());
        goalService.saveGoal(goal);
        PortletUtils.addInfoMsg("Record successfully updated.", request);
        return "redirect:/gears/view-gear/" + gearId;
    }

    @RequestMapping(value = "/delete-goal", method = RequestMethod.POST)
    public String deleteGoal( HttpServletRequest request, long goalId, long gearId) {
        if (!canManagePredefinedHierarchy()) {
            return unauthorizedRedirect(request);
        }

        Goal goal = goalService.getGoalById(goalId);
        if (goal != null) {
            goalService.deleteGoal(goal);
            PortletUtils.addInfoMsg("Record successfully deleted.", request);
        } else {
            PortletUtils.addErrorMsg("Goal not found.", request);
        }
        return "redirect:/gears/view-gear/" + gearId;
    }

    @RequestMapping(value = "/save-outcome", method = RequestMethod.POST)
    public String saveOutcome( HttpServletRequest request, String name, long goalId, long gearId, long pillarId) {
        if (!canManagePredefinedHierarchy()) {
            return unauthorizedRedirect(request);
        }

        Outcome outcome = new Outcome();
        outcome.setName(name);
        if(pillarId !=0){
            outcome.setPillar(pillarService.findById(pillarId));
        }else{
            outcome.setGoal(goalService.getGoalById(goalId));
        }
        outcomeService.saveOutcome(outcome);
        PortletUtils.addInfoMsg("Record successfully added.", request);
        return "redirect:/gears/view-gear/" + gearId;
    }

    @RequestMapping(value = "/update-outcome", method = RequestMethod.POST)
    public String updateOutcome( HttpServletRequest request, String name, long outcomeId, long gearId) {
        if (!canManagePredefinedHierarchy()) {
            return unauthorizedRedirect(request);
        }

        Outcome outcome = outcomeService.getOutcomeById(outcomeId);
        outcome.setName(name);
        outcomeService.saveOutcome(outcome);
        PortletUtils.addInfoMsg("Record successfully updated.", request);
        return "redirect:/gears/view-gear/" + gearId;
    }

    @RequestMapping(value = "/delete-outcome", method = RequestMethod.POST)
    public String deleteOutcome( HttpServletRequest request, long outcomeId, long gearId) {
        if (!canManagePredefinedHierarchy()) {
            return unauthorizedRedirect(request);
        }

        Outcome outcome = outcomeService.getOutcomeById(outcomeId);
        outcomeService.deleteOutcome(outcome);
        PortletUtils.addInfoMsg("Record successfully deleted.", request);
        return "redirect:/gears/view-gear/" + gearId;
    }

    @RequestMapping(value = "/save-pillar", method = RequestMethod.POST)
    public String savePillar( HttpServletRequest request, String name, long goalId, long gearId) {
        if (!canManagePredefinedHierarchy()) {
            return unauthorizedRedirect(request);
        }

        Pillar pillar = new Pillar();
        pillar.setName(name);
        pillar.setGoal(goalService.getGoalById(goalId));
        pillarService.savePillar(pillar);
        PortletUtils.addInfoMsg("Pillar successfully added.", request);
        return "redirect:/gears/view-gear/" + gearId;
    }

    @RequestMapping(value = "/update-pillar", method = RequestMethod.POST)
    public String updatePillar( HttpServletRequest request, String name, long pillarId, long gearId) {
        if (!canManagePredefinedHierarchy()) {
            return unauthorizedRedirect(request);
        }

        Pillar pillar = pillarService.findById(pillarId);
        pillar.setName(name);
        pillarService.savePillar(pillar);
        PortletUtils.addInfoMsg("Pillar successfully updated.", request);
        return "redirect:/gears/view-gear/" + gearId;
    }

    @RequestMapping(value = "/delete-pillar", method = RequestMethod.POST)
    public String deletePillar( HttpServletRequest request,long pillarId, long gearId) {
        if (!canManagePredefinedHierarchy()) {
            return unauthorizedRedirect(request);
        }
        pillarService.deletePillar(pillarId);
        PortletUtils.addInfoMsg("Pillar successfully deleted.", request);
        return "redirect:/gears/view-gear/" + gearId;
    }

    public ModelAndView addTerminology(ModelAndView modelAndView) {
        if(modelAndView.getModel().containsKey("gear")){
            String stage1, stage2,stage3,stage4, model;
            Gear gear = (Gear) modelAndView.getModel().get("gear");
            if("programme".equalsIgnoreCase(gear.getCategory())){
                stage1 = "Programme";
                stage2 = "Outcome";
                stage3 = "Pillar";
                stage4 = "Strategic Goal";
                model = "programme";
            }else {
                stage1 = "Gear";
                stage2 = "Goal";
                stage3 = "Goal";
                stage4 = "Outcome";
                model = "gear";
            }
            modelAndView.addObject("stage1", stage1);
            modelAndView.addObject("stage2", stage2);
            modelAndView.addObject("stage3", stage3);
            modelAndView.addObject("stage4", stage4);
            modelAndView.addObject("model", model);
        };

        return modelAndView;
    }

}
