package hr.performancemanagement.controllers.probation;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.PerformanceImprovementPlan;
import hr.performancemanagement.entities.ProbationAssessment;
import hr.performancemanagement.entities.ProbationAssessmentDimension;
import hr.performancemanagement.entities.ProbationDimensionTemplate;
import hr.performancemanagement.entities.ProbationKpi;
import hr.performancemanagement.entities.ProbationKpiComment;
import hr.performancemanagement.entities.ProbationWorkflowStep;
import hr.performancemanagement.service.api.AccountService;
import hr.performancemanagement.service.api.AccessControlService;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.PerformanceImprovementPlanService;
import hr.performancemanagement.service.api.ProbationAssessmentService;
import hr.performancemanagement.service.api.ProbationConfigService;
import hr.performancemanagement.service.api.ReportingPeriodService;
import hr.performancemanagement.service.api.NotificationService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.constants.AccessPermissions;
import hr.performancemanagement.utils.constants.Pages;
import hr.performancemanagement.utils.dto.ProbationResultSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/probation-assessments")
public class ProbationAssessmentController {

    private static final String ATTACHMENT_DIR = "uploads/probation-attachments";
    private static final double MIN_MARK = 1.0;
    private static final double MAX_MARK = 5.0;

    @Autowired
    private AccountService accountService;
    @Autowired
    private ProbationAssessmentService probationAssessmentService;
    @Autowired
    private ProbationConfigService probationConfigService;
    @Autowired
    private PerformanceImprovementPlanService performanceImprovementPlanService;
    @Autowired
    private ReportingPeriodService reportingPeriodService;
    @Autowired
    private CommonService commonService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AccessControlService accessControlService;

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {
        List<Account> accounts = accountService.listAllAccounts();
        List<String> accountTypes = accounts.stream()
                .map(Account::getAccountType)
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        List<String> roles = accounts.stream()
                .map(Account::getRole)
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        modelAndView.addObject("pageDomain", "Performance");
        modelAndView.addObject("pageName", "Probation Assessments");
        modelAndView.addObject("accountsList", accounts);
        modelAndView.addObject("accountTypes", accountTypes);
        modelAndView.addObject("roles", roles);
        modelAndView.addObject("approverModes", probationConfigService.listApproverModes());
        modelAndView.addObject("approverModeAccountType", PMConstants.PROBATION_APPROVER_MODE_ACCOUNT_TYPE);
        modelAndView.addObject("approverModeRole", PMConstants.PROBATION_APPROVER_MODE_ROLE);
        modelAndView.addObject("approverModeUser", PMConstants.PROBATION_APPROVER_MODE_USER);
        PortletUtils.addMessagesToPage(modelAndView, request);
    }

    @RequestMapping
    public ModelAndView viewAssessments(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PROBATION_ASSESSMENTS);
        List<ProbationAssessment> assessments = probationAssessmentService.listVisibleAssessments();
        Map<Long, ProbationResultSummary> resultsByAssessmentId = new HashMap<>();
        for (ProbationAssessment assessment : assessments) {
            resultsByAssessmentId.put(assessment.getId(), probationAssessmentService.getResultSummary(assessment.getId()));
        }
        modelAndView.addObject("pageTitle", "View");
        modelAndView.addObject("assessments", assessments);
        modelAndView.addObject("resultsByAssessmentId", resultsByAssessmentId);
        modelAndView.addObject("loggedUser", commonService.getLoggedUser());
        modelAndView.addObject("canManageProbation", canCreateProbationContract(null) || canConfigureProbation());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/add-assessment")
    public String addAssessment() {
        return "redirect:/probation-assessments/add-contract";
    }

    @RequestMapping("/add-contract")
    public ModelAndView addProbationContract(HttpServletRequest request) {
        if (!canCreateProbationContract(null)) {
            PortletUtils.addErrorMsg("Only HR or an administrator can create probation contracts.", request);
            return new ModelAndView("redirect:/probation-assessments");
        }
        ModelAndView modelAndView = new ModelAndView("scorecard/probationContract");
        modelAndView.addObject("pageTitle", "Create Probation Contract");
        modelAndView.addObject("users", accountService.listAllAccounts());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-contract", method = RequestMethod.POST)
    public String saveProbationContract(HttpServletRequest request,
                                        @RequestParam("incumbentName") long incumbentId,
                                        @RequestParam("startDate") String startDate,
                                        @RequestParam("endDate") String endDate) {
        try {
            if (!canCreateProbationContract(null)) {
                PortletUtils.addErrorMsg("Only HR or an administrator can create probation contracts.", request);
                return "redirect:/probation-assessments";
            }
            Account incumbent = accountService.getAccountById(incumbentId);
            if (incumbent == null) {
                PortletUtils.addErrorMsg("Selected incumbent not found.", request);
                return "redirect:/probation-assessments/add-contract";
            }
            if (!canCreateProbationContract(incumbent)) {
                PortletUtils.addErrorMsg("You do not have permission to create a probation contract for this incumbent.", request);
                return "redirect:/probation-assessments/add-contract";
            }
            if (!PMConstants.STATUS_ACTIVE.equalsIgnoreCase(incumbent.getStatus())) {
                PortletUtils.addErrorMsg("A probation contract can only be created for an active incumbent.", request);
                return "redirect:/probation-assessments/add-contract";
            }
            if (incumbent.getSupervisor() == null) {
                PortletUtils.addErrorMsg("Assign a supervisor to the incumbent before creating the probation contract.", request);
                return "redirect:/probation-assessments/add-contract";
            }
            if (!PMConstants.STATUS_ACTIVE.equalsIgnoreCase(incumbent.getSupervisor().getStatus())
                    || incumbent.getSupervisor().getClientId() != incumbent.getClientId()) {
                PortletUtils.addErrorMsg("Assign an active supervisor from the same client before creating the probation contract.", request);
                return "redirect:/probation-assessments/add-contract";
            }
            if (probationConfigService.listActiveDimensionTemplates().isEmpty()) {
                PortletUtils.addErrorMsg("Configure at least one active personal dimension before creating a probation contract.", request);
                return "redirect:/probation-assessments/config";
            }

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (end.isBefore(start)) {
                PortletUtils.addErrorMsg("End date cannot be before start date.", request);
                return "redirect:/probation-assessments/add-contract";
            }

            ProbationAssessment assessment = new ProbationAssessment();
            assessment.setEmployee(incumbent);
            assessment.setStartDate(start.toString());
            assessment.setEndDate(end.toString());
            assessment.setPerformancePeriod(start + " - " + end);
            assessment.setStatus(PMConstants.PROBATION_STATUS_CONTRACT_CREATED);

            ProbationAssessment savedAssessment = probationAssessmentService.createAssessment(assessment);
            if (savedAssessment == null) {
                PortletUtils.addErrorMsg("Probation contract could not be created. Check for an overlapping open contract.", request);
                return "redirect:/probation-assessments/add-contract";
            }

            PortletUtils.addInfoMsg("Probation contract created. Set KPI contract items next.", request);
            return "redirect:/probation-assessments/add-kpis/" + savedAssessment.getId();
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Error creating probation contract: " + exception.getMessage(), request);
            return "redirect:/probation-assessments/add-contract";
        }
    }

    @RequestMapping("/add-kpis/{assessmentId}")
    public ModelAndView addKpisToContract(@PathVariable("assessmentId") long assessmentId, HttpServletRequest request) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return new ModelAndView("redirect:/probation-assessments");
        }

        ModelAndView modelAndView = new ModelAndView("scorecard/captureKpisProbation");
        List<ProbationKpi> kpis = probationAssessmentService.listKpis(assessmentId);
        boolean canSupervisorApprove = probationAssessmentService.isLoggedUserSupervisor(assessment)
                && (PMConstants.PROBATION_STATUS_KPI_PENDING_SUPERVISOR_APPROVAL.equalsIgnoreCase(assessment.getStatus())
                || PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_HR.equalsIgnoreCase(assessment.getStatus()));
        boolean canHrApprove = canApproveProbationKpis(assessment)
                && PMConstants.PROBATION_STATUS_KPI_PENDING_HR_APPROVAL.equalsIgnoreCase(assessment.getStatus());
        Map<Long, List<ProbationKpiComment>> kpiCommentsByKpiId = new HashMap<>();
        for (ProbationKpi kpi : kpis) {
            kpiCommentsByKpiId.put(kpi.getId(), probationAssessmentService.listKpiComments(kpi.getId()));
        }
        modelAndView.addObject("pageTitle", "Probation KPI Contract");
        modelAndView.addObject("assessment", assessment);
        modelAndView.addObject("kpis", kpis);
        modelAndView.addObject("kpiCommentsByKpiId", kpiCommentsByKpiId);
        modelAndView.addObject("approvalHistory", probationAssessmentService.listApprovalHistory(assessmentId));
        modelAndView.addObject("loggedUser", commonService.getLoggedUser());
        modelAndView.addObject("isOwner", probationAssessmentService.isLoggedUserOwner(assessment));
        modelAndView.addObject("isSupervisor", probationAssessmentService.isLoggedUserSupervisor(assessment));
        modelAndView.addObject("isHr", canApproveProbationKpis(assessment));
        modelAndView.addObject("canEditKpis", probationAssessmentService.isLoggedUserOwner(assessment) && isKpiEditableStatus(assessment.getStatus()));
        modelAndView.addObject("canSubmitKpis", probationAssessmentService.isLoggedUserOwner(assessment) && PMConstants.PROBATION_STATUS_KPI_SET.equalsIgnoreCase(assessment.getStatus()));
        modelAndView.addObject("canSupervisorApprove", canSupervisorApprove);
        modelAndView.addObject("canHrApprove", canHrApprove);
        modelAndView.addObject("canReviewKpis", canSupervisorApprove || canHrApprove);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-kpis/{assessmentId}", method = RequestMethod.POST)
    public String saveKpis(@PathVariable("assessmentId") long assessmentId,
                           HttpServletRequest request,
                           @RequestParam(value = "kpiId", required = false) List<Long> kpiIds,
                           @RequestParam(value = "name", required = false) List<String> names,
                           @RequestParam(value = "measureOfSuccess", required = false) List<String> measures,
                           @RequestParam(value = "target", required = false) List<String> targets) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        if (!probationAssessmentService.isLoggedUserOwner(assessment)) {
            PortletUtils.addErrorMsg("Only the incumbent can edit KPI contract lines.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }
        if (!isKpiEditableStatus(assessment.getStatus())) {
            PortletUtils.addErrorMsg("KPI contract cannot be edited at this stage.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }

        int savedCount = 0;
        int rowCount = names == null ? 0 : names.size();
        for (int i = 0; i < rowCount; i++) {
            String name = sanitizeValue(names.get(i));
            String measure = measures != null && i < measures.size() ? sanitizeValue(measures.get(i)) : "";
            String target = targets != null && i < targets.size() ? sanitizeValue(targets.get(i)) : "";
            if (!StringUtils.hasText(name) && !StringUtils.hasText(measure) && !StringUtils.hasText(target)) {
                continue;
            }
            if (!StringUtils.hasText(name)) {
                continue;
            }

            ProbationKpi kpi;
            Long requestedId = kpiIds != null && i < kpiIds.size() ? kpiIds.get(i) : null;
            if (requestedId != null && requestedId > 0) {
                kpi = probationAssessmentService.getKpiById(requestedId);
                if (kpi == null || kpi.getAssessment() == null || kpi.getAssessment().getId() != assessmentId) {
                    continue;
                }
            } else {
                kpi = new ProbationKpi();
                kpi.setAssessment(assessment);
            }

            kpi.setName(name);
            kpi.setMeasureOfSuccess(measure);
            kpi.setTarget(target);
            kpi.setStatus(PMConstants.STATUS_ACTIVE);
            probationAssessmentService.addKpi(assessmentId, kpi);
            savedCount++;
        }

        if (savedCount == 0 && probationAssessmentService.listKpis(assessmentId).isEmpty()) {
            PortletUtils.addErrorMsg("Add at least one KPI before saving.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }

        probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_KPI_SET);
        PortletUtils.addInfoMsg("KPI contract saved.", request);
        return "redirect:/probation-assessments/add-kpis/" + assessmentId;
    }

    @RequestMapping(value = "/delete-kpi", method = RequestMethod.POST)
    public String deleteKpi(HttpServletRequest request, long assessmentId, long kpiId) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        if (!probationAssessmentService.isLoggedUserOwner(assessment) || !isKpiEditableStatus(assessment.getStatus())) {
            PortletUtils.addErrorMsg("KPI cannot be deleted at this stage.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }

        if (probationAssessmentService.deleteKpi(kpiId)) {
            PortletUtils.addInfoMsg("KPI deleted.", request);
        } else {
            PortletUtils.addErrorMsg("KPI could not be deleted. Reviewed KPI items must be retained for audit history.", request);
        }
        return "redirect:/probation-assessments/add-kpis/" + assessmentId;
    }

    @RequestMapping(value = "/submit-kpis-for-approval/{assessmentId}", method = RequestMethod.POST)
    public String submitKpisForApproval(@PathVariable("assessmentId") long assessmentId, HttpServletRequest request) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        if (!probationAssessmentService.isLoggedUserOwner(assessment)) {
            PortletUtils.addErrorMsg("Only the incumbent can submit KPI contract for approval.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }
        if (probationAssessmentService.listKpis(assessmentId).isEmpty()) {
            PortletUtils.addErrorMsg("Cannot submit without KPI lines.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }
        if (!PMConstants.PROBATION_STATUS_KPI_SET.equalsIgnoreCase(assessment.getStatus())) {
            PortletUtils.addErrorMsg("KPI contract can only be submitted after it has been saved.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }
        String contractIssue = findKpiContractIssue(probationAssessmentService.listKpis(assessmentId));
        if (contractIssue != null) {
            PortletUtils.addErrorMsg(contractIssue, request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }

        probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_KPI_PENDING_SUPERVISOR_APPROVAL);
        probationAssessmentService.recordReviewAction(assessmentId, "KPI Contract", PMConstants.PROBATION_ACTION_SUBMITTED, null);
        notifySupervisorKpiSubmission(request, assessment);
        PortletUtils.addInfoMsg("KPI contract submitted to supervisor.", request);
        return "redirect:/probation-assessments/add-kpis/" + assessmentId;
    }

    @RequestMapping(value = "/approve-kpis-supervisor/{assessmentId}", method = RequestMethod.POST)
    public String approveKpisBySupervisor(@PathVariable("assessmentId") long assessmentId, HttpServletRequest request) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        if (!probationAssessmentService.isLoggedUserSupervisor(assessment)) {
            PortletUtils.addErrorMsg("Only the supervisor can approve this stage.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }
        if (!PMConstants.PROBATION_STATUS_KPI_PENDING_SUPERVISOR_APPROVAL.equalsIgnoreCase(assessment.getStatus())
                && !PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_HR.equalsIgnoreCase(assessment.getStatus())) {
            PortletUtils.addErrorMsg("This assessment is not pending supervisor approval.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }

        probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_KPI_PENDING_HR_APPROVAL);
        probationAssessmentService.recordReviewAction(assessmentId, "Supervisor KPI Review", PMConstants.PROBATION_ACTION_APPROVED, null);
        notifyHrKpiSubmission(request, assessment);
        notifyIncumbentHrPendingInformative(request, assessment);
        PortletUtils.addInfoMsg("Supervisor approved KPI contract. Sent to HR.", request);
        return "redirect:/probation-assessments/add-kpis/" + assessmentId;
    }

    @RequestMapping(value = "/reject-kpis-supervisor/{assessmentId}", method = RequestMethod.POST)
    public String rejectKpisBySupervisor(@PathVariable("assessmentId") long assessmentId,
                                         HttpServletRequest request,
                                         @RequestParam(value = "remarks", required = false) String remarks) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        if (!probationAssessmentService.isLoggedUserSupervisor(assessment)) {
            PortletUtils.addErrorMsg("Only the supervisor can reject this stage.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }
        if (!PMConstants.PROBATION_STATUS_KPI_PENDING_SUPERVISOR_APPROVAL.equalsIgnoreCase(assessment.getStatus())
                && !PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_HR.equalsIgnoreCase(assessment.getStatus())) {
            PortletUtils.addErrorMsg("This assessment is not pending supervisor action.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }
        if (!StringUtils.hasText(remarks)) {
            PortletUtils.addErrorMsg("A rejection reason is required.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }

        probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_SUPERVISOR);
        probationAssessmentService.recordReviewAction(assessmentId, "Supervisor KPI Review", PMConstants.PROBATION_ACTION_REJECTED, remarks);
        notifyIncumbentAndSupervisorKpiDecision(request, assessment, "Rejected by Supervisor", remarks, false);
        String msg = StringUtils.hasText(remarks) ? "Supervisor rejected KPI contract: " + remarks : "Supervisor rejected KPI contract.";
        PortletUtils.addInfoMsg(msg, request);
        return "redirect:/probation-assessments/add-kpis/" + assessmentId;
    }

    @RequestMapping(value = "/approve-kpis-hr/{assessmentId}", method = RequestMethod.POST)
    public String approveKpisByHr(@PathVariable("assessmentId") long assessmentId, HttpServletRequest request) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        if (!canApproveProbationKpis(assessment)) {
            PortletUtils.addErrorMsg("Only HR can approve this stage.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }
        if (!PMConstants.PROBATION_STATUS_KPI_PENDING_HR_APPROVAL.equalsIgnoreCase(assessment.getStatus())) {
            PortletUtils.addErrorMsg("This assessment is not pending HR approval.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }
        probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_KPI_APPROVED);
        probationAssessmentService.recordReviewAction(assessmentId, "HR KPI Review", PMConstants.PROBATION_ACTION_APPROVED, null);
        notifyIncumbentAndSupervisorKpiDecision(request, assessment, "Approved by HR", null, false);
        PortletUtils.addInfoMsg("HR approved KPI contract. Incumbent can now submit evaluation.", request);
        return "redirect:/probation-assessments/evaluate/" + assessmentId;
    }

    @RequestMapping(value = "/reject-kpis-hr/{assessmentId}", method = RequestMethod.POST)
    public String rejectKpisByHr(@PathVariable("assessmentId") long assessmentId,
                                 HttpServletRequest request,
                                 @RequestParam(value = "remarks", required = false) String remarks) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        if (!canApproveProbationKpis(assessment)) {
            PortletUtils.addErrorMsg("Only HR can reject this stage.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }
        if (!PMConstants.PROBATION_STATUS_KPI_PENDING_HR_APPROVAL.equalsIgnoreCase(assessment.getStatus())) {
            PortletUtils.addErrorMsg("This assessment is not pending HR approval.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }
        if (!StringUtils.hasText(remarks)) {
            PortletUtils.addErrorMsg("A rejection reason is required.", request);
            return "redirect:/probation-assessments/add-kpis/" + assessmentId;
        }

        probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_HR);
        probationAssessmentService.recordReviewAction(assessmentId, "HR KPI Review", PMConstants.PROBATION_ACTION_REJECTED, remarks);
        notifyIncumbentAndSupervisorKpiDecision(request, assessment, "Rejected by HR", remarks, true);
        String msg = StringUtils.hasText(remarks)
                ? "HR rejected KPI contract and returned it to supervisor: " + remarks
                : "HR rejected KPI contract and returned it to supervisor.";
        PortletUtils.addInfoMsg(msg, request);
        return "redirect:/probation-assessments/add-kpis/" + assessmentId;
    }

    @RequestMapping(value = "/save-kpi-flag", method = RequestMethod.POST)
    public String saveKpiFlag(HttpServletRequest request,
                              @RequestParam("assessmentId") long assessmentId,
                              @RequestParam("kpiId") long kpiId,
                              @RequestParam(value = "flagReason", required = false) String flagReason) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        ProbationKpi saved = probationAssessmentService.saveKpiFlag(kpiId, flagReason);
        if (saved == null) {
            PortletUtils.addErrorMsg("KPI flag update failed. Only current reviewer can flag KPI items.", request);
        } else {
            PortletUtils.addInfoMsg("KPI flag saved.", request);
        }
        return "redirect:/probation-assessments/add-kpis/" + assessmentId;
    }

    @RequestMapping(value = "/save-kpi-comment", method = RequestMethod.POST)
    public String saveKpiComment(HttpServletRequest request,
                                 @RequestParam("assessmentId") long assessmentId,
                                 @RequestParam("kpiId") long kpiId,
                                 @RequestParam("message") String message) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        ProbationKpiComment saved = probationAssessmentService.saveKpiComment(kpiId, message);
        if (saved == null) {
            PortletUtils.addErrorMsg("KPI comment was not saved. Only current reviewer can comment at this stage.", request);
        } else {
            PortletUtils.addInfoMsg("KPI comment saved.", request);
        }
        return "redirect:/probation-assessments/add-kpis/" + assessmentId;
    }

    @RequestMapping("/evaluate/{assessmentId}")
    public ModelAndView evaluateProbation(@PathVariable("assessmentId") long assessmentId, HttpServletRequest request) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return new ModelAndView("redirect:/probation-assessments");
        }

        if (isDimensionStageStatus(assessment.getStatus())) {
            return new ModelAndView("redirect:/probation-assessments/dimensions/" + assessmentId);
        }

        ModelAndView modelAndView = new ModelAndView("probation-assessment/evaluateProbation");
        modelAndView.addObject("pageTitle", "Probation KPI Evaluation");
        modelAndView.addObject("assessment", assessment);
        modelAndView.addObject("kpis", probationAssessmentService.listKpis(assessmentId));
        modelAndView.addObject("isOwner", probationAssessmentService.isLoggedUserOwner(assessment));
        modelAndView.addObject("isSupervisor", probationAssessmentService.isLoggedUserSupervisor(assessment));
        modelAndView.addObject("canIncumbentEvaluate", probationAssessmentService.isLoggedUserOwner(assessment)
                && (PMConstants.PROBATION_STATUS_KPI_APPROVED.equalsIgnoreCase(assessment.getStatus())
                || PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_SUPERVISOR.equalsIgnoreCase(assessment.getStatus())));
        modelAndView.addObject("canSupervisorReview", probationAssessmentService.isLoggedUserSupervisor(assessment)
                && PMConstants.PROBATION_STATUS_EVALUATION_PENDING_SUPERVISOR_REVIEW.equalsIgnoreCase(assessment.getStatus()));
        modelAndView.addObject("resultSummary", probationAssessmentService.getResultSummary(assessmentId));
        modelAndView.addObject("approvalHistory", probationAssessmentService.listApprovalHistory(assessmentId));
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/submit-incumbent-evaluation/{assessmentId}", method = RequestMethod.POST)
    public String submitIncumbentEvaluation(@PathVariable("assessmentId") long assessmentId,
                                            HttpServletRequest request) {
        try {
            ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
            if (assessment == null) {
                PortletUtils.addErrorMsg("Probation assessment not found.", request);
                return "redirect:/probation-assessments";
            }
            if (!probationAssessmentService.isLoggedUserOwner(assessment)) {
                PortletUtils.addErrorMsg("Only the incumbent can submit this step.", request);
                return "redirect:/probation-assessments/evaluate/" + assessmentId;
            }
            if (!PMConstants.PROBATION_STATUS_KPI_APPROVED.equalsIgnoreCase(assessment.getStatus())
                    && !PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_SUPERVISOR.equalsIgnoreCase(assessment.getStatus())) {
                PortletUtils.addErrorMsg("Evaluation cannot be submitted at this stage.", request);
                return "redirect:/probation-assessments/evaluate/" + assessmentId;
            }

            List<ProbationKpi> kpis = probationAssessmentService.listKpis(assessmentId);
            if (kpis.isEmpty()) {
                throw new IllegalArgumentException("No KPI lines are available for evaluation.");
            }
            for (ProbationKpi kpi : kpis) {
                if (kpi.getIncumbentMark() == null) {
                    throw new IllegalArgumentException("Save incumbent mark for KPI " + defaultText(kpi.getName(), "ID " + kpi.getId()) + " before submitting.");
                }
                if (kpi.getProgressPercent() == null) {
                    throw new IllegalArgumentException("Save progress for KPI " + defaultText(kpi.getName(), "ID " + kpi.getId()) + " before submitting.");
                }
                ensureMarkRange(kpi.getIncumbentMark(), kpi, "Incumbent mark");
                ensureProgressRange(kpi.getProgressPercent(), kpi);
            }

            probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_EVALUATION_PENDING_SUPERVISOR_REVIEW);
            probationAssessmentService.recordReviewAction(assessmentId, "Incumbent Evaluation", PMConstants.PROBATION_ACTION_SUBMITTED, null);
            PortletUtils.addInfoMsg("Evaluation submitted to supervisor.", request);
            return "redirect:/probation-assessments/evaluate/" + assessmentId;
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Failed to submit evaluation: " + exception.getMessage(), request);
            return "redirect:/probation-assessments/evaluate/" + assessmentId;
        }
    }

    @RequestMapping(value = "/save-incumbent-evaluation-line/{assessmentId}", method = RequestMethod.POST)
    public String saveIncumbentEvaluationLine(@PathVariable("assessmentId") long assessmentId,
                                              HttpServletRequest request,
                                              @RequestParam("saveKpiId") long kpiId,
                                              @RequestParam Map<String, String> allRequestParams) {
        try {
            ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
            if (assessment == null) {
                PortletUtils.addErrorMsg("Probation assessment not found.", request);
                return "redirect:/probation-assessments";
            }
            if (!probationAssessmentService.isLoggedUserOwner(assessment)) {
                PortletUtils.addErrorMsg("Only the incumbent can save this row.", request);
                return "redirect:/probation-assessments/evaluate/" + assessmentId;
            }
            if (!PMConstants.PROBATION_STATUS_KPI_APPROVED.equalsIgnoreCase(assessment.getStatus())
                    && !PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_SUPERVISOR.equalsIgnoreCase(assessment.getStatus())) {
                PortletUtils.addErrorMsg("Evaluation line cannot be saved at this stage.", request);
                return "redirect:/probation-assessments/evaluate/" + assessmentId;
            }

            ProbationKpi kpi = probationAssessmentService.getKpiById(kpiId);
            if (kpi == null || kpi.getAssessment() == null || kpi.getAssessment().getId() != assessmentId) {
                PortletUtils.addErrorMsg("KPI line not found for this assessment.", request);
                return "redirect:/probation-assessments/evaluate/" + assessmentId;
            }

            String progressPercent = sanitizeValue(allRequestParams.get("progress_" + kpi.getId()));
            String incumbentMark = sanitizeValue(allRequestParams.get("incumbentMark_" + kpi.getId()));
            String incumbentComment = sanitizeValue(allRequestParams.get("incumbentComment_" + kpi.getId()));

            if (hasText(progressPercent)) {
                kpi.setProgressPercent(parseAndValidateProgressPercent(progressPercent, kpi));
            }
            kpi.setIncumbentMark(parseAndValidateMark(incumbentMark, kpi, "Incumbent mark"));
            kpi.setIncumbentComment(incumbentComment);

            MultipartHttpServletRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
            MultipartFile file = multipartRequest == null ? null : multipartRequest.getFile("attachment_" + kpi.getId());
            if (file != null && !file.isEmpty()) {
                kpi.setAttachmentPath(storeAttachmentFile(file));
            }

            ProbationKpi saved = probationAssessmentService.updateKpi(kpi);
            if (saved == null) {
                PortletUtils.addErrorMsg("KPI line could not be saved.", request);
            } else {
                PortletUtils.addInfoMsg("KPI evaluation row saved.", request);
            }
            return "redirect:/probation-assessments/evaluate/" + assessmentId;
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Failed to save KPI evaluation row: " + exception.getMessage(), request);
            return "redirect:/probation-assessments/evaluate/" + assessmentId;
        }
    }

    @RequestMapping(value = "/submit-supervisor-review/{assessmentId}", method = RequestMethod.POST)
    public String submitSupervisorReview(@PathVariable("assessmentId") long assessmentId,
                                         HttpServletRequest request,
                                         @RequestParam Map<String, String> allRequestParams) {
        try {
            ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
            if (assessment == null) {
                PortletUtils.addErrorMsg("Probation assessment not found.", request);
                return "redirect:/probation-assessments";
            }
            if (!probationAssessmentService.isLoggedUserSupervisor(assessment)) {
                PortletUtils.addErrorMsg("Only the supervisor can submit this step.", request);
                return "redirect:/probation-assessments/evaluate/" + assessmentId;
            }
            if (!PMConstants.PROBATION_STATUS_EVALUATION_PENDING_SUPERVISOR_REVIEW.equalsIgnoreCase(assessment.getStatus())) {
                PortletUtils.addErrorMsg("Assessment is not awaiting supervisor review.", request);
                return "redirect:/probation-assessments/evaluate/" + assessmentId;
            }

            List<ProbationKpi> kpis = probationAssessmentService.listKpis(assessmentId);
            if (kpis.isEmpty()) {
                throw new IllegalArgumentException("No KPI lines are available for supervisor review.");
            }
            for (ProbationKpi kpi : kpis) {
                String supervisorMark = sanitizeValue(allRequestParams.get("supervisorMark_" + kpi.getId()));
                String supervisorComment = sanitizeValue(allRequestParams.get("supervisorComment_" + kpi.getId()));
                kpi.setSupervisorMark(parseAndValidateMark(supervisorMark, kpi, "Supervisor mark"));
                kpi.setSupervisorComment(supervisorComment);
                probationAssessmentService.updateKpi(kpi);
            }

            probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_PERSONAL_DIMENSIONS_IN_PROGRESS);
            probationAssessmentService.recordReviewAction(assessmentId, "Supervisor Evaluation Review", PMConstants.PROBATION_ACTION_APPROVED, null);
            PortletUtils.addInfoMsg("Supervisor review saved. Continue with personal dimensions.", request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId + "?step=0";
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Failed to submit supervisor review: " + exception.getMessage(), request);
            return "redirect:/probation-assessments/evaluate/" + assessmentId;
        }
    }

    @RequestMapping(value = "/reject-supervisor-review/{assessmentId}", method = RequestMethod.POST)
    public String rejectSupervisorReview(@PathVariable("assessmentId") long assessmentId,
                                         HttpServletRequest request,
                                         @RequestParam(value = "remarks", required = false) String remarks) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        if (!probationAssessmentService.isLoggedUserSupervisor(assessment)) {
            PortletUtils.addErrorMsg("Only the supervisor can reject this step.", request);
            return "redirect:/probation-assessments/evaluate/" + assessmentId;
        }
        if (!PMConstants.PROBATION_STATUS_EVALUATION_PENDING_SUPERVISOR_REVIEW.equalsIgnoreCase(assessment.getStatus())) {
            PortletUtils.addErrorMsg("Assessment is not awaiting supervisor review.", request);
            return "redirect:/probation-assessments/evaluate/" + assessmentId;
        }
        if (!StringUtils.hasText(remarks)) {
            PortletUtils.addErrorMsg("A reason for returning the evaluation is required.", request);
            return "redirect:/probation-assessments/evaluate/" + assessmentId;
        }

        probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_SUPERVISOR);
        probationAssessmentService.recordReviewAction(assessmentId, "Supervisor Evaluation Review", PMConstants.PROBATION_ACTION_REJECTED, remarks);
        String msg = StringUtils.hasText(remarks) ? "Evaluation sent back: " + remarks : "Evaluation sent back to incumbent.";
        PortletUtils.addInfoMsg(msg, request);
        return "redirect:/probation-assessments/evaluate/" + assessmentId;
    }

    @RequestMapping("/dimensions/{assessmentId}")
    public ModelAndView captureDimensions(@PathVariable("assessmentId") long assessmentId,
                                          @RequestParam(value = "step", required = false, defaultValue = "0") int step,
                                          HttpServletRequest request) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return new ModelAndView("redirect:/probation-assessments");
        }

        if (isEvaluationStageStatus(assessment.getStatus())) {
            return new ModelAndView("redirect:/probation-assessments/evaluate/" + assessmentId);
        }

        List<ProbationAssessmentDimension> dimensions = probationAssessmentService.listAssessmentDimensions(assessmentId);
        if (dimensions.isEmpty()) {
            PortletUtils.addErrorMsg("No personal dimensions configured.", request);
            return new ModelAndView("redirect:/probation-assessments/config");
        }

        int total = dimensions.size();
        int stepIndex = Math.max(0, Math.min(step, total - 1));
        ProbationAssessmentDimension current = dimensions.get(stepIndex);

        boolean isSupervisor = probationAssessmentService.isLoggedUserSupervisor(assessment);
        boolean isHr = canApproveFinalProbation(assessment);
        boolean dimensionEditStage = PMConstants.PROBATION_STATUS_PERSONAL_DIMENSIONS_IN_PROGRESS.equalsIgnoreCase(assessment.getStatus())
                || PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_HR.equalsIgnoreCase(assessment.getStatus());

        ModelAndView modelAndView = new ModelAndView("probation-assessment/captureDimensionsStep");
        modelAndView.addObject("pageTitle", "Personal Dimensions");
        modelAndView.addObject("assessment", assessment);
        modelAndView.addObject("dimensions", dimensions);
        modelAndView.addObject("currentDimension", current);
        modelAndView.addObject("pipPlans", performanceImprovementPlanService.listAllPerformanceImprovementPlansByEmployee(assessment.getEmployee()));
        modelAndView.addObject("stepIndex", stepIndex);
        modelAndView.addObject("totalSteps", total);
        modelAndView.addObject("firstStep", stepIndex == 0);
        modelAndView.addObject("lastStep", stepIndex == total - 1);
        modelAndView.addObject("canEditDimension", isSupervisor && dimensionEditStage);
        modelAndView.addObject("canSubmitToHr", isSupervisor && dimensionEditStage && stepIndex == total - 1);
        modelAndView.addObject("showHrActions", isHr && PMConstants.PROBATION_STATUS_EVALUATION_PENDING_HR_APPROVAL.equalsIgnoreCase(assessment.getStatus()));
        modelAndView.addObject("isCompleted", PMConstants.PROBATION_STATUS_EVALUATION_COMPLETED.equalsIgnoreCase(assessment.getStatus()));
        modelAndView.addObject("resultSummary", probationAssessmentService.getResultSummary(assessmentId));
        modelAndView.addObject("approvalHistory", probationAssessmentService.listApprovalHistory(assessmentId));
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-dimension-step/{assessmentId}", method = RequestMethod.POST)
    public String saveDimensionStep(@PathVariable("assessmentId") long assessmentId,
                                    HttpServletRequest request,
                                    @RequestParam("dimensionId") long dimensionId,
                                    @RequestParam("strengths") String strengths,
                                    @RequestParam("areasForImprovement") String areasForImprovement,
                                    @RequestParam(value = "linkedPlanId", required = false) Long linkedPlanId,
                                    @RequestParam(value = "createNewPlan", required = false) String createNewPlan,
                                    @RequestParam(value = "stepIndex", required = false, defaultValue = "0") int stepIndex,
                                    @RequestParam(value = "action", required = false, defaultValue = "save") String action) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }

        boolean editableStage = PMConstants.PROBATION_STATUS_PERSONAL_DIMENSIONS_IN_PROGRESS.equalsIgnoreCase(assessment.getStatus())
                || PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_HR.equalsIgnoreCase(assessment.getStatus());
        if (!probationAssessmentService.isLoggedUserSupervisor(assessment) || !editableStage) {
            PortletUtils.addErrorMsg("Personal dimensions are not editable at this stage.", request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId + "?step=" + Math.max(stepIndex, 0);
        }

        ProbationAssessmentDimension current = probationAssessmentService.getAssessmentDimensionById(dimensionId);
        if (current == null || current.getAssessment() == null || current.getAssessment().getId() != assessmentId) {
            PortletUtils.addErrorMsg("Dimension step not found.", request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId + "?step=" + Math.max(stepIndex, 0);
        }

        Long planId = linkedPlanId;
        if ("yes".equalsIgnoreCase(createNewPlan) && StringUtils.hasText(areasForImprovement)) {
            PerformanceImprovementPlan plan = createImprovementPlan(assessment, current, areasForImprovement);
            if (plan != null) {
                planId = plan.getId();
                PortletUtils.addInfoMsg("Area linked to a new performance improvement plan.", request);
            }
        }

        ProbationAssessmentDimension saved = probationAssessmentService.saveDimensionResponse(
                assessmentId,
                current.getDimensionTemplate().getId(),
                sanitizeValue(strengths),
                sanitizeValue(areasForImprovement),
                planId
        );

        if (saved == null) {
            PortletUtils.addErrorMsg("Failed to save personal dimension step.", request);
        } else if (!"yes".equalsIgnoreCase(createNewPlan)) {
            PortletUtils.addInfoMsg("Dimension step saved.", request);
        }

        List<ProbationAssessmentDimension> dimensions = probationAssessmentService.listAssessmentDimensions(assessmentId);
        int nextStep = Math.max(0, Math.min(stepIndex, Math.max(0, dimensions.size() - 1)));
        if ("next".equalsIgnoreCase(action)) {
            nextStep = Math.min(nextStep + 1, Math.max(0, dimensions.size() - 1));
        } else if ("previous".equalsIgnoreCase(action)) {
            nextStep = Math.max(nextStep - 1, 0);
        }
        return "redirect:/probation-assessments/dimensions/" + assessmentId + "?step=" + nextStep;
    }

    @RequestMapping(value = "/submit-dimensions/{assessmentId}", method = RequestMethod.POST)
    public String submitDimensionsToHr(@PathVariable("assessmentId") long assessmentId,
                                       HttpServletRequest request,
                                       @RequestParam(value = "generalObservations", required = false) String generalObservations) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }

        boolean editableStage = PMConstants.PROBATION_STATUS_PERSONAL_DIMENSIONS_IN_PROGRESS.equalsIgnoreCase(assessment.getStatus())
                || PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_HR.equalsIgnoreCase(assessment.getStatus());
        if (!probationAssessmentService.isLoggedUserSupervisor(assessment) || !editableStage) {
            PortletUtils.addErrorMsg("Only the supervisor can submit this stage.", request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId;
        }
        if (!StringUtils.hasText(generalObservations)) {
            PortletUtils.addErrorMsg("General observations are required before submission to HR.", request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId;
        }
        String dimensionIssue = findDimensionIssue(probationAssessmentService.listAssessmentDimensions(assessmentId));
        if (dimensionIssue != null) {
            PortletUtils.addErrorMsg(dimensionIssue, request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId;
        }

        assessment.setGeneralObservations(sanitizeValue(generalObservations));
        probationAssessmentService.updateAssessmentCore(assessment);
        probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_EVALUATION_PENDING_HR_APPROVAL);
        probationAssessmentService.recordReviewAction(assessmentId, "Personal Dimensions", PMConstants.PROBATION_ACTION_SUBMITTED, generalObservations);
        PortletUtils.addInfoMsg("Personal dimensions and general observations submitted to HR.", request);
        int last = Math.max(probationAssessmentService.listAssessmentDimensions(assessmentId).size() - 1, 0);
        return "redirect:/probation-assessments/dimensions/" + assessmentId + "?step=" + last;
    }

    @RequestMapping(value = "/approve-evaluation-hr/{assessmentId}", method = RequestMethod.POST)
    public String approveEvaluationByHr(@PathVariable("assessmentId") long assessmentId, HttpServletRequest request) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        if (!canApproveFinalProbation(assessment)) {
            PortletUtils.addErrorMsg("Only HR can approve final probation evaluation.", request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId;
        }
        if (!PMConstants.PROBATION_STATUS_EVALUATION_PENDING_HR_APPROVAL.equalsIgnoreCase(assessment.getStatus())) {
            PortletUtils.addErrorMsg("Assessment is not awaiting HR final approval.", request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId;
        }
        ProbationResultSummary resultSummary = probationAssessmentService.getResultSummary(assessmentId);
        if (!resultSummary.isComplete()) {
            PortletUtils.addErrorMsg("Final approval is blocked because KPI marks or progress are incomplete.", request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId;
        }
        String dimensionIssue = findDimensionIssue(probationAssessmentService.listAssessmentDimensions(assessmentId));
        if (dimensionIssue != null || !StringUtils.hasText(assessment.getGeneralObservations())) {
            PortletUtils.addErrorMsg(dimensionIssue == null
                    ? "General observations are required before final approval."
                    : dimensionIssue, request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId;
        }

        probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_EVALUATION_COMPLETED);
        probationAssessmentService.recordReviewAction(assessmentId, "HR Final Evaluation", PMConstants.PROBATION_ACTION_APPROVED, resultSummary.getRecommendation());
        PortletUtils.addInfoMsg("HR approved final probation evaluation.", request);
        return "redirect:/probation-assessments/dimensions/" + assessmentId;
    }

    @RequestMapping(value = "/reject-evaluation-hr/{assessmentId}", method = RequestMethod.POST)
    public String rejectEvaluationByHr(@PathVariable("assessmentId") long assessmentId,
                                       HttpServletRequest request,
                                       @RequestParam(value = "remarks", required = false) String remarks) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(assessmentId);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Probation assessment not found.", request);
            return "redirect:/probation-assessments";
        }
        if (!canApproveFinalProbation(assessment)) {
            PortletUtils.addErrorMsg("Only HR can reject final probation evaluation.", request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId;
        }
        if (!PMConstants.PROBATION_STATUS_EVALUATION_PENDING_HR_APPROVAL.equalsIgnoreCase(assessment.getStatus())) {
            PortletUtils.addErrorMsg("Assessment is not awaiting HR final approval.", request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId;
        }
        if (!StringUtils.hasText(remarks)) {
            PortletUtils.addErrorMsg("A rejection reason is required.", request);
            return "redirect:/probation-assessments/dimensions/" + assessmentId;
        }

        probationAssessmentService.updateAssessmentStatus(assessmentId, PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_HR);
        probationAssessmentService.recordReviewAction(assessmentId, "HR Final Evaluation", PMConstants.PROBATION_ACTION_REJECTED, remarks);
        String msg = StringUtils.hasText(remarks) ? "HR requested updates: " + remarks : "HR requested updates to supervisor submission.";
        PortletUtils.addInfoMsg(msg, request);
        return "redirect:/probation-assessments/dimensions/" + assessmentId;
    }

    @RequestMapping("/view-assessment/{id}")
    public ModelAndView viewAssessment(@PathVariable("id") long id, HttpServletRequest request) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(id);
        if (assessment == null) {
            PortletUtils.addErrorMsg("Assessment not found.", request);
            return new ModelAndView("redirect:/probation-assessments");
        }

        String status = assessment.getStatus();
        if (PMConstants.PROBATION_STATUS_KPI_APPROVED.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_EVALUATION_PENDING_SUPERVISOR_REVIEW.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_SUPERVISOR.equalsIgnoreCase(status)) {
            return new ModelAndView("redirect:/probation-assessments/evaluate/" + id);
        }
        if (isDimensionStageStatus(status)) {
            return new ModelAndView("redirect:/probation-assessments/dimensions/" + id);
        }
        return new ModelAndView("redirect:/probation-assessments/add-kpis/" + id);
    }

    @RequestMapping("/download-attachment/{kpiId}")
    public void downloadAttachment(@PathVariable("kpiId") long kpiId, HttpServletResponse response) throws IOException {
        ProbationKpi kpi = probationAssessmentService.getKpiById(kpiId);
        if (kpi == null || kpi.getAssessment() == null || probationAssessmentService.getAssessmentById(kpi.getAssessment().getId()) == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        if (!StringUtils.hasText(kpi.getAttachmentPath())) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        String safeName;
        try {
            safeName = sanitizeFileName(kpi.getAttachmentPath());
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }

        Path file = resolveAttachmentDirectory().resolve(safeName).normalize();
        if (!file.startsWith(resolveAttachmentDirectory()) || !Files.exists(file) || !Files.isRegularFile(file)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        String contentType = Files.probeContentType(file);
        if (!StringUtils.hasText(contentType)) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + UriUtils.encode(safeName, StandardCharsets.UTF_8));
        response.setContentLengthLong(Files.size(file));

        try (InputStream inputStream = Files.newInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
    }

    @RequestMapping("/config")
    public ModelAndView config(HttpServletRequest request) {
        if (!canConfigureProbation()) {
            PortletUtils.addErrorMsg("Only HR or an administrator can configure probation assessments.", request);
            return new ModelAndView("redirect:/probation-assessments");
        }
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
        if (!canConfigureProbation()) {
            PortletUtils.addErrorMsg("Only HR or an administrator can configure probation assessments.", request);
            return "redirect:/probation-assessments";
        }
        boolean update = template != null && template.getId() > 0;
        ProbationDimensionTemplate savedTemplate = probationConfigService.saveDimensionTemplate(template);
        if (savedTemplate == null) {
            PortletUtils.addErrorMsg("Dimension template could not be saved.", request);
        } else {
            PortletUtils.addInfoMsg(update ? "Dimension template updated." : "Dimension template saved.", request);
        }
        return "redirect:/probation-assessments/config";
    }

    @RequestMapping(value = "/delete-dimension-template", method = RequestMethod.POST)
    public String deleteDimensionTemplate(HttpServletRequest request, long id) {
        if (!canConfigureProbation()) {
            PortletUtils.addErrorMsg("Only HR or an administrator can configure probation assessments.", request);
            return "redirect:/probation-assessments";
        }
        boolean deleted = probationConfigService.deleteDimensionTemplate(id);
        if (deleted) {
            PortletUtils.addInfoMsg("Dimension template deleted.", request);
        } else {
            PortletUtils.addInfoMsg("Dimension template is used by existing assessments, so it was marked inactive instead.", request);
        }
        return "redirect:/probation-assessments/config";
    }

    @RequestMapping(value = "/save-workflow-step", method = RequestMethod.POST)
    public String saveWorkflowStep(HttpServletRequest request, ProbationWorkflowStep step) {
        if (!canConfigureProbation()) {
            PortletUtils.addErrorMsg("Only HR or an administrator can configure probation assessments.", request);
            return "redirect:/probation-assessments";
        }
        normalizeWorkflowStep(step);
        probationConfigService.saveWorkflowStep(step);
        PortletUtils.addInfoMsg("Workflow step saved.", request);
        return "redirect:/probation-assessments/config";
    }

    @RequestMapping(value = "/delete-workflow-step", method = RequestMethod.POST)
    public String deleteWorkflowStep(HttpServletRequest request, long id) {
        if (!canConfigureProbation()) {
            PortletUtils.addErrorMsg("Only HR or an administrator can configure probation assessments.", request);
            return "redirect:/probation-assessments";
        }
        probationConfigService.deactivateWorkflowStep(id);
        PortletUtils.addInfoMsg("Workflow step deactivated.", request);
        return "redirect:/probation-assessments/config";
    }

    private boolean isKpiEditableStatus(String status) {
        return PMConstants.PROBATION_STATUS_CONTRACT_CREATED.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_KPI_SET.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_SUPERVISOR.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_DRAFT.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_REJECTED.equalsIgnoreCase(status);
    }

    private boolean isEvaluationStageStatus(String status) {
        return PMConstants.PROBATION_STATUS_KPI_APPROVED.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_EVALUATION_PENDING_SUPERVISOR_REVIEW.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_SUPERVISOR.equalsIgnoreCase(status);
    }

    private boolean isDimensionStageStatus(String status) {
        return PMConstants.PROBATION_STATUS_PERSONAL_DIMENSIONS_IN_PROGRESS.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_EVALUATION_PENDING_HR_APPROVAL.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_HR.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_EVALUATION_COMPLETED.equalsIgnoreCase(status);
    }

    private boolean canCreateProbationContract(Account subject) {
        if (subject == null) {
            return accessControlService.hasPermissionForAnyScope(commonService.getLoggedUser(),
                    AccessPermissions.PROBATION_CREATE_CONTRACT);
        }
        return accessControlService.hasPermission(commonService.getLoggedUser(),
                AccessPermissions.PROBATION_CREATE_CONTRACT, subject);
    }

    private boolean canApproveProbationKpis(ProbationAssessment assessment) {
        return accessControlService.hasPermission(commonService.getLoggedUser(),
                AccessPermissions.PROBATION_APPROVE_KPI_CONTRACT,
                assessment == null ? null : assessment.getEmployee());
    }

    private boolean canApproveFinalProbation(ProbationAssessment assessment) {
        return accessControlService.hasPermission(commonService.getLoggedUser(),
                AccessPermissions.PROBATION_APPROVE_FINAL_ASSESSMENT,
                assessment == null ? null : assessment.getEmployee());
    }

    private boolean canConfigureProbation() {
        return accessControlService.hasPermission(AccessPermissions.PROBATION_CONFIGURE);
    }

    private PerformanceImprovementPlan createImprovementPlan(ProbationAssessment assessment,
                                                             ProbationAssessmentDimension dimension,
                                                             String areasForImprovement) {
        try {
            PerformanceImprovementPlan plan = new PerformanceImprovementPlan();
            plan.setClientId(assessment.getClientId());
            plan.setEmployee(assessment.getEmployee());
            plan.setReportingPeriod(reportingPeriodService.getActiveReportingPeriod());
            plan.setTargetArea(dimension.getDimensionTemplate() == null ? "Personal Dimension" : dimension.getDimensionTemplate().getTitle());
            plan.setConcern(areasForImprovement);
            plan.setAgreedAction(areasForImprovement);
            plan.setExpectedStandard("");
            plan.setRequiredSupport("");
            plan.setReviewNotes("");
            plan.setStatus("todo");
            plan.setProgress(0.0);
            return performanceImprovementPlanService.savePerformanceImprovementPlan(plan);
        } catch (Exception exception) {
            return null;
        }
    }

    private Path resolveAttachmentDirectory() {
        return Paths.get(ATTACHMENT_DIR).toAbsolutePath().normalize();
    }

    private String storeAttachmentFile(MultipartFile file) throws IOException {
        String safeSourceName = sanitizeFileName(file.getOriginalFilename());
        String extension = commonService.getFileExtention(safeSourceName);
        String generated = UUID.randomUUID().toString();
        if (StringUtils.hasText(extension)) {
            generated = generated + "." + extension.toLowerCase(Locale.ENGLISH);
        }

        Path directory = resolveAttachmentDirectory();
        Files.createDirectories(directory);
        Path destination = directory.resolve(generated).normalize();
        if (!destination.startsWith(directory)) {
            throw new IllegalArgumentException("Invalid attachment path.");
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return generated;
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("Attachment name is required.");
        }
        String cleaned = fileName.trim().replace("\\", "/");
        String simpleName = cleaned.substring(cleaned.lastIndexOf('/') + 1);
        if (!StringUtils.hasText(simpleName)) {
            throw new IllegalArgumentException("Invalid attachment name.");
        }
        simpleName = simpleName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (".".equals(simpleName) || "..".equals(simpleName)) {
            throw new IllegalArgumentException("Invalid attachment name.");
        }
        return simpleName;
    }

    private String sanitizeValue(String value) {
        return value == null ? "" : value.trim();
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

    private void notifySupervisorKpiSubmission(HttpServletRequest request, ProbationAssessment assessment) {
        if (assessment == null || assessment.getEmployee() == null || assessment.getEmployee().getSupervisor() == null) {
            return;
        }
        Account supervisor = assessment.getEmployee().getSupervisor();
        if (!hasText(supervisor.getEmail())) {
            return;
        }
        String link = buildAssessmentLink(request, assessment.getId());
        String subject = "Probation KPI Contract Pending Supervisor Approval";
        String message = "The probation KPI contract for " + assessment.getEmployee().getFullName() + " has been submitted for your review.\n"
                + "Performance period: " + resolvePerformancePeriod(assessment) + "\n"
                + "Start date: " + defaultText(assessment.getStartDate(), "N/A") + "\n"
                + "End date: " + defaultText(assessment.getEndDate(), "N/A") + "\n"
                + "Link: " + link;
        notificationService.sendUserMessageAsync(supervisor.getEmail().trim(), supervisor.getFullName(), subject, message);
    }

    private void notifyHrKpiSubmission(HttpServletRequest request, ProbationAssessment assessment) {
        if (assessment == null || assessment.getEmployee() == null) {
            return;
        }
        String subject = "Probation KPI Contract Pending HR Approval";
        String link = buildAssessmentLink(request, assessment.getId());
        String message = "The probation KPI contract for " + assessment.getEmployee().getFullName() + " was approved by supervisor and now requires HR approval.\n"
                + "Performance period: " + resolvePerformancePeriod(assessment) + "\n"
                + "Start date: " + defaultText(assessment.getStartDate(), "N/A") + "\n"
                + "End date: " + defaultText(assessment.getEndDate(), "N/A") + "\n"
                + "Link: " + link + "\n"
                + buildKpiReviewSummary(assessment);

        String hrEmail = commonService.getHREmail();
        if (hasText(hrEmail)) {
            notificationService.sendUserMessageAsync(hrEmail.trim(), "HR", subject, message);
        } else {
            PortletUtils.addErrorMsg("HR notification was not sent because email.hr is not configured in System Settings.", request);
        }
    }

    private void notifyIncumbentHrPendingInformative(HttpServletRequest request, ProbationAssessment assessment) {
        if (assessment == null || assessment.getEmployee() == null || !hasText(assessment.getEmployee().getEmail())) {
            return;
        }
        String subject = "Probation KPI Contract Sent to HR";
        String link = buildAssessmentLink(request, assessment.getId());
        String message = "Your probation KPI contract was approved by your supervisor and has been sent to HR for final KPI-contract approval.\n"
                + "Performance period: " + resolvePerformancePeriod(assessment) + "\n"
                + "This is an informational update.\n"
                + "Link: " + link;
        notificationService.sendUserMessageAsync(
                assessment.getEmployee().getEmail().trim(),
                assessment.getEmployee().getFullName(),
                subject,
                message
        );
    }

    private void notifyIncumbentAndSupervisorKpiDecision(HttpServletRequest request,
                                                         ProbationAssessment assessment,
                                                         String decision,
                                                         String remarks,
                                                         boolean supervisorActionRequired) {
        if (assessment == null || assessment.getEmployee() == null) {
            return;
        }
        String subject = "Probation KPI Contract " + decision;
        String link = buildAssessmentLink(request, assessment.getId());
        String summary = buildKpiReviewSummary(assessment);
        StringBuilder message = new StringBuilder();
        message.append("Your probation KPI contract has been ").append(decision.toLowerCase()).append(".\n")
                .append("Performance period: ").append(resolvePerformancePeriod(assessment)).append("\n")
                .append("Link: ").append(link).append("\n");
        if (hasText(remarks)) {
            message.append("Decision reason: ").append(remarks.trim()).append("\n");
        }
        if (supervisorActionRequired) {
            message.append("Action owner: Supervisor (may return this contract to incumbent for updates).\n");
        }
        if (hasText(summary)) {
            message.append(summary);
        }
        String incumbentMessage = message.toString();
        if (!supervisorActionRequired) {
            incumbentMessage = incumbentMessage + "This email is for your information.\n";
        }

        if (hasText(assessment.getEmployee().getEmail())) {
            notificationService.sendUserMessageAsync(assessment.getEmployee().getEmail().trim(),
                    assessment.getEmployee().getFullName(), subject, incumbentMessage);
        }

        Account supervisor = assessment.getEmployee().getSupervisor();
        if (supervisor != null && hasText(supervisor.getEmail())) {
            String supervisorMessage = "The probation KPI contract for " + assessment.getEmployee().getFullName()
                    + " has been " + decision.toLowerCase() + ".\n"
                    + "Performance period: " + resolvePerformancePeriod(assessment) + "\n"
                    + "Link: " + link + "\n"
                    + (hasText(remarks) ? "Decision reason: " + remarks.trim() + "\n" : "");
            if (supervisorActionRequired) {
                supervisorMessage = "The probation KPI contract for " + assessment.getEmployee().getFullName()
                        + " was rejected by HR and requires your action.\n"
                        + "You may either update and re-approve, or return to incumbent for changes.\n"
                        + "Performance period: " + resolvePerformancePeriod(assessment) + "\n"
                        + "Link: " + link + "\n"
                        + (hasText(remarks) ? "HR reason: " + remarks.trim() + "\n" : "")
                        + (hasText(summary) ? summary : "");
            } else if (hasText(summary)) {
                supervisorMessage = supervisorMessage + summary;
            }
            notificationService.sendUserMessageAsync(supervisor.getEmail().trim(),
                    supervisor.getFullName(), subject, supervisorMessage);
        }
    }

    private String buildKpiReviewSummary(ProbationAssessment assessment) {
        List<ProbationKpi> kpis = probationAssessmentService.listKpis(assessment.getId());
        int commentCount = 0;
        List<String> flagged = new java.util.ArrayList<>();
        for (ProbationKpi kpi : kpis) {
            commentCount += probationAssessmentService.listKpiComments(kpi.getId()).size();
            if (hasText(kpi.getFlag())) {
                String name = defaultText(kpi.getName(), "KPI");
                flagged.add(name + ": " + kpi.getFlag().trim());
            }
        }
        if (commentCount == 0 && flagged.isEmpty()) {
            return "";
        }
        StringBuilder summary = new StringBuilder("Review notes summary:\n");
        if (commentCount > 0) {
            summary.append("Reviewer comments added: ").append(commentCount).append("\n");
        }
        if (!flagged.isEmpty()) {
            summary.append("Flagged KPI items:\n");
            for (String item : flagged) {
                summary.append("- ").append(item).append("\n");
            }
        }
        return summary.toString();
    }

    private String buildAssessmentLink(HttpServletRequest request, long assessmentId) {
        try {
            String host = commonService.getCurrentUrl(request);
            if (hasText(host)) {
                return host + "/probation-assessments/add-kpis/" + assessmentId;
            }
        } catch (Exception ignored) {
            // fallback below
        }
        return "/probation-assessments/add-kpis/" + assessmentId;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String resolvePerformancePeriod(ProbationAssessment assessment) {
        if (assessment == null) {
            return "N/A";
        }
        if (hasText(assessment.getPerformancePeriod())) {
            return assessment.getPerformancePeriod().trim();
        }
        boolean hasStart = hasText(assessment.getStartDate());
        boolean hasEnd = hasText(assessment.getEndDate());
        if (hasStart && hasEnd) {
            return assessment.getStartDate().trim() + " - " + assessment.getEndDate().trim();
        }
        if (hasStart) {
            return assessment.getStartDate().trim();
        }
        if (hasEnd) {
            return assessment.getEndDate().trim();
        }
        return "N/A";
    }

    private double parseAndValidateMark(String rawValue, ProbationKpi kpi, String label) {
        String kpiLabel = (kpi != null && hasText(kpi.getName())) ? kpi.getName().trim() : "ID " + (kpi == null ? "N/A" : kpi.getId());
        if (!hasText(rawValue)) {
            throw new IllegalArgumentException(label + " is required for KPI " + kpiLabel + ".");
        }

        final double numericValue;
        try {
            numericValue = Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be numeric for KPI " + kpiLabel + ".");
        }

        if (numericValue < MIN_MARK || numericValue > MAX_MARK) {
            throw new IllegalArgumentException(label + " must be between 1 and 5 inclusive for KPI " + kpiLabel + ".");
        }
        return numericValue;
    }

    private void ensureMarkRange(double mark, ProbationKpi kpi, String label) {
        String kpiLabel = (kpi != null && hasText(kpi.getName())) ? kpi.getName().trim() : "ID " + (kpi == null ? "N/A" : kpi.getId());
        if (mark < MIN_MARK || mark > MAX_MARK) {
            throw new IllegalArgumentException(label + " must be between 1 and 5 inclusive for KPI " + kpiLabel + ".");
        }
    }

    private double parseAndValidateProgressPercent(String rawValue, ProbationKpi kpi) {
        String kpiLabel = (kpi != null && hasText(kpi.getName())) ? kpi.getName().trim() : "ID " + (kpi == null ? "N/A" : kpi.getId());
        final double numericValue;
        try {
            numericValue = Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Progress percent must be numeric for KPI " + kpiLabel + ".");
        }
        if (numericValue < 0.0 || numericValue > 100.0) {
            throw new IllegalArgumentException("Progress percent must be between 0 and 100 for KPI " + kpiLabel + ".");
        }
        return numericValue;
    }

    private void ensureProgressRange(double progress, ProbationKpi kpi) {
        String kpiLabel = (kpi != null && hasText(kpi.getName())) ? kpi.getName().trim() : "ID " + (kpi == null ? "N/A" : kpi.getId());
        if (progress < 0.0 || progress > 100.0) {
            throw new IllegalArgumentException("Progress percent must be between 0 and 100 for KPI " + kpiLabel + ".");
        }
    }

    private String findKpiContractIssue(List<ProbationKpi> kpis) {
        if (kpis == null || kpis.isEmpty()) {
            return "Add at least one KPI before submitting the contract.";
        }
        for (ProbationKpi kpi : kpis) {
            String label = defaultText(kpi.getName(), "KPI ID " + kpi.getId());
            if (!hasText(kpi.getName()) || !hasText(kpi.getMeasureOfSuccess()) || !hasText(kpi.getTarget())) {
                return "Complete the KPI name, measure of success, and target for " + label + " before submission.";
            }
        }
        return null;
    }

    private String findDimensionIssue(List<ProbationAssessmentDimension> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return "At least one personal dimension must be configured and completed.";
        }
        for (ProbationAssessmentDimension dimension : dimensions) {
            if (!hasText(dimension.getStrengths()) && !hasText(dimension.getAreasForImprovement())) {
                String label = dimension.getDimensionTemplate() == null
                        ? "a personal dimension"
                        : defaultText(dimension.getDimensionTemplate().getTitle(), "a personal dimension");
                return "Capture strengths or areas for improvement for " + label + " before submission.";
            }
        }
        return null;
    }
}
