package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ProbationDimensionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProbationDimensionTemplateRepository extends JpaRepository<ProbationDimensionTemplate, Long> {

    List<ProbationDimensionTemplate> findProbationDimensionTemplatesByClientIdOrderByDisplayOrderAsc(long clientId);

    List<ProbationDimensionTemplate> findProbationDimensionTemplatesByClientIdAndStatusOrderByDisplayOrderAsc(long clientId, String status);

    ProbationDimensionTemplate findProbationDimensionTemplateById(long id);

    ProbationDimensionTemplate findProbationDimensionTemplateByIdAndClientId(long id, long clientId);
}
