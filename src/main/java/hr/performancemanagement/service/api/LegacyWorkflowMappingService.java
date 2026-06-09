package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.ScorecardWorkflowMappingAudit;
import hr.performancemanagement.entities.ScorecardWorkflowStage;

import java.util.List;

public interface LegacyWorkflowMappingService {

    List<Scorecard> listUnmappedScorecards();

    List<ScorecardWorkflowStage> listActiveWorkflowStages();

    List<ScorecardWorkflowMappingAudit> listRecentAudit();

    int saveWorkflowStageMappings(List<Long> scorecardIds,
                                  long workflowStageId,
                                  boolean initializeReportingDateStages,
                                  String reason);
}
