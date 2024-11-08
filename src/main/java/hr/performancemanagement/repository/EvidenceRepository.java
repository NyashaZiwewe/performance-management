package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Evidence;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    boolean existsEvidenceByTargetAndReportingDate(Target target, ReportingDate reportingDate);
    Evidence getEvidenceByTargetAndReportingDate(Target target, ReportingDate reportingDate);
}
