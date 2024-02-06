package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Output;
import hr.performancemanagement.entities.Scorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutputRepository extends JpaRepository<Output, Long> {

    Output findOutputById(long id);
    List<Output> findOutputsByScorecard(Scorecard scorecard);
    boolean existsByName(String name);
    Output findOutputByName(String name);
}
