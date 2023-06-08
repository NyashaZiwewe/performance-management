package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findTasksByActionPlan_Id(long id);
    Task findTaskById(long id);

}
