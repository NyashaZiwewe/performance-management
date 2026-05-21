package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ProbationAssessment;
import hr.performancemanagement.entities.ProbationAssessmentApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProbationAssessmentApprovalRepository extends JpaRepository<ProbationAssessmentApproval, Long> {

    List<ProbationAssessmentApproval> findProbationAssessmentApprovalsByAssessmentOrderByDateDesc(ProbationAssessment assessment);
}
