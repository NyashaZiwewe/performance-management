package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.*;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


@Service
public class ProbationAssessmentServiceImpl implements hr.performancemanagement.service.api.ProbationAssessmentService {

    @Autowired
    private ProbationAssessmentRepository assessmentRepository;
    @Autowired
    private ProbationAssessmentDimensionRepository assessmentDimensionRepository;
    @Autowired
    private ProbationKpiRepository probationKpiRepository;
    @Autowired
    private ProbationAssessmentApprovalRepository approvalRepository;
    @Autowired
    private ProbationConfigService probationConfigService;
    @Autowired
    private CommonService commonService;
    @Autowired
    private AccountService accountService;

    @Override
    public List<ProbationAssessment> listVisibleAssessments() {
        Account loggedUser = commonService.getLoggedUser();
        List<ProbationAssessment> allAssessments = assessmentRepository.findProbationAssessmentsByClientIdOrderByDateDesc(loggedUser.getClientId());
        if (commonService.isAdmin() || commonService.hasSpecialRights()) {
            return allAssessments;
        }

        List<ProbationAssessment> visibleAssessments = new ArrayList<>();
        for (ProbationAssessment assessment : allAssessments) {
            if (isOwner(assessment, loggedUser) || isSupervisor(assessment, loggedUser) || canUserApprove(assessment, loggedUser)) {
                visibleAssessments.add(assessment);
            }
        }
        return visibleAssessments;
    }

    @Override
    public ProbationAssessment getAssessmentById(long id) {
        Account loggedUser = commonService.getLoggedUser();
        return assessmentRepository.findProbationAssessmentByIdAndClientId(id, loggedUser.getClientId());
    }

    @Override
    public ProbationAssessment createAssessment(ProbationAssessment assessment) {
        Account loggedUser = commonService.getLoggedUser();
        if (assessment.getEmployee() == null) {
            return null;
        }
        Account selectedEmployee = accountService.getAccountById(assessment.getEmployee().getId());
        if (!isSameClientAccount(selectedEmployee, loggedUser.getClientId())) {
            return null;
        }
        assessment.setEmployee(selectedEmployee);
        assessment.setClientId(loggedUser.getClientId());
        assessment.setStatus(PMConstants.PROBATION_STATUS_DRAFT);
        assessment.setCurrentStepOrder(null);
        assessment.setCurrentStepName(null);
        ProbationAssessment savedAssessment = assessmentRepository.save(assessment);
        initializeDimensions(savedAssessment);
        return savedAssessment;
    }

    @Override
    public ProbationAssessment updateAssessmentCore(ProbationAssessment updatedAssessment) {
        ProbationAssessment existingAssessment = getAssessmentById(updatedAssessment.getId());
        Account loggedUser = commonService.getLoggedUser();
        if (existingAssessment == null) {
            return null;
        }

        if (isOwner(existingAssessment, loggedUser)) {
            existingAssessment.setPerformancePeriod(updatedAssessment.getPerformancePeriod());
            existingAssessment.setStartDate(updatedAssessment.getStartDate());
            existingAssessment.setEndDate(updatedAssessment.getEndDate());
            existingAssessment.setGeneralObservations(updatedAssessment.getGeneralObservations());
            existingAssessment.setEmployeeComment(updatedAssessment.getEmployeeComment());
        }
        if (isSupervisor(existingAssessment, loggedUser)) {
            existingAssessment.setSupervisorComment(updatedAssessment.getSupervisorComment());
        }
        if (!isOwner(existingAssessment, loggedUser) && !isSupervisor(existingAssessment, loggedUser)) {
            return null;
        }
        return assessmentRepository.save(existingAssessment);
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
    public ProbationAssessmentDimension saveDimensionResponse(long assessmentId, long dimensionTemplateId, String strengths, String areasForImprovement) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        ProbationDimensionTemplate template = probationConfigService.getDimensionTemplateById(dimensionTemplateId);
        Account loggedUser = commonService.getLoggedUser();
        if (assessment == null || template == null || !canUserEditAssessmentContent(assessment, loggedUser)) {
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
        return assessmentDimensionRepository.save(dimension);
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
    public ProbationKpi addKpi(long assessmentId, ProbationKpi kpi) {
        ProbationAssessment assessment = getAssessmentById(assessmentId);
        if (assessment == null || !canUserEditAssessmentContent(assessment, commonService.getLoggedUser())) {
            return null;
        }
        kpi.setAssessment(assessment);
        if (kpi.getStatus() == null || kpi.getStatus().isEmpty()) {
            kpi.setStatus(PMConstants.STATUS_ACTIVE);
        }
        return probationKpiRepository.save(kpi);
    }

    @Override
    public ProbationKpi updateKpi(ProbationKpi newKpi) {
        ProbationKpi existingKpi = probationKpiRepository.findProbationKpiById(newKpi.getId());
        if (existingKpi == null || !isAssessmentAccessible(existingKpi.getAssessment()) || !canUserEditAssessmentContent(existingKpi.getAssessment(), commonService.getLoggedUser())) {
            return null;
        }
        existingKpi.setName(newKpi.getName());
        existingKpi.setTarget(newKpi.getTarget());
        existingKpi.setProgressPercent(newKpi.getProgressPercent());
        existingKpi.setProgressComment(newKpi.getProgressComment());
        existingKpi.setIncumbentComment(newKpi.getIncumbentComment());
        existingKpi.setSupervisorComment(newKpi.getSupervisorComment());
        return probationKpiRepository.save(existingKpi);
    }

    @Override
    public void deleteKpi(long kpiId) {
        ProbationKpi kpi = probationKpiRepository.findProbationKpiById(kpiId);
        if (kpi != null && isAssessmentAccessible(kpi.getAssessment()) && canUserEditAssessmentContent(kpi.getAssessment(), commonService.getLoggedUser())) {
            probationKpiRepository.delete(kpi);
        }
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

    private void initializeDimensions(ProbationAssessment assessment) {
        List<ProbationDimensionTemplate> templates = probationConfigService.listActiveDimensionTemplates();
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

    private boolean canUserEditAssessmentContent(ProbationAssessment assessment, Account user) {
        return isOwner(assessment, user) || isSupervisor(assessment, user);
    }

    private boolean isAssessmentAccessible(ProbationAssessment assessment) {
        Account loggedUser = commonService.getLoggedUser();
        return assessment != null && assessment.getClientId() == loggedUser.getClientId();
    }

    private boolean isSameClientAccount(Account account, long clientId) {
        return account != null && account.getClientId() == clientId;
    }
}
