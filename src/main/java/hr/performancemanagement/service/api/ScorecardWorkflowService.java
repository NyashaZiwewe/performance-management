package hr.performancemanagement.service.api;

import hr.performancemanagement.utils.dto.ScorecardWorkflowDefinition;

public interface ScorecardWorkflowService {
    ScorecardWorkflowDefinition getWorkflowDefinition();
    boolean matches(String actualStatus, String expectedStatus);
}
