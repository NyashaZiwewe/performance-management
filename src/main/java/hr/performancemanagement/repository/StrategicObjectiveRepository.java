package hr.performancemanagement.repository;

import hr.performancemanagement.entities.StrategicObjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StrategicObjectiveRepository extends JpaRepository<StrategicObjective, Long> {

    List<StrategicObjective> findStrategicObjectivesByReportingPeriodId(long reportingPeriodId);
    StrategicObjective findStrategicObjectiveById(long id);
}
