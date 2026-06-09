package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ScorecardWorkflowMappingAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScorecardWorkflowMappingAuditRepository extends JpaRepository<ScorecardWorkflowMappingAudit, Long> {

    List<ScorecardWorkflowMappingAudit> findTop100ByClientIdOrderByIdDesc(long clientId);
}
