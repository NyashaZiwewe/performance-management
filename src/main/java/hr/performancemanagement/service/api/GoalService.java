package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.GoalRepository;
import hr.performancemanagement.repository.TargetRepository;
import java.util.ArrayList;
import java.util.List;

public interface GoalService {
    Goal getGoalById(long id);
    List<Goal> listAllGoals(long scorecardId);
    Goal saveGoal(Goal goal);
    double getTotalAllocatedWeight(long scorecardId);
    double getAverageManagerScore(long scorecardId);
    double getAverageEmployeeScore(long scorecardId);
    double getAverageModeratorScore(long scorecardId);
    double getAverageAgreedScore(long scorecardId);
    void deleteGoal(Goal goal);
}
