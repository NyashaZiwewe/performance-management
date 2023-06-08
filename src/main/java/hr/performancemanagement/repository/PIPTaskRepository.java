package hr.performancemanagement.repository;

import hr.performancemanagement.entities.PIPTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PIPTaskRepository extends JpaRepository<PIPTask, Long> {
    List<PIPTask> findPIPTasksByPerformanceImprovementPlan_Id(long id);
    PIPTask findPIPTaskById(long id);

}
