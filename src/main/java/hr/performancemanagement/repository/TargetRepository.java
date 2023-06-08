package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetRepository extends JpaRepository<Target, Long> {
    List<Target> findTargetsByGoalId(long id);
}
