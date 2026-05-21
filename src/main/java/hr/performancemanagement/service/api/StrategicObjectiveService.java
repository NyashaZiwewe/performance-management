package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.entities.StrategicObjective;
import hr.performancemanagement.repository.ReportingPeriodRepository;
import hr.performancemanagement.repository.StrategicObjectiveRepository;
import java.util.ArrayList;
import java.util.List;

public interface StrategicObjectiveService {
    StrategicObjective getStrategicObjectiveById(long id);
    List<StrategicObjective> listAllStrategicObjectives(long reportingPeriodId);
    long countStrategicObjectives(long reportingPeriodId);
    List<StrategicObjective> listStrategicObjectivesByScorecard(long scorecardId);
    void addStrategicObjective(StrategicObjective strategicObjective);
    StrategicObjective saveStrategicObjective(StrategicObjective strategicObjective);
}
