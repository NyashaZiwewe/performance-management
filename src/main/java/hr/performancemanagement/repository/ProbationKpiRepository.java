package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ProbationAssessment;
import hr.performancemanagement.entities.ProbationKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProbationKpiRepository extends JpaRepository<ProbationKpi, Long> {

    List<ProbationKpi> findProbationKpisByAssessmentOrderByIdAsc(ProbationAssessment assessment);

    ProbationKpi findProbationKpiById(long id);

    ProbationKpi findProbationKpiByIdAndAssessment_ClientId(long id, long clientId);
}
