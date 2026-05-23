package hr.performancemanagement.controllers;

import hr.performancemanagement.entities.Output;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.service.OutputService;
import hr.performancemanagement.service.api.TargetService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping(value = "/clean-database")
public class AdminController {

    @Autowired
    OutputService outputService;
    @Autowired
    TargetService targetService;

    @RequestMapping
    public ModelAndView goToHome(HttpServletRequest request ){

        ModelAndView modelAndView =  new ModelAndView("index");
        modelAndView.addObject("pageDomain", "Home");
        modelAndView.addObject("pageName", "Home");
        modelAndView.addObject("pageTitle", "Home");
        cleanDatabase();
        PortletUtils.addMessagesToPage(modelAndView, request);
        return modelAndView;
    }

    public void cleanDatabase(){

        List<Output> outputs = outputService.listAllOutputs();

        for(Output output : outputs){

            if (output.getOutcome() == null) {

                List<Target> targets = output.getTargets();
                System.out.println("Deleting targets >>>>>>>>>>>>>> "+ targets.size());
                targetService.deleteTargets(targets);
            }

            if(!targetService.checkIfOutputHasTargets(output)){
                System.out.println("Deleting output >>>>>>>>>>>>>> "+ output.getName());
                outputService.deleteOutput(output);

            }
        }

    }
}
