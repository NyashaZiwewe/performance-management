package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ProbationKpi;
import hr.performancemanagement.entities.ProbationKpiComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProbationKpiCommentRepository extends JpaRepository<ProbationKpiComment, Long> {

    List<ProbationKpiComment> findProbationKpiCommentsByProbationKpiOrderByDateAsc(ProbationKpi probationKpi);
}
