package hr.performancemanagement.controllers.scorecard;

import hr.performancemanagement.entities.ScorecardWorkflowStage;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.ScorecardWorkflowStageService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/scorecard-workflow-stages")
public class ScorecardWorkflowController {

    private final CommonService commonService;
    private final ScorecardWorkflowStageService scorecardWorkflowStageService;

    public ScorecardWorkflowController(CommonService commonService,
                                       ScorecardWorkflowStageService scorecardWorkflowStageService) {
        this.commonService = commonService;
        this.scorecardWorkflowStageService = scorecardWorkflowStageService;
    }

    @RequestMapping
    public ModelAndView viewConfig(@RequestParam(value = "id", required = false) Long id,
                                   HttpServletRequest request) {
        if (!hasAccess(request)) {
            return new ModelAndView("redirect:/");
        }

        List<ScorecardWorkflowStage> stages = scorecardWorkflowStageService.listAllWorkflowStages();
        ScorecardWorkflowStage stageForm;
        if (id != null && id > 0) {
            stageForm = scorecardWorkflowStageService.getWorkflowStageById(id);
            if (stageForm == null) {
                PortletUtils.addErrorMsg("Workflow stage not found.", request);
                stageForm = buildNewStage(stages);
            }
        } else {
            stageForm = buildNewStage(stages);
        }


        ModelAndView modelAndView = new ModelAndView(Pages.CONFIG_SCORECARD_WORKFLOW);
        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Scorecard Workflow");
        modelAndView.addObject("pageTitle", "Scorecard Workflow");
        modelAndView.addObject("workflowStages", stages);
        modelAndView.addObject("workflowStage", stageForm);

        // Stream stage names to produce List<String>
        List<String> workflowRoles = stages.stream()
                .map(ScorecardWorkflowStage::getRoleKey)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        modelAndView.addObject("workflowRoles", workflowRoles);

        PortletUtils.addMessagesToPage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-stage", method = RequestMethod.POST)
    public String saveStage(HttpServletRequest request,
                           ScorecardWorkflowStage workflowStage,
                           @RequestParam(value = "statusCodes", required = false) String[] statusCodesArray) {
        if (!hasAccess(request)) {
            return "redirect:/";
        }

        try {
            // Convert array to comma-separated string
            if (statusCodesArray != null && statusCodesArray.length > 0) {
                workflowStage.setStatusCodes(String.join(",", statusCodesArray));
            }
            scorecardWorkflowStageService.saveWorkflowStage(workflowStage);
            PortletUtils.addInfoMsg("Workflow stage saved successfully.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Could not save workflow stage: " + exception.getMessage(), request);
        }
        return "redirect:/scorecard-workflow-stages";
    }

    @RequestMapping(value = "/delete-stage", method = RequestMethod.POST)
    public String deleteStage(HttpServletRequest request, long id) {
        if (!hasAccess(request)) {
            return "redirect:/";
        }

        try {
            scorecardWorkflowStageService.deactivateWorkflowStage(id);
            PortletUtils.addInfoMsg("Workflow stage deactivated successfully.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Could not deactivate workflow stage: " + exception.getMessage(), request);
        }
        return "redirect:/scorecard-workflow-stages";
    }

    @RequestMapping(value = "/remove-stage", method = RequestMethod.POST)
    public String removeStage(HttpServletRequest request, long id) {
        if (!hasAccess(request)) {
            return "redirect:/";
        }

        try {
            scorecardWorkflowStageService.deleteWorkflowStage(id);
            PortletUtils.addInfoMsg("Workflow status deleted successfully.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Could not delete workflow status: " + exception.getMessage(), request);
        }
        return "redirect:/scorecard-workflow-stages";
    }

    private boolean hasAccess(HttpServletRequest request) {
        if (commonService.isAdmin() || commonService.hasSpecialRights()) {
            return true;
        }
        PortletUtils.addErrorMsg("You are not allowed to manage scorecard workflow settings.", request);
        return false;
    }

    private ScorecardWorkflowStage buildNewStage(List<ScorecardWorkflowStage> stages) {
        ScorecardWorkflowStage stage = new ScorecardWorkflowStage();
        List<ScorecardWorkflowStage> safeStages = stages == null ? Collections.<ScorecardWorkflowStage>emptyList() : stages;
        int nextOrder = 1;
        for (ScorecardWorkflowStage workflowStage : safeStages) {
            if (workflowStage == null || workflowStage.getStageOrder() == null) {
                continue;
            }
            if (workflowStage.getStageOrder() >= nextOrder) {
                nextOrder = workflowStage.getStageOrder() + 1;
            }
        }
        stage.setStageOrder(nextOrder);
        stage.setStatus(PMConstants.STATUS_ACTIVE);
        stage.setRoleKey(PMConstants.SCORECARD_WORKFLOW_ROLE_NONE);
        stage.setStatusCode("");
        stage.setStatusLabel("");
        stage.setActionButtonLabel("");
        stage.setName("");
        return stage;
    }
}
