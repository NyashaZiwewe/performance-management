package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.*;
import java.util.List;

public interface TargetService {
    List<Target> getAllTargetsByScorecard(long scorecardId);
    List<Target> getAllTargetsByGoal(Goal goal);
    List<String> listAllUnits();
    Target getTargetById(long id);
    boolean checkIfGoalHasTargets(Goal goal);
    boolean checkIfOutputHasTargets(Output output);
    void deleteTargets(List<Target> targets);
    Target saveTarget(Target target);
    void deleteTarget(Target target);
}
