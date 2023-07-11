package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetRepository extends JpaRepository<Target, Long> {
    List<Target> findTargetsByGoalId(long id);
    Target findTargetById(long id);
    int countTargetsByGoal(Goal goal);

}
