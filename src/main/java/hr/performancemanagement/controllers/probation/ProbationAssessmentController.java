package hr.performancemanagement.controllers.probation;

import hr.performancemanagement.entities.ProbationAssessment;
import hr.performancemanagement.entities.ProbationDimensionTemplate;
import hr.performancemanagement.entities.ProbationKpi;
import hr.performancemanagement.entities.ProbationWorkflowStep;
import hr.performancemanagement.service.api.AccountService;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.ProbationAssessmentService;
import hr.performancemanagement.service.api.ProbationConfigService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/probation-assessments")
public class ProbationAssessmentController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private ProbationAssessmentService probationAssessmentService;
    @Autowired
    private ProbationConfigService probationConfigService;
    @Autowired
    private CommonService commonService;

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {
        modelAndView.addObject("pageDomain", "Performance");
        modelAndView.addObject("pageName", "Probation Assessments");
        modelAndView.addObject("accountsList", accountService.listAllAccounts());
        modelAndView.addObject("approverModes", probationConfigService.listApproverModes());
        modelAndView.addObject("approverModeAccountType", PMConstants.PROBATION_APPROVER_MODE_ACCOUNT_TYPE);
        modelAndView.addObject("approverModeRole", PMConstants.PROBATION_APPROVER_MODE_ROLE);
        modelAndView.addObject("approverModeUser", PMConstants.PROBATION_APPROVER_MODE_USER);
        PortletUtils.addMessagesToPage(modelAndView, request);
    }

    @RequestMapping
    public ModelAndView viewAssessments(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PROBATION_ASSESSMENTS);
        modelAndView.addObject("pageTitle", "View");
        modelAndView.addObject("assessments", probationAssessmentService.listVisibleAssessments());
        modelAndView.addObject("loggedUser", commonService.getLoggedUser());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/add-assessment")
    public ModelAndView addAssessment(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.ADD_PROBATION_ASSESSMENT);
        modelAndView.addObject("pageTitle", "New Assessment");
        modelAndView.addObject("assessment", new ProbationAssessment());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-assessment", method = RequestMethod.POST)
    public String saveAssessment(HttpServletRequest request, ProbationAssessment assessment) {
        if (assessment.getEmployee() == null) {
            PortletUtils.addErrorMsg("Please select an employee", request);
            return "redirect:/probation-assessments/add-assessment";
        }
        ProbationAssessment savedAssessment = probationAssessmentService.createAssessment(assessment);
        if (savedAssessment == null) {
            PortletUtils.addErrorMsg("Assessment could not be created. Check employee selection and access rights.", request);
            return "redirect:/probation-assessments/add-assessment";
        }
        PortletUtils.addInfoMsg("Probation assessment successfully created.", request);
        return "redirect:/probation-assessments/view-assessment/" + savedAssessment.getId();
    }

    @RequestMapping("/view-assessment/{id}")
    public ModelAndView viewAssessment(@PathVariable("id") long id, HttpServletRequest request) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(id);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Assessment not found.", request);
            return new ModelAndView("redirect:/probation-assessments");
        }
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PROBATION_ASSESSMENT);
        modelAndView.addObject("pageTitle", "View Assessment");
        modelAndView.addObject("assessment", assessment);
        modelAndView.addObject("dimensionResponses", probationAssessmentService.listAssessmentDimensions(id));
        modelAndView.addObject("kpis", probationAssessmentService.listKpis(id));
        modelAndView.addObject("approvalHistory", probationAssessmentService.listApprovalHistory(id));
        modelAndView.addObject("canSubmit", probationAssessmentService.canLoggedUserSubmit(assessment));
        modelAndView.addObject("canApprove", probationAssessmentService.canLoggedUserApprove(assessment));
        modelAndView.addObject("isOwner", probationAssessmentService.isLoggedUserOwner(assessment));
        modelAndView.addObject("isSupervisor", probationAssessmentService.isLoggedUserSupervisor(assessment));
        modelAndView.addObject("workflowConfigured", !probationConfigService.listActiveWorkflowSteps().isEmpty());
        modelAndView.addObject("newKpi", new ProbationKpi());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/update-assessment", method = RequestMethod.POST)
    public String updateAssessment(HttpServletRequest request, ProbationAssessment assessment) {
        ProbationAssessment updated = probationAssessmentService.updateAssessmentCore(assessment);
        if (updated == null) {
            PortletUtils.addErrorMsg("Assessment update failed. You are not allowed to update this section.", request);
        } else {
            PortletUtils.addInfoMsg("Assessment updated.", request);
        }
        return "redirect:/probation-assessments/view-assessment/" + assessment.getId();
    }

    @RequestMapping(value = "/save-dimension-response", method = RequestMethod.POST)
    public String saveDimensionResponse(HttpServletRequest request, long assessmentId, long dimensionTemplateId, String strengths, String areasForImprovement) {
        if (probationAssessmentService.saveDimensionResponse(assessmentId, dimensionTemplateId, strengths, areasForImprovement) == null) {
            PortletUtils.addErrorMsg("Dimension feedback was not saved. Check access rights and configuration.", request);
        } else {
            PortletUtils.addInfoMsg("Dimension feedback saved.", request);
        }
        return "redirect:/probation-assessments/view-assessment/" + assessmentId;
    }

    @RequestMapping(value = "/add-kpi", method = RequestMethod.POST)
    public String addKpi(HttpServletRequest request, long assessmentId, ProbationKpi newKpi) {
        if (probationAssessmentService.addKpi(assessmentId, newKpi) == null) {
            PortletUtils.addErrorMsg("KPI was not added. Check access rights.", request);
        } else {
            PortletUtils.addInfoMsg("KPI added.", request);
        }
        return "redirect:/probation-assessments/view-assessment/" + assessmentId;
    }

    @RequestMapping(value = "/update-kpi", method = RequestMethod.POST)
    public String updateKpi(HttpServletRequest request, long assessmentId, ProbationKpi kpi) {
        if (probationAssessmentService.updateKpi(kpi) == null) {
            PortletUtils.addErrorMsg("KPI update failed. You are not allowed to update this KPI.", request);
        } else {
            PortletUtils.addInfoMsg("KPI updated.", request);
        }
        return "redirect:/probation-assessments/view-assessment/" + assessmentId;
    }

    @RequestMapping(value = "/delete-kpi", method = RequestMethod.POST)
    public String deleteKpi(HttpServletRequest request, long assessmentId, long kpiId) {
        int before = probationAssessmentService.listKpis(assessmentId).size();
        probationAssessmentService.deleteKpi(kpiId);
        int after = probationAssessmentService.listKpis(assessmentId).size();
        if (after < before) {
            PortletUtils.addInfoMsg("KPI deleted.", request);
        } else {
            PortletUtils.addErrorMsg("KPI delete failed. You are not allowed to delete this KPI.", request);
        }
        return "redirect:/probation-assessments/view-assessment/" + assessmentId;
    }

    @RequestMapping(value = "/submit-assessment", method = RequestMethod.POST)
    public String submitAssessment(HttpServletRequest request, long id, String remarks) {
        boolean submitted = probationAssessmentService.submitAssessment(id, remarks);
        if (submitted) {
            PortletUtils.addInfoMsg("Assessment submitted for authorization.", request);
        } else {
            PortletUtils.addErrorMsg("Submission failed. Ensure an active workflow is configured and you have access to submit.", request);
        }
        return "redirect:/probation-assessments/view-assessment/" + id;
    }

    @RequestMapping(value = "/approve-assessment", method = RequestMethod.POST)
    public String approveAssessment(HttpServletRequest request, long id, String remarks) {
        boolean approved = probationAssessmentService.approveAssessment(id, remarks);
        if (approved) {
            PortletUtils.addInfoMsg("Assessment step approved.", request);
        } else {
            PortletUtils.addErrorMsg("Approval failed. You are not allowed to approve this step.", request);
        }
        return "redirect:/probation-assessments/view-assessment/" + id;
    }

    @RequestMapping(value = "/reject-assessment", method = RequestMethod.POST)
    public String rejectAssessment(HttpServletRequest request, long id, String remarks) {
        boolean rejected = probationAssessmentService.rejectAssessment(id, remarks);
        if (rejected) {
            PortletUtils.addInfoMsg("Assessment rejected and sent back for updates.", request);
        } else {
            PortletUtils.addErrorMsg("Rejection failed. You are not allowed to reject this step.", request);
        }
        return "redirect:/probation-assessments/view-assessment/" + id;
    }

    @RequestMapping("/config")
    public ModelAndView config(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.CONFIG_PROBATION_ASSESSMENT);
        modelAndView.addObject("pageTitle", "Configuration");
        modelAndView.addObject("dimensionTemplates", probationConfigService.listAllDimensionTemplates());
        modelAndView.addObject("workflowSteps", probationConfigService.listAllWorkflowSteps());
        modelAndView.addObject("dimensionTemplate", new ProbationDimensionTemplate());
        modelAndView.addObject("workflowStep", new ProbationWorkflowStep());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-dimension-template", method = RequestMethod.POST)
    public String saveDimensionTemplate(HttpServletRequest request, ProbationDimensionTemplate template) {
        probationConfigService.saveDimensionTemplate(template);
        PortletUtils.addInfoMsg("Dimension template saved.", request);
        return "redirect:/probation-assessments/config";
    }

    @RequestMapping(value = "/delete-dimension-template", method = RequestMethod.POST)
    public String deleteDimensionTemplate(HttpServletRequest request, long id) {
        probationConfigService.deactivateDimensionTemplate(id);
        PortletUtils.addInfoMsg("Dimension template deactivated.", request);
        return "redirect:/probation-assessments/config";
    }

    @RequestMapping(value = "/save-workflow-step", method = RequestMethod.POST)
    public String saveWorkflowStep(HttpServletRequest request, ProbationWorkflowStep step) {
        normalizeWorkflowStep(step);
        probationConfigService.saveWorkflowStep(step);
        PortletUtils.addInfoMsg("Workflow step saved.", request);
        return "redirect:/probation-assessments/config";
    }

    @RequestMapping(value = "/delete-workflow-step", method = RequestMethod.POST)
    public String deleteWorkflowStep(HttpServletRequest request, long id) {
        probationConfigService.deactivateWorkflowStep(id);
        PortletUtils.addInfoMsg("Workflow step deactivated.", request);
        return "redirect:/probation-assessments/config";
    }

    private void normalizeWorkflowStep(ProbationWorkflowStep step) {
        if (step.getApproverMode() == null) {
            step.setApproverMode(PMConstants.PROBATION_APPROVER_MODE_SUPERVISOR);
        }
        if (!PMConstants.PROBATION_APPROVER_MODE_ACCOUNT_TYPE.equalsIgnoreCase(step.getApproverMode())) {
            step.setApproverAccountType(null);
            step.setSameDivisionOnly(false);
        }
        if (!PMConstants.PROBATION_APPROVER_MODE_ROLE.equalsIgnoreCase(step.getApproverMode())) {
            step.setApproverRole(null);
        }
        if (!PMConstants.PROBATION_APPROVER_MODE_USER.equalsIgnoreCase(step.getApproverMode())) {
            step.setApproverAccount(null);
        }
        if (step.getStatus() == null || step.getStatus().isEmpty()) {
            step.setStatus(PMConstants.STATUS_ACTIVE);
        }
    }
}
