package hr.performancemanagement.controllers.perspective;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.PerspectiveRepository;
import hr.performancemanagement.service.PerspectiveService;
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
@RequestMapping(value = "/perspectives")
public class PerspectiveController {

    @Autowired
    PerspectiveRepository perspectiveRepository;
    @Autowired
    PerspectiveService perspectiveService;

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {

        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Key Focus Area");
        PortletUtils.addMessagesToPage(modelAndView, request);
    }


    @RequestMapping
    public ModelAndView viewPerspectives(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERSPECTIVES);
        modelAndView.addObject("pageTitle", "View Perspectives");
        List<Perspective> perspectives = perspectiveRepository.findAll();
        modelAndView.addObject("perspectives", perspectives);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/add-perspective")
    public ModelAndView addPerspective(HttpServletRequest request) {

        ModelAndView modelAndView = new ModelAndView(Pages.ADD_PERSPECTIVE);
        modelAndView.addObject("pageTitle", "New Perspective");
        modelAndView.addObject("perspective", new Perspective());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-perspective", method = RequestMethod.POST)
    public String savePerspective(HttpServletRequest request, Perspective newPerspective) {

        newPerspective.setClientId(Client.CLIENT_ID);
        perspectiveService.addPerspective(newPerspective);
        PortletUtils.addInfoMsg("Perspective was successfully created or updated.", request);
        return "redirect:/perspectives";
    }


    @RequestMapping("/edit-perspective/{id}")
    public ModelAndView editAccount(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.EDIT_PERSPECTIVE);
        modelAndView.addObject("pageTitle", "Update Perspective");
        Perspective perspective = perspectiveService.getPerspectiveById(id);
        modelAndView.addObject("perspective", perspective);
        preparePage(modelAndView, request);
        return modelAndView;
    }
}
