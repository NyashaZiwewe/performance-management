package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.StrategicObjective;
import java.util.List;

public interface StrategicObjectiveService {
    StrategicObjective getStrategicObjectiveById(long id);
    List<StrategicObjective> listAllStrategicObjectives(long reportingPeriodId);
    long countStrategicObjectives(long reportingPeriodId);
    void addStrategicObjective(StrategicObjective strategicObjective);
    StrategicObjective saveStrategicObjective(StrategicObjective strategicObjective);
    void deleteStrategicObjective(StrategicObjective strategicObjective);
}
