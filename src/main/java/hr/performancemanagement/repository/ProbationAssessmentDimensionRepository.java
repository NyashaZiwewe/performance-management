package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ProbationAssessment;
import hr.performancemanagement.entities.ProbationAssessmentDimension;
import hr.performancemanagement.entities.ProbationDimensionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProbationAssessmentDimensionRepository extends JpaRepository<ProbationAssessmentDimension, Long> {

    List<ProbationAssessmentDimension> findProbationAssessmentDimensionsByAssessmentOrderByDimensionTemplate_DisplayOrderAsc(ProbationAssessment assessment);

    ProbationAssessmentDimension findProbationAssessmentDimensionByAssessmentAndDimensionTemplate(ProbationAssessment assessment, ProbationDimensionTemplate dimensionTemplate);

    ProbationAssessmentDimension findProbationAssessmentDimensionById(long id);
}
