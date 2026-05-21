package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ProbationAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProbationAssessmentRepository extends JpaRepository<ProbationAssessment, Long> {

    List<ProbationAssessment> findProbationAssessmentsByClientIdOrderByDateDesc(long clientId);

    List<ProbationAssessment> findProbationAssessmentsByEmployeeOrderByDateDesc(Account employee);

    ProbationAssessment findProbationAssessmentById(long id);

    ProbationAssessment findProbationAssessmentByIdAndClientId(long id, long clientId);
}
