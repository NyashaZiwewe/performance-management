package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.GoalRepository;
import hr.performancemanagement.repository.TargetRepository;
import java.util.ArrayList;
import java.util.List;

public interface TargetService {
    List<Target> getAllTargetsByScorecard(long scorecardId);
    List<Target> getAllTargetsByGoal(Goal goal);
    List<String> listAllUnits();
    Target getTargetById(long id);
    boolean checkIfGoalHasTargets(Goal goal);
    boolean checkIfOutputHasTargets(Output output);
    void deleteTargets(List<Target> targets);
    //    public boolean  updateWeightedTargetScore(Target target);
    //    public boolean  updateActualTargetScore(Target target);
    //    public boolean  updateManagerTargetScore(Target target);
    //    public boolean  updateEmployeeTargetScore(Target target);
    Target saveTarget(Target target);
    void deleteTarget(Target target);
}
