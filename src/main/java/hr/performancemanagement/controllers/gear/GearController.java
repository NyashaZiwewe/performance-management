package hr.performancemanagement.controllers.gear;
import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.GearRepository;
import hr.performancemanagement.service.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Client;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
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
    OutcomeService outcomeService;
    @Autowired
    private PillarService pillarService;

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {

        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Key Focus Area");
        modelAndView = addTerminology(modelAndView);
        PortletUtils.addMessagesToPage(modelAndView, request);
    }


    @RequestMapping
    public ModelAndView viewgears(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_GEARS);
        modelAndView.addObject("pageTitle", "View Metrics");
        List<Gear> gears = gearService.listAllGears(commonService.getLoggedUser().getClientId());
        modelAndView.addObject("gears", gears);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/add-gear")
    public ModelAndView addGear(HttpServletRequest request) {

        ModelAndView modelAndView = new ModelAndView(Pages.ADD_GEAR);
        modelAndView.addObject("pageTitle", "New Metric");
        modelAndView.addObject("gear", new Gear());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-gear", method = RequestMethod.POST)
    public String saveGear(HttpServletRequest request, Gear newGear) {

        newGear.setClientId(Client.CLIENT_ID);
        gearService.addGear(newGear);
        PortletUtils.addInfoMsg("Metric was successfully created or updated.", request);
        return "redirect:/gears";
    }


    @RequestMapping("/edit-gear/{id}")
    public ModelAndView editGear(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.EDIT_GEAR);
        modelAndView.addObject("pageTitle", "Update Metric");
        Gear gear = gearService.getGearById(id);
        modelAndView.addObject("gear", gear);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/view-gear/{id}")
    public ModelAndView viewGear(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_GEAR);
        modelAndView.addObject("pageTitle", "Update Metric");
        Gear gear = gearService.getGearById(id);
        modelAndView.addObject("gear", gear);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/add-goal", method = RequestMethod.POST)
    public String addGoal(HttpServletRequest request, Goal newGoal) {
        try {
            Goal goal = goalService.addGoal(newGoal, request);
            PortletUtils.addInfoMsg("Strategic goal successfully added.", request);
        }catch (Exception e){

        }

        return "redirect:/gears/view-gear/" + newGoal.getGear().getId();
    }

    @RequestMapping(value = "/save-goal", method = RequestMethod.POST)
    public String saveGoal( HttpServletRequest request, Goal Goal) {

        goalService.saveGoal(Goal);
        PortletUtils.addInfoMsg("Strategic goal successfully updated.", request);
        return "redirect:/gears/view-gear/" + Goal.getGear().getId();
    }

    @RequestMapping(value = "/save-outcome", method = RequestMethod.POST)
    public String saveOutcome( HttpServletRequest request, String name, long goalId, long gearId, long pillarId) {

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

        Outcome outcome = outcomeService.getOutcomeById(outcomeId);
        outcome.setName(name);
        outcomeService.saveOutcome(outcome);
        PortletUtils.addInfoMsg("Record successfully updated.", request);
        return "redirect:/gears/view-gear/" + gearId;
    }

    @RequestMapping(value = "/save-pillar", method = RequestMethod.POST)
    public String savePillar( HttpServletRequest request, String name, long goalId, long gearId) {

        Pillar pillar = new Pillar();
        pillar.setName(name);
        pillar.setGoal(goalService.getGoalById(goalId));
        pillarService.savePillar(pillar);
        PortletUtils.addInfoMsg("Pillar successfully added.", request);
        return "redirect:/gears/view-gear/" + gearId;
    }

    @RequestMapping(value = "/update-pillar", method = RequestMethod.POST)
    public String updatePillar( HttpServletRequest request, String name, long pillarId, long gearId) {

        Pillar pillar = pillarService.findById(pillarId);
        pillar.setName(name);
        pillarService.savePillar(pillar);
        PortletUtils.addInfoMsg("Pillar successfully updated.", request);
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
