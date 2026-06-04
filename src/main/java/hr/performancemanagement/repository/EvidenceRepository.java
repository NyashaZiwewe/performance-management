package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Evidence;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    boolean existsEvidenceByTargetAndReportingDate(Target target, ReportingDate reportingDate);
    List<Evidence> findEvidenceByTargetAndReportingDateOrderByIdDesc(Target target, ReportingDate reportingDate);
    List<Evidence> findEvidenceByTargetOrderByReportingDate_DateDescIdDesc(Target target);
    List<Evidence> findEvidenceByTarget_IdAndReportingDate_IdOrderByIdDesc(long targetId, long reportingDateId);
    List<Evidence> findEvidenceByTarget_IdOrderByReportingDate_DateDescIdDesc(long targetId);
}
