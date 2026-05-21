package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ProbationWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProbationWorkflowStepRepository extends JpaRepository<ProbationWorkflowStep, Long> {

    List<ProbationWorkflowStep> findProbationWorkflowStepsByClientIdOrderByStepOrderAsc(long clientId);

    List<ProbationWorkflowStep> findProbationWorkflowStepsByClientIdAndStatusOrderByStepOrderAsc(long clientId, String status);

    ProbationWorkflowStep findProbationWorkflowStepById(long id);

    ProbationWorkflowStep findProbationWorkflowStepByIdAndClientId(long id, long clientId);
}
