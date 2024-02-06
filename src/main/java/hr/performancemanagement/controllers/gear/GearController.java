package hr.performancemanagement.controllers.gear;
import hr.performancemanagement.entities.Gear;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Outcome;
import hr.performancemanagement.repository.GearRepository;
import hr.performancemanagement.service.CommonService;
import hr.performancemanagement.service.GearService;
import hr.performancemanagement.service.GoalService;
import hr.performancemanagement.service.OutcomeService;
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

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {

        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Key Focus Area");
        PortletUtils.addMessagesToPage(modelAndView, request);
    }


    @RequestMapping
    public ModelAndView viewgears(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_GEARS);
        modelAndView.addObject("pageTitle", "View gears");
        List<Gear> gears = gearService.listAllGears(commonService.getLoggedUser().getClientId());
        modelAndView.addObject("gears", gears);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/add-gear")
    public ModelAndView addGear(HttpServletRequest request) {

        ModelAndView modelAndView = new ModelAndView(Pages.ADD_GEAR);
        modelAndView.addObject("pageTitle", "New gear");
        modelAndView.addObject("gear", new Gear());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-gear", method = RequestMethod.POST)
    public String saveGear(HttpServletRequest request, Gear newGear) {

        newGear.setClientId(Client.CLIENT_ID);
        gearService.addGear(newGear);
        PortletUtils.addInfoMsg("Gear was successfully created or updated.", request);
        return "redirect:/gears";
    }


    @RequestMapping("/edit-gear/{id}")
    public ModelAndView editGear(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.EDIT_GEAR);
        modelAndView.addObject("pageTitle", "Update Gear");
        Gear gear = gearService.getGearById(id);
        modelAndView.addObject("gear", gear);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/view-gear/{id}")
    public ModelAndView viewGear(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_GEAR);
        modelAndView.addObject("pageTitle", "Update Gear");
        Gear gear = gearService.getGearById(id);
        modelAndView.addObject("gear", gear);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/add-goal", method = RequestMethod.POST)
    public String addGoal(HttpServletRequest request, Goal newGoal) {

        goalService.addGoal(newGoal);
        PortletUtils.addInfoMsg("Strategic goal successfully added.", request);
        return "redirect:/gears/view-gear/" + newGoal.getGear().getId();
    }

    @RequestMapping(value = "/save-goal", method = RequestMethod.POST)
    public String saveGoal( HttpServletRequest request, Goal Goal) {

        goalService.saveGoal(Goal);
        PortletUtils.addInfoMsg("Strategic goal successfully updated.", request);
        return "redirect:/gears/view-gear/" + Goal.getGear().getId();
    }

    @RequestMapping(value = "/save-outcome", method = RequestMethod.POST)
    public String saveOutcome( HttpServletRequest request, String name, long goalId, long gearId) {

        Outcome outcome = new Outcome();
        outcome.setName(name);
        outcome.setGoal(goalService.getGoalById(goalId));
        outcomeService.saveOutcome(outcome);
        PortletUtils.addInfoMsg("Outcome successfully added.", request);
        return "redirect:/gears/view-gear/" + gearId;
    }

}
