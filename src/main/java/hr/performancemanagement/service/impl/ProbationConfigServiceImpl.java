package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.ProbationDimensionTemplate;
import hr.performancemanagement.entities.ProbationWorkflowStep;
import hr.performancemanagement.repository.ProbationAssessmentDimensionRepository;
import hr.performancemanagement.repository.ProbationDimensionTemplateRepository;
import hr.performancemanagement.repository.ProbationWorkflowStepRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;


@Service
public class ProbationConfigServiceImpl implements hr.performancemanagement.service.api.ProbationConfigService {

    @Autowired
    private ProbationDimensionTemplateRepository dimensionTemplateRepository;
    @Autowired
    private ProbationAssessmentDimensionRepository assessmentDimensionRepository;
    @Autowired
    private ProbationWorkflowStepRepository workflowStepRepository;
    @Autowired
    private CommonService commonService;
    @Autowired
    private AccountService accountService;

    @Override
    public List<ProbationDimensionTemplate> listAllDimensionTemplates() {
        return dimensionTemplateRepository.findProbationDimensionTemplatesByClientIdOrderByDisplayOrderAsc(commonService.getLoggedUser().getClientId());
    }

    @Override
    public List<ProbationDimensionTemplate> listActiveDimensionTemplates() {
        return dimensionTemplateRepository.findProbationDimensionTemplatesByClientIdAndStatusOrderByDisplayOrderAsc(commonService.getLoggedUser().getClientId(), PMConstants.STATUS_ACTIVE);
    }

    @Override
    public ProbationDimensionTemplate getDimensionTemplateById(long id) {
        return dimensionTemplateRepository.findProbationDimensionTemplateByIdAndClientId(id, commonService.getLoggedUser().getClientId());
    }

    @Override
    public ProbationDimensionTemplate saveDimensionTemplate(ProbationDimensionTemplate template) {
        if (template == null) {
            return null;
        }
        if (template.getId() > 0) {
            ProbationDimensionTemplate existingTemplate = getDimensionTemplateById(template.getId());
            if (existingTemplate == null) {
                return null;
            }
            existingTemplate.setCode(template.getCode());
            existingTemplate.setTitle(template.getTitle());
            existingTemplate.setDescription(template.getDescription());
            existingTemplate.setDisplayOrder(template.getDisplayOrder() == null ? 100 : template.getDisplayOrder());
            if (StringUtils.hasText(template.getStatus())) {
                existingTemplate.setStatus(template.getStatus());
            } else if (!StringUtils.hasText(existingTemplate.getStatus())) {
                existingTemplate.setStatus(PMConstants.STATUS_ACTIVE);
            }
            return dimensionTemplateRepository.save(existingTemplate);
        }
        if (template.getClientId() < 1) {
            template.setClientId(commonService.getLoggedUser().getClientId());
        }
        if (template.getStatus() == null || template.getStatus().isEmpty()) {
            template.setStatus(PMConstants.STATUS_ACTIVE);
        }
        if (template.getDisplayOrder() == null) {
            template.setDisplayOrder(100);
        }
        return dimensionTemplateRepository.save(template);
    }

    @Override
    public void deactivateDimensionTemplate(long id) {
        ProbationDimensionTemplate template = getDimensionTemplateById(id);
        if (template != null) {
            template.setStatus(PMConstants.STATUS_IN_ACTIVE);
            saveDimensionTemplate(template);
        }
    }

    @Override
    public boolean deleteDimensionTemplate(long id) {
        ProbationDimensionTemplate template = getDimensionTemplateById(id);
        if (template == null) {
            return false;
        }
        if (assessmentDimensionRepository.existsProbationAssessmentDimensionByDimensionTemplate(template)) {
            template.setStatus(PMConstants.STATUS_IN_ACTIVE);
            saveDimensionTemplate(template);
            return false;
        }
        dimensionTemplateRepository.delete(template);
        return true;
    }

    @Override
    public List<ProbationWorkflowStep> listAllWorkflowSteps() {
        return workflowStepRepository.findProbationWorkflowStepsByClientIdOrderByStepOrderAsc(commonService.getLoggedUser().getClientId());
    }

    @Override
    public List<ProbationWorkflowStep> listActiveWorkflowSteps() {
        return workflowStepRepository.findProbationWorkflowStepsByClientIdAndStatusOrderByStepOrderAsc(commonService.getLoggedUser().getClientId(), PMConstants.STATUS_ACTIVE);
    }

    @Override
    public ProbationWorkflowStep getWorkflowStepById(long id) {
        return workflowStepRepository.findProbationWorkflowStepByIdAndClientId(id, commonService.getLoggedUser().getClientId());
    }

    @Override
    public ProbationWorkflowStep saveWorkflowStep(ProbationWorkflowStep step) {
        long clientId = commonService.getLoggedUser().getClientId();
        if (step.getClientId() < 1) {
            step.setClientId(clientId);
        }
        if (step.getStatus() == null || step.getStatus().isEmpty()) {
            step.setStatus(PMConstants.STATUS_ACTIVE);
        }
        if (step.getStepOrder() == null) {
            step.setStepOrder(10);
        }
        if (step.getSameDivisionOnly() == null) {
            step.setSameDivisionOnly(false);
        }
        if (step.getApproverAccount() != null && step.getApproverAccount().getId() > 0) {
            step.setApproverAccount(accountService.getAccountById(step.getApproverAccount().getId()));
            if (step.getApproverAccount() == null || step.getApproverAccount().getClientId() != clientId) {
                step.setApproverAccount(null);
            }
        } else {
            step.setApproverAccount(null);
        }
        return workflowStepRepository.save(step);
    }

    @Override
    public void deactivateWorkflowStep(long id) {
        ProbationWorkflowStep step = getWorkflowStepById(id);
        if (step != null) {
            step.setStatus(PMConstants.STATUS_IN_ACTIVE);
            saveWorkflowStep(step);
        }
    }

    @Override
    public List<String> listApproverModes() {
        return Arrays.asList(
                PMConstants.PROBATION_APPROVER_MODE_SUPERVISOR,
                PMConstants.PROBATION_APPROVER_MODE_ACCOUNT_TYPE,
                PMConstants.PROBATION_APPROVER_MODE_ROLE,
                PMConstants.PROBATION_APPROVER_MODE_USER
        );
    }
}
