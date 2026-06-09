package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.*;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.constants.AccessPermissions;
import hr.performancemanagement.utils.dto.ProbationResultSummary;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;


@Service
public class ProbationAssessmentServiceImpl implements hr.performancemanagement.service.api.ProbationAssessmentService {

    @Autowired
    private ProbationAssessmentRepository assessmentRepository;
    @Autowired
    private ProbationAssessmentDimensionRepository assessmentDimensionRepository;
    @Autowired
    private ProbationKpiRepository probationKpiRepository;
    @Autowired
    private ProbationKpiCommentRepository probationKpiCommentRepository;
    @Autowired
    private ProbationAssessmentApprovalRepository approvalRepository;
    @Autowired
    private ProbationConfigService probationConfigService;
    @Autowired
    private CommonService commonService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private PerformanceImprovementPlanRepository performanceImprovementPlanRepository;
    @Autowired
    private AccessControlService accessControlService;

    @Override
    public List<ProbationAssessment> listVisibleAssessments() {
        Account loggedUser = commonService.getLoggedUser();
        List<ProbationAssessment> allAssessments = assessmentRepository.findProbationAssessmentsByClientIdOrderByDateDesc(loggedUser.getClientId());
        if (commonService.isAdmin() || commonService.hasSpecialRights()) {
            return allAssessments;
        }

        List<ProbationAssessment> visibleAssessments = new ArrayList<>();
        for (ProbationAssessment assessment : allAssessments) {
            if (isOwner(assessment, loggedUser) || isSupervisor(assessment, loggedUser)
                    || canViewAsHr(assessment, loggedUser) || canUserApprove(assessment, loggedUser)) {
                visibleAssessments.add(assessment);
            }
        }
        return visibleAssessments;
    }

    @Override
    public ProbationAssessment getAssessmentById(long id) {
        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser == null) {
            return null;
        }
        ProbationAssessment assessment = assessmentRepository.findProbationAssessmentByIdAndClientId(id, loggedUser.getClientId());
        if (assessment == null) {
            return null;
        }
        if (isOwner(assessment, loggedUser) || isSupervisor(assessment, loggedUser)
                || canViewAsHr(assessment, loggedUser) || canUserApprove(assessment, loggedUser)) {
            return assessment;
        }
        return null;
    }

    @Override
    public ProbationAssessment createAssessment(ProbationAssessment assessment) {
        Account loggedUser = commonService.getLoggedUser();
        if (assessment == null || assessment.getEmployee() == null) {
            return null;
        }
        Account selectedEmployee = accountService.getAccountById(assessment.getEmployee().getId());
        if (!hasPermission(loggedUser, AccessPermissions.PROBATION_CREATE_CONTRACT, selectedEmployee)) {
            return null;
        }
        if (!isSameClientAccount(selectedEmployee, loggedUser.getClientId())
                || selectedEmployee.getSupervisor() == null
                || !isSameClientAccount(selectedEmployee.getSupervisor(), loggedUser.getClientId())
                || !PMConstants.STATUS_ACTIVE.equalsIgnoreCase(selectedEmployee.getStatus())
                || !PMConstants.STATUS_ACTIVE.equalsIgnoreCase(selectedEmployee.getSupervisor().getStatus())
                || !isValidDateRange(assessment.getStartDate(), assessment.getEndDate())) {
            return null;
        }
        List<ProbationDimensionTemplate> dimensionTemplates = probationConfigService.listActiveDimensionTemplates();
        if (dimensionTemplates.isEmpty()
                || hasOverlappingOpenAssessment(selectedEmployee, assessment.getStartDate(), assessment.getEndDate())) {
            return null;
        }
        assessment.setEmployee(selectedEmployee);
        assessment.setClientId(loggedUser.getClientId());
        assessment.setStartDate(assessment.getStartDate().trim());
        assessment.setEndDate(assessment.getEndDate().trim());
        assessment.setPerformancePeriod(assessment.getStartDate() + " - " + assessment.getEndDate());
        assessment.setStatus(PMConstants.PROBATION_STATUS_CONTRACT_CREATED);
        assessment.setCurrentStepOrder(null);
        assessment.setCurrentStepName(null);
        ProbationAssessment savedAssessment = assessmentRepository.save(assessment);
        initializeDimensions(savedAssessment, dimensionTemplates);
        return savedAssessment;
    }

    @Override
    public ProbationAssessment updateAssessmentCore(ProbationAssessment updatedAssessment) {
        ProbationAssessment existingAssessment = getAssessmentById(updatedAssessment.getId());
        Account loggedUser = commonService.getLoggedUser();
        if (existingAssessment == null) {
            return null;
        }

        if (isOwner(existingAssessment, loggedUser) && isKpiContractEditable(existingAssessment)) {
            if (!isValidDateRange(updatedAssessment.getStartDate(), updatedAssessment.getEndDate())) {
                return null;
            }
            existingAssessment.setStartDate(updatedAssessment.getStartDate().trim());
            existingAssessment.setEndDate(updatedAssessment.getEndDate().trim());
            existingAssessment.setPerformancePeriod(existingAssessment.getStartDate() + " - " + existingAssessment.getEndDate());
            existingAssessment.setGeneralObservations(updatedAssessment.getGeneralObservations());
            existingAssessment.setEmployeeComment(updatedAssessment.getEmployeeComment());
        }
        if (isSupervisor(existingAssessment, loggedUser) && canUserEditDimensions(existingAssessment, loggedUser)) {
            existingAssessment.setGeneralObservations(updatedAssessment.getGeneralObservations());
            existingAssessment.setSupervisorComment(updatedAssessment.getSupervisorComment());
        }
        if (!(isOwner(existingAssessment, loggedUser) && isKpiContractEditable(existingAssessment))
                && !(isSupervisor(existingAssessment, loggedUser) && canUserEditDimensions(existingAssessment, loggedUser))) {
            return null;
        }
        return assessmentRepository.save(existingAssessment);
    }

    @Override
    public ProbationAssessment updateAssessmentStatus(long assessmentId, String status) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        if (assessment == null) {
            return null;
        }
        Account loggedUser = commonService.getLoggedUser();
        if (!isAllowedStatusTransition(assessment, status, loggedUser)) {
            return null;
        }
        assessment.setStatus(status);
        return assessmentRepository.save(assessment);
    }

    @Override
    public List<ProbationAssessmentDimension> listAssessmentDimensions(long assessmentId) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        if (assessment == null) {
            return Collections.emptyList();
        }
        return assessmentDimensionRepository.findProbationAssessmentDimensionsByAssessmentOrderByDimensionTemplate_DisplayOrderAsc(assessment);
    }

    @Override
    public ProbationAssessmentDimension saveDimensionResponse(long assessmentId,
                                                              long dimensionTemplateId,
                                                              String strengths,
                                                              String areasForImprovement,
                                                              Long performanceImprovementPlanId) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        ProbationDimensionTemplate template = probationConfigService.getDimensionTemplateById(dimensionTemplateId);
        Account loggedUser = commonService.getLoggedUser();
        if (assessment == null || template == null || !canUserEditDimensions(assessment, loggedUser)) {
            return null;
        }
        ProbationAssessmentDimension dimension = assessmentDimensionRepository.findProbationAssessmentDimensionByAssessmentAndDimensionTemplate(assessment, template);
        if (dimension == null) {
            dimension = new ProbationAssessmentDimension();
            dimension.setAssessment(assessment);
            dimension.setDimensionTemplate(template);
        }
        dimension.setStrengths(strengths);
        dimension.setAreasForImprovement(areasForImprovement);
        if (performanceImprovementPlanId != null && performanceImprovementPlanId > 0) {
            PerformanceImprovementPlan plan = performanceImprovementPlanRepository.findPerformanceImprovementPlanById(performanceImprovementPlanId);
            if (plan != null && plan.getClientId() == loggedUser.getClientId()) {
                dimension.setPerformanceImprovementPlan(plan);
            }
        } else {
            dimension.setPerformanceImprovementPlan(null);
        }
        return assessmentDimensionRepository.save(dimension);
    }

    @Override
    public ProbationAssessmentDimension getAssessmentDimensionById(long dimensionId) {
        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser == null) {
            return null;
        }
        return assessmentDimensionRepository.findProbationAssessmentDimensionByIdAndAssessment_ClientId(dimensionId, loggedUser.getClientId());
    }

    @Override
    public List<ProbationKpi> listKpis(long assessmentId) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        if (assessment == null) {
            return Collections.emptyList();
        }
        return probationKpiRepository.findProbationKpisByAssessmentOrderByIdAsc(assessment);
    }

    @Override
    public ProbationKpi getKpiById(long kpiId) {
        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser == null) {
            return null;
        }
        return probationKpiRepository.findProbationKpiByIdAndAssessment_ClientId(kpiId, loggedUser.getClientId());
    }

    @Override
    public ProbationKpi addKpi(long assessmentId, ProbationKpi kpi) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        Account loggedUser = commonService.getLoggedUser();
        if (assessment == null || kpi == null || !isOwner(assessment, loggedUser) || !isKpiContractEditable(assessment)) {
            return null;
        }

        ProbationKpi targetKpi = kpi;
        if (kpi.getId() > 0) {
            targetKpi = getKpiById(kpi.getId());
            if (targetKpi == null || targetKpi.getAssessment() == null || targetKpi.getAssessment().getId() != assessmentId) {
                return null;
            }
            targetKpi.setName(kpi.getName());
            targetKpi.setMeasureOfSuccess(kpi.getMeasureOfSuccess());
            targetKpi.setTarget(kpi.getTarget());
        } else {
            targetKpi.setAssessment(assessment);
            targetKpi.setIncumbentMark(null);
            targetKpi.setSupervisorMark(null);
            targetKpi.setProgressPercent(null);
            targetKpi.setProgressComment(null);
            targetKpi.setIncumbentComment(null);
            targetKpi.setSupervisorComment(null);
            targetKpi.setAttachmentPath(null);
            targetKpi.setFlag(null);
        }
        targetKpi.setStatus(PMConstants.STATUS_ACTIVE);
        return probationKpiRepository.save(targetKpi);
    }

    @Override
    public ProbationKpi updateKpi(ProbationKpi newKpi) {
        if (newKpi == null) {
            return null;
        }
        ProbationKpi existingKpi = getKpiById(newKpi.getId());
        if (existingKpi == null || existingKpi.getAssessment() == null) {
            return null;
        }

        ProbationAssessment assessment = existingKpi.getAssessment();
        Account loggedUser = commonService.getLoggedUser();
        if (isOwner(assessment, loggedUser) && isKpiContractEditable(assessment)) {
            existingKpi.setName(newKpi.getName());
            existingKpi.setMeasureOfSuccess(newKpi.getMeasureOfSuccess());
            existingKpi.setTarget(newKpi.getTarget());
        } else if (isOwner(assessment, loggedUser) && isIncumbentEvaluationEditable(assessment)) {
            if (!isValidMark(newKpi.getIncumbentMark()) || !isValidProgress(newKpi.getProgressPercent())) {
                return null;
            }
            existingKpi.setIncumbentMark(newKpi.getIncumbentMark());
            existingKpi.setProgressPercent(newKpi.getProgressPercent());
            existingKpi.setProgressComment(newKpi.getProgressComment());
            existingKpi.setIncumbentComment(newKpi.getIncumbentComment());
            existingKpi.setAttachmentPath(newKpi.getAttachmentPath());
        } else if (isSupervisor(assessment, loggedUser) && isSupervisorEvaluationEditable(assessment)) {
            if (!isValidMark(newKpi.getSupervisorMark())) {
                return null;
            }
            existingKpi.setSupervisorMark(newKpi.getSupervisorMark());
            existingKpi.setSupervisorComment(newKpi.getSupervisorComment());
        } else {
            return null;
        }
        return probationKpiRepository.save(existingKpi);
    }

    @Override
    public ProbationKpi saveKpiFlag(long kpiId, String flagReason) {
        ProbationKpi kpi = probationKpiRepository.findProbationKpiById(kpiId);
        if (kpi == null || !isAssessmentAccessible(kpi.getAssessment())) {
            return null;
        }
        Account loggedUser = commonService.getLoggedUser();
        if (!canUserAnnotateKpiContract(kpi.getAssessment(), loggedUser)) {
            return null;
        }
        String normalizedFlag = flagReason == null ? null : flagReason.trim();
        kpi.setFlag((normalizedFlag == null || normalizedFlag.isEmpty()) ? null : normalizedFlag);
        return probationKpiRepository.save(kpi);
    }

    @Override
    public List<ProbationKpiComment> listKpiComments(long kpiId) {
        ProbationKpi kpi = probationKpiRepository.findProbationKpiById(kpiId);
        if (kpi == null || !isAssessmentAccessible(kpi.getAssessment())) {
            return Collections.emptyList();
        }
        return probationKpiCommentRepository.findProbationKpiCommentsByProbationKpiOrderByDateAsc(kpi);
    }

    @Override
    public ProbationKpiComment saveKpiComment(long kpiId, String message) {
        ProbationKpi kpi = probationKpiRepository.findProbationKpiById(kpiId);
        if (kpi == null || !isAssessmentAccessible(kpi.getAssessment())) {
            return null;
        }
        Account loggedUser = commonService.getLoggedUser();
        if (!canUserAnnotateKpiContract(kpi.getAssessment(), loggedUser)) {
            return null;
        }
        if (message == null || message.trim().isEmpty()) {
            return null;
        }
        ProbationKpiComment comment = new ProbationKpiComment();
        comment.setProbationKpi(kpi);
        comment.setSender(loggedUser);
        comment.setMessage(message.trim());
        return probationKpiCommentRepository.save(comment);
    }

    @Override
    public boolean deleteKpi(long kpiId) {
        ProbationKpi kpi = getKpiById(kpiId);
        if (kpi == null || !isOwner(kpi.getAssessment(), commonService.getLoggedUser()) || !isKpiContractEditable(kpi.getAssessment())) {
            return false;
        }
        if (!probationKpiCommentRepository.findProbationKpiCommentsByProbationKpiOrderByDateAsc(kpi).isEmpty()) {
            return false;
        }
        probationKpiRepository.delete(kpi);
        return true;
    }

    @Override
    public List<ProbationAssessmentApproval> listApprovalHistory(long assessmentId) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        if (assessment == null) {
            return Collections.emptyList();
        }
        return approvalRepository.findProbationAssessmentApprovalsByAssessmentOrderByDateDesc(assessment);
    }

    @Override
    public ProbationAssessmentApproval recordReviewAction(long assessmentId, String stepName, String action, String remarks) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        Account loggedUser = commonService.getLoggedUser();
        if (assessment == null || loggedUser == null
                || (!isOwner(assessment, loggedUser) && !isSupervisor(assessment, loggedUser) && !canViewAsHr(assessment, loggedUser))) {
            return null;
        }
        ProbationAssessmentApproval approval = new ProbationAssessmentApproval();
        approval.setAssessment(assessment);
        approval.setWorkflowStepName(stepName);
        approval.setAction(action);
        approval.setActor(loggedUser);
        approval.setRemarks(remarks == null ? null : remarks.trim());
        return approvalRepository.save(approval);
    }

    @Override
    public boolean submitAssessment(long assessmentId, String remarks) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        if (!canLoggedUserSubmit(assessment)) {
            return false;
        }
        List<ProbationWorkflowStep> steps = probationConfigService.listActiveWorkflowSteps();
        if (steps.isEmpty()) {
            return false;
        }
        steps.sort(Comparator.comparing(ProbationWorkflowStep::getStepOrder));
        ProbationWorkflowStep firstStep = steps.get(0);
        assessment.setStatus(PMConstants.PROBATION_STATUS_PENDING);
        assessment.setCurrentStepOrder(firstStep.getStepOrder());
        assessment.setCurrentStepName(firstStep.getName());
        assessmentRepository.save(assessment);
        createApprovalAudit(assessment, firstStep.getName(), firstStep.getStepOrder(), PMConstants.PROBATION_ACTION_SUBMITTED, remarks);
        return true;
    }

    @Override
    public boolean approveAssessment(long assessmentId, String remarks) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        Account loggedUser = commonService.getLoggedUser();
        if (assessment == null) {
            return false;
        }
        ProbationWorkflowStep currentStep = getCurrentWorkflowStep(assessment);
        if (currentStep == null || !canUserApprove(assessment, loggedUser)) {
            return false;
        }
        createApprovalAudit(assessment, currentStep.getName(), currentStep.getStepOrder(), PMConstants.PROBATION_ACTION_APPROVED, remarks);

        ProbationWorkflowStep nextStep = getNextWorkflowStep(currentStep.getStepOrder());
        if (nextStep == null) {
            assessment.setStatus(PMConstants.PROBATION_STATUS_AUTHORIZED);
            assessment.setCurrentStepOrder(null);
            assessment.setCurrentStepName(null);
        } else {
            assessment.setStatus(PMConstants.PROBATION_STATUS_PENDING);
            assessment.setCurrentStepOrder(nextStep.getStepOrder());
            assessment.setCurrentStepName(nextStep.getName());
        }
        assessmentRepository.save(assessment);
        return true;
    }

    @Override
    public boolean rejectAssessment(long assessmentId, String remarks) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        Account loggedUser = commonService.getLoggedUser();
        if (assessment == null) {
            return false;
        }
        ProbationWorkflowStep currentStep = getCurrentWorkflowStep(assessment);
        if (currentStep == null || !canUserApprove(assessment, loggedUser)) {
            return false;
        }
        createApprovalAudit(assessment, currentStep.getName(), currentStep.getStepOrder(), PMConstants.PROBATION_ACTION_REJECTED, remarks);
        assessment.setStatus(PMConstants.PROBATION_STATUS_REJECTED);
        assessment.setCurrentStepOrder(null);
        assessment.setCurrentStepName(null);
        assessmentRepository.save(assessment);
        return true;
    }

    @Override
    public boolean canLoggedUserSubmit(ProbationAssessment assessment) {
        Account loggedUser = commonService.getLoggedUser();
        if (assessment == null) {
            return false;
        }
        return isOwner(assessment, loggedUser) &&
                (PMConstants.PROBATION_STATUS_DRAFT.equalsIgnoreCase(assessment.getStatus()) || PMConstants.PROBATION_STATUS_REJECTED.equalsIgnoreCase(assessment.getStatus()));
    }

    @Override
    public boolean canLoggedUserApprove(ProbationAssessment assessment) {
        if (assessment == null) {
            return false;
        }
        return canUserApprove(assessment, commonService.getLoggedUser());
    }

    @Override
    public boolean isLoggedUserOwner(ProbationAssessment assessment) {
        if (assessment == null) {
            return false;
        }
        return isOwner(assessment, commonService.getLoggedUser());
    }

    @Override
    public boolean isLoggedUserSupervisor(ProbationAssessment assessment) {
        if (assessment == null) {
            return false;
        }
        return isSupervisor(assessment, commonService.getLoggedUser());
    }

    @Override
    public ProbationResultSummary getResultSummary(long assessmentId) {
        List<ProbationKpi> kpis = listKpis(assessmentId);
        int incumbentCount = 0;
        int supervisorCount = 0;
        int progressCount = 0;
        int flaggedCount = 0;
        double incumbentTotal = 0.0;
        double supervisorTotal = 0.0;
        double progressTotal = 0.0;

        for (ProbationKpi kpi : kpis) {
            if (kpi.getIncumbentMark() != null) {
                incumbentCount++;
                incumbentTotal += kpi.getIncumbentMark();
            }
            if (kpi.getSupervisorMark() != null) {
                supervisorCount++;
                supervisorTotal += kpi.getSupervisorMark();
            }
            if (kpi.getProgressPercent() != null) {
                progressCount++;
                progressTotal += kpi.getProgressPercent();
            }
            if (hasText(kpi.getFlag())) {
                flaggedCount++;
            }
        }

        int totalKpis = kpis.size();
        double averageIncumbentMark = average(incumbentTotal, incumbentCount);
        double averageSupervisorMark = average(supervisorTotal, supervisorCount);
        double averageProgress = average(progressTotal, progressCount);
        boolean complete = totalKpis > 0 && incumbentCount == totalKpis
                && supervisorCount == totalKpis && progressCount == totalKpis;
        double resultMark = supervisorCount > 0 ? averageSupervisorMark : averageIncumbentMark;
        String resultSource = supervisorCount > 0 ? "Supervisor marks" : "Incumbent marks";

        return new ProbationResultSummary(
                totalKpis,
                incumbentCount,
                supervisorCount,
                progressCount,
                flaggedCount,
                averageIncumbentMark,
                averageSupervisorMark,
                averageProgress,
                resultMark,
                resultSource,
                complete,
                resolvePerformanceBand(resultMark),
                resolveRecommendation(totalKpis, incumbentCount, supervisorCount, progressCount,
                        flaggedCount, resultMark, averageProgress)
        );
    }

    private boolean canUserApprove(ProbationAssessment assessment, Account user) {
        if (assessment == null || user == null) {
            return false;
        }
        if (!PMConstants.PROBATION_STATUS_PENDING.equalsIgnoreCase(assessment.getStatus())) {
            return false;
        }
        ProbationWorkflowStep currentStep = getCurrentWorkflowStep(assessment);
        return currentStep != null && isApproverMatch(currentStep, user, assessment);
    }

    private ProbationWorkflowStep getCurrentWorkflowStep(ProbationAssessment assessment) {
        if (assessment.getCurrentStepOrder() == null) {
            return null;
        }
        List<ProbationWorkflowStep> steps = probationConfigService.listActiveWorkflowSteps();
        for (ProbationWorkflowStep step : steps) {
            if (assessment.getCurrentStepOrder().equals(step.getStepOrder())) {
                return step;
            }
        }
        return null;
    }

    private ProbationWorkflowStep getNextWorkflowStep(Integer currentStepOrder) {
        List<ProbationWorkflowStep> steps = probationConfigService.listActiveWorkflowSteps();
        steps.sort(Comparator.comparing(ProbationWorkflowStep::getStepOrder));
        for (ProbationWorkflowStep step : steps) {
            if (step.getStepOrder() > currentStepOrder) {
                return step;
            }
        }
        return null;
    }

    private boolean isApproverMatch(ProbationWorkflowStep step, Account user, ProbationAssessment assessment) {
        String approverMode = step.getApproverMode();
        if (approverMode == null) {
            return false;
        }

        if (PMConstants.PROBATION_APPROVER_MODE_SUPERVISOR.equalsIgnoreCase(approverMode)) {
            Account supervisor = assessment.getEmployee() == null ? null : assessment.getEmployee().getSupervisor();
            return supervisor != null && supervisor.getId() == user.getId();
        }
        if (PMConstants.PROBATION_APPROVER_MODE_ACCOUNT_TYPE.equalsIgnoreCase(approverMode)) {
            boolean accountTypeMatch = step.getApproverAccountType() != null && step.getApproverAccountType().equalsIgnoreCase(user.getAccountType());
            if (!accountTypeMatch) {
                return false;
            }
            if (Boolean.TRUE.equals(step.getSameDivisionOnly()) && assessment.getEmployee() != null && assessment.getEmployee().getDivision() != null && user.getDivision() != null) {
                return assessment.getEmployee().getDivision().getId() == user.getDivision().getId();
            }
            return true;
        }
        if (PMConstants.PROBATION_APPROVER_MODE_ROLE.equalsIgnoreCase(approverMode)) {
            return step.getApproverRole() != null && step.getApproverRole().equalsIgnoreCase(user.getRole());
        }
        if (PMConstants.PROBATION_APPROVER_MODE_USER.equalsIgnoreCase(approverMode)) {
            return step.getApproverAccount() != null && step.getApproverAccount().getId() == user.getId();
        }

        return false;
    }

    private void initializeDimensions(ProbationAssessment assessment, List<ProbationDimensionTemplate> templates) {
        for (ProbationDimensionTemplate template : templates) {
            ProbationAssessmentDimension dimension = new ProbationAssessmentDimension();
            dimension.setAssessment(assessment);
            dimension.setDimensionTemplate(template);
            dimension.setStrengths("");
            dimension.setAreasForImprovement("");
            assessmentDimensionRepository.save(dimension);
        }
    }

    private void createApprovalAudit(ProbationAssessment assessment, String stepName, Integer stepOrder, String action, String remarks) {
        ProbationAssessmentApproval approval = new ProbationAssessmentApproval();
        approval.setAssessment(assessment);
        approval.setWorkflowStepName(stepName);
        approval.setStepOrder(stepOrder);
        approval.setAction(action);
        approval.setActor(commonService.getLoggedUser());
        approval.setRemarks(remarks);
        approvalRepository.save(approval);
    }

    private boolean isOwner(ProbationAssessment assessment, Account user) {
        return assessment != null && user != null && assessment.getEmployee() != null && assessment.getEmployee().getId() == user.getId();
    }

    private boolean isSupervisor(ProbationAssessment assessment, Account user) {
        return assessment != null &&
                user != null &&
                assessment.getEmployee() != null &&
                assessment.getEmployee().getSupervisor() != null &&
                assessment.getEmployee().getSupervisor().getId() == user.getId();
    }

    private boolean canUserEditDimensions(ProbationAssessment assessment, Account user) {
        return isSupervisor(assessment, user)
                && (statusMatches(assessment, PMConstants.PROBATION_STATUS_PERSONAL_DIMENSIONS_IN_PROGRESS)
                || statusMatches(assessment, PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_HR));
    }

    private boolean isAssessmentAccessible(ProbationAssessment assessment) {
        Account loggedUser = commonService.getLoggedUser();
        return assessment != null && assessment.getClientId() == loggedUser.getClientId();
    }

    private boolean isSameClientAccount(Account account, long clientId) {
        return account != null && account.getClientId() == clientId;
    }

    private boolean canUserAnnotateKpiContract(ProbationAssessment assessment, Account user) {
        if (assessment == null || user == null) {
            return false;
        }
        if (PMConstants.PROBATION_STATUS_KPI_PENDING_SUPERVISOR_APPROVAL.equalsIgnoreCase(assessment.getStatus())) {
            return isSupervisor(assessment, user);
        }
        if (PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_HR.equalsIgnoreCase(assessment.getStatus())) {
            return isSupervisor(assessment, user);
        }
        if (PMConstants.PROBATION_STATUS_KPI_PENDING_HR_APPROVAL.equalsIgnoreCase(assessment.getStatus())) {
            return hasPermission(user, AccessPermissions.PROBATION_APPROVE_KPI_CONTRACT, assessment.getEmployee());
        }
        return false;
    }

    private boolean canViewAsHr(ProbationAssessment assessment, Account user) {
        Account subject = assessment == null ? null : assessment.getEmployee();
        return hasPermission(user, AccessPermissions.PROBATION_CREATE_CONTRACT, subject)
                || hasPermission(user, AccessPermissions.PROBATION_APPROVE_KPI_CONTRACT, subject)
                || hasPermission(user, AccessPermissions.PROBATION_APPROVE_FINAL_ASSESSMENT, subject)
                || hasPermission(user, AccessPermissions.PROBATION_CONFIGURE, subject);
    }

    private boolean isKpiContractEditable(ProbationAssessment assessment) {
        return statusMatches(assessment, PMConstants.PROBATION_STATUS_CONTRACT_CREATED)
                || statusMatches(assessment, PMConstants.PROBATION_STATUS_KPI_SET)
                || statusMatches(assessment, PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_SUPERVISOR)
                || statusMatches(assessment, PMConstants.PROBATION_STATUS_DRAFT)
                || statusMatches(assessment, PMConstants.PROBATION_STATUS_REJECTED);
    }

    private boolean isIncumbentEvaluationEditable(ProbationAssessment assessment) {
        return statusMatches(assessment, PMConstants.PROBATION_STATUS_KPI_APPROVED)
                || statusMatches(assessment, PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_SUPERVISOR);
    }

    private boolean isSupervisorEvaluationEditable(ProbationAssessment assessment) {
        return statusMatches(assessment, PMConstants.PROBATION_STATUS_EVALUATION_PENDING_SUPERVISOR_REVIEW);
    }

    private boolean statusMatches(ProbationAssessment assessment, String status) {
        return assessment != null && assessment.getStatus() != null && status.equalsIgnoreCase(assessment.getStatus());
    }

    private boolean isAllowedStatusTransition(ProbationAssessment assessment, String nextStatus, Account user) {
        if (assessment == null || user == null || nextStatus == null) {
            return false;
        }
        if (statusMatches(assessment, nextStatus)) {
            return true;
        }

        if (isOwner(assessment, user)) {
            if (PMConstants.PROBATION_STATUS_KPI_SET.equalsIgnoreCase(nextStatus)) {
                return isKpiContractEditable(assessment);
            }
            if (PMConstants.PROBATION_STATUS_KPI_PENDING_SUPERVISOR_APPROVAL.equalsIgnoreCase(nextStatus)) {
                return statusMatches(assessment, PMConstants.PROBATION_STATUS_KPI_SET);
            }
            if (PMConstants.PROBATION_STATUS_EVALUATION_PENDING_SUPERVISOR_REVIEW.equalsIgnoreCase(nextStatus)) {
                return isIncumbentEvaluationEditable(assessment);
            }
        }

        if (isSupervisor(assessment, user)) {
            boolean supervisorKpiReview = statusMatches(assessment, PMConstants.PROBATION_STATUS_KPI_PENDING_SUPERVISOR_APPROVAL)
                    || statusMatches(assessment, PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_HR);
            if (PMConstants.PROBATION_STATUS_KPI_PENDING_HR_APPROVAL.equalsIgnoreCase(nextStatus)
                    || PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_SUPERVISOR.equalsIgnoreCase(nextStatus)) {
                return supervisorKpiReview;
            }
            if (PMConstants.PROBATION_STATUS_PERSONAL_DIMENSIONS_IN_PROGRESS.equalsIgnoreCase(nextStatus)
                    || PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_SUPERVISOR.equalsIgnoreCase(nextStatus)) {
                return statusMatches(assessment, PMConstants.PROBATION_STATUS_EVALUATION_PENDING_SUPERVISOR_REVIEW);
            }
            if (PMConstants.PROBATION_STATUS_EVALUATION_PENDING_HR_APPROVAL.equalsIgnoreCase(nextStatus)) {
                return canUserEditDimensions(assessment, user);
            }
        }

        if (PMConstants.PROBATION_STATUS_KPI_APPROVED.equalsIgnoreCase(nextStatus)
                || PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_HR.equalsIgnoreCase(nextStatus)) {
            if (hasPermission(user, AccessPermissions.PROBATION_APPROVE_KPI_CONTRACT, assessment.getEmployee())) {
                return statusMatches(assessment, PMConstants.PROBATION_STATUS_KPI_PENDING_HR_APPROVAL);
            }
        }
        if (PMConstants.PROBATION_STATUS_EVALUATION_COMPLETED.equalsIgnoreCase(nextStatus)
                || PMConstants.PROBATION_STATUS_EVALUATION_REJECTED_BY_HR.equalsIgnoreCase(nextStatus)) {
            if (hasPermission(user, AccessPermissions.PROBATION_APPROVE_FINAL_ASSESSMENT, assessment.getEmployee())) {
                return statusMatches(assessment, PMConstants.PROBATION_STATUS_EVALUATION_PENDING_HR_APPROVAL);
            }
        }
        return false;
    }

    private boolean hasPermission(Account user, String permissionCode, Account subject) {
        return accessControlService != null && accessControlService.hasPermission(user, permissionCode, subject);
    }

    private boolean isValidMark(Double mark) {
        return mark == null || (!mark.isNaN() && !mark.isInfinite() && mark >= 1.0 && mark <= 5.0);
    }

    private boolean isValidProgress(Double progress) {
        return progress == null || (!progress.isNaN() && !progress.isInfinite() && progress >= 0.0 && progress <= 100.0);
    }

    private double average(double total, int count) {
        if (count == 0) {
            return 0.0;
        }
        return Math.round((total / count) * 100.0) / 100.0;
    }

    private String resolvePerformanceBand(double mark) {
        if (mark >= 4.5) {
            return "Outstanding";
        }
        if (mark >= 3.5) {
            return "Exceeds expectations";
        }
        if (mark >= 2.5) {
            return "Meets expectations";
        }
        if (mark >= 1.5) {
            return "Needs improvement";
        }
        if (mark > 0.0) {
            return "Unsatisfactory";
        }
        return "Not assessed";
    }

    private String resolveRecommendation(int totalKpis,
                                         int incumbentCount,
                                         int supervisorCount,
                                         int progressCount,
                                         int flaggedCount,
                                         double resultMark,
                                         double averageProgress) {
        if (totalKpis == 0) {
            return "No result is available until KPI contract items are added.";
        }
        if (incumbentCount < totalKpis || progressCount < totalKpis) {
            return "Incumbent evaluation is incomplete; capture a mark and progress for every KPI.";
        }
        if (supervisorCount < totalKpis) {
            return "Supervisor evaluation is incomplete; capture a supervisor mark for every KPI.";
        }
        if (flaggedCount > 0) {
            return "HR decision required: review flagged KPI items before confirming an outcome.";
        }
        if (resultMark >= 3.5 && averageProgress >= 70.0) {
            return "Performance supports confirmation, subject to HR review of the full assessment.";
        }
        if (resultMark >= 2.5) {
            return "Consider confirmation with targeted follow-up on weaker KPI areas.";
        }
        return "Consider extending probation or starting formal improvement action before confirmation.";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isValidDateRange(String startDate, String endDate) {
        if (!hasText(startDate) || !hasText(endDate)) {
            return false;
        }
        try {
            LocalDate start = LocalDate.parse(startDate.trim());
            LocalDate end = LocalDate.parse(endDate.trim());
            return !end.isBefore(start);
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean hasOverlappingOpenAssessment(Account employee, String startDate, String endDate) {
        List<ProbationAssessment> existingAssessments = assessmentRepository.findProbationAssessmentsByEmployeeOrderByDateDesc(employee);
        if (existingAssessments == null || existingAssessments.isEmpty()) {
            return false;
        }
        LocalDate requestedStart = LocalDate.parse(startDate.trim());
        LocalDate requestedEnd = LocalDate.parse(endDate.trim());
        for (ProbationAssessment existing : existingAssessments) {
            if (existing == null || isCompletedProbationStatus(existing.getStatus())
                    || !isValidDateRange(existing.getStartDate(), existing.getEndDate())) {
                continue;
            }
            LocalDate existingStart = LocalDate.parse(existing.getStartDate().trim());
            LocalDate existingEnd = LocalDate.parse(existing.getEndDate().trim());
            if (!requestedEnd.isBefore(existingStart) && !requestedStart.isAfter(existingEnd)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCompletedProbationStatus(String status) {
        return PMConstants.PROBATION_STATUS_EVALUATION_COMPLETED.equalsIgnoreCase(status)
                || PMConstants.PROBATION_STATUS_AUTHORIZED.equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status)
                || "CLOSED".equalsIgnoreCase(status);
    }
}
