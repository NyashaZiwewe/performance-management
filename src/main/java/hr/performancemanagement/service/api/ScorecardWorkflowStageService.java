package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.ScorecardWorkflowStage;

import java.util.List;

public interface ScorecardWorkflowStageService {
    List<ScorecardWorkflowStage> listAllWorkflowStages();
    List<ScorecardWorkflowStage> listActiveWorkflowStages();
    ScorecardWorkflowStage getWorkflowStageById(long id);
    ScorecardWorkflowStage getWorkflowStageByRoleKey(String roleKey);
    ScorecardWorkflowStage saveWorkflowStage(ScorecardWorkflowStage stage);
    void deactivateWorkflowStage(long id);
    void deleteWorkflowStage(long id);
    List<String> listWorkflowStages();
}
