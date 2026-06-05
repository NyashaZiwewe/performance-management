package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.ProbationDimensionTemplate;
import hr.performancemanagement.entities.ProbationWorkflowStep;
import hr.performancemanagement.repository.ProbationDimensionTemplateRepository;
import hr.performancemanagement.repository.ProbationWorkflowStepRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import java.util.Arrays;
import java.util.List;

public interface ProbationConfigService {
    List<ProbationDimensionTemplate> listAllDimensionTemplates();
    List<ProbationDimensionTemplate> listActiveDimensionTemplates();
    ProbationDimensionTemplate getDimensionTemplateById(long id);
    ProbationDimensionTemplate saveDimensionTemplate(ProbationDimensionTemplate template);
    void deactivateDimensionTemplate(long id);
    boolean deleteDimensionTemplate(long id);
    List<ProbationWorkflowStep> listAllWorkflowSteps();
    List<ProbationWorkflowStep> listActiveWorkflowSteps();
    ProbationWorkflowStep getWorkflowStepById(long id);
    ProbationWorkflowStep saveWorkflowStep(ProbationWorkflowStep step);
    void deactivateWorkflowStep(long id);
    List<String> listApproverModes();
}
