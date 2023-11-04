package hr.performancemanagement.controllers.scorecardModel;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.service.CommonService;
import hr.performancemanagement.service.ScorecardModelService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Client;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.util.List;

@Controller
@RequestMapping(value = "/scorecard-models")
public class ScorecardModelController {
    @Autowired
    private final ScorecardModelService scorecardModelService;
    @Autowired
    CommonService cs;

    public ScorecardModelController(ScorecardModelService scorecardModelService) {
        this.scorecardModelService = scorecardModelService;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request, HttpSession session) {

        modelAndView.addObject("pageDomain", "Performance");
        modelAndView.addObject("pageName", "Scorecard Models");
        PortletUtils.addMessagesToPage(modelAndView, request);

    }

    @RequestMapping
    public ModelAndView viewScorecardModels(HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORECARD_MODELS);
        modelAndView.addObject("pageTitle", "View Scorecard Models");
        Account loggedUser = cs.getLoggedUser();
        List<ScorecardModel> scorecardModelList = scorecardModelService.getAllScorecardModels(loggedUser.getClientId());
        modelAndView.addObject("scorecardModelList", scorecardModelList);
        modelAndView.addObject("loggedUser", loggedUser);
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping("/add-scorecard-model")
    public ModelAndView addAScorecardModel(HttpServletRequest request, HttpSession session) {

        ModelAndView modelAndView = new ModelAndView(Pages.ADD_SCORECARD_MODEL);
        modelAndView.addObject("pageTitle", "New Scorecard Model");
        modelAndView.addObject("scorecardModel", new ScorecardModel());
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/save-scorecard-model", method = RequestMethod.POST)
    public String saveScorecardModel(HttpServletRequest request, ScorecardModel scorecardModel) throws UnsupportedEncodingException {

        Account loggedUser = cs.getLoggedUser();
        boolean modelExists = scorecardModelService.checkIfClientModelExits(loggedUser.getClientId(), scorecardModel.getName());
        if(modelExists){
            PortletUtils.addErrorMsg("You already have "+ scorecardModel.getName() + " As a model for your organisation", request);
            return "redirect:/scorecard-models/add-scorecard-model";
        }else{
            scorecardModel.setClientId(loggedUser.getClientId());
            scorecardModelService.saveScorecardModel(scorecardModel);
            PortletUtils.addInfoMsg("You have successfully added "+ scorecardModel.getName() + " ss a model for your organisation", request);
            return "redirect:/scorecard-models/add-scorecard-model";
        }
    }

    @RequestMapping(value = "/update-scorecard-model", method = RequestMethod.POST)
    public String updateScorecardModel(HttpServletRequest request, ScorecardModel model){

        ScorecardModel model1 = scorecardModelService.getScorecardModelById(model.getId());
        model.setClientId(model1.getClientId());
        model.setDate(model1.getDate());
        scorecardModelService.saveScorecardModel(model);
        PortletUtils.addInfoMsg("You have successfully updated "+ model.getName(), request);
        return "redirect:/scorecard-models";
    }

    @RequestMapping(value = "/delete-model", method = RequestMethod.POST)
    public String deleteScorecardModel(HttpServletRequest request, ScorecardModel scorecardModel) {

        int response = scorecardModelService.deleteScorecardModel(scorecardModel);
        if(response == 0){
            PortletUtils.addErrorMsg("You cannot delete the only scorecard Model you have. add a different one if you want to delete this one", request);
        }else {
            PortletUtils.addInfoMsg("Scorecard Model was successfully deleted", request);
        }
        return "redirect:/scorecard-models";
    }

}
