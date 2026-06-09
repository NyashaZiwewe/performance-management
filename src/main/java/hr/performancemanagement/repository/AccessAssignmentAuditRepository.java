package hr.performancemanagement.repository;

import hr.performancemanagement.entities.AccessAssignmentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessAssignmentAuditRepository extends JpaRepository<AccessAssignmentAudit, Long> {
    List<AccessAssignmentAudit> findTop100ByClientIdOrderByIdDesc(long clientId);
}
