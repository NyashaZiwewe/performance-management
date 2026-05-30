package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ScorecardWorkflowStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScorecardWorkflowStageRepository extends JpaRepository<ScorecardWorkflowStage, Long> {

    List<ScorecardWorkflowStage> findScorecardWorkflowStagesByClientIdOrderByStageOrderAsc(long clientId);

    List<ScorecardWorkflowStage> findScorecardWorkflowStagesByClientIdAndStatusOrderByStageOrderAsc(long clientId, String status);

    ScorecardWorkflowStage findScorecardWorkflowStageByIdAndClientId(long id, long clientId);

    ScorecardWorkflowStage findScorecardWorkflowStageByClientIdAndRoleKey(long clientId, String roleKey);
}
