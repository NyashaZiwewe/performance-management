package hr.performancemanagement.service;

import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.GoalRepository;
import hr.performancemanagement.repository.TargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TargetService {
    @Autowired
    TargetRepository targetRepository;
    @Autowired
    GoalRepository goalRepository;

    public List<Target> getAllTargetsByScorecard(long scorecardId){
        List<Target> targetList = new ArrayList<>();

        List<Goal> goalList = goalRepository.findGoalsByScorecardIdOrderByPerspectiveAscStrategicObjective(scorecardId);

        for(Goal goal: goalList){
           List<Target> targets = targetRepository.findTargetsByGoalId(goal.getId());
            for(Target target: targets){
                target.setPerspective(goal.getPerspective());
                target.setStrategicObjective(goal.getStrategicObjective());
                targetList.add(target);
            }
        }

        return targetList;
    }
    public Target getTargetById(long id){
        Target target = targetRepository.findTargetById(id);
        return target;
    }

    public boolean checkIfGoalHasTargets(Goal goal){
        int count = targetRepository.countTargetsByGoal(goal);
        if(count < 1){
            return false;
        }else {
            return true;
        }
    }

//    public boolean  updateWeightedTargetScore(Target target){
//
//        double sumActual = targetRepository.totalWeightedScoreByTarget(target);
//        target.setWeightedScore(sumActual);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }
//
//    public boolean  updateActualTargetScore(Target target){
//
//        double avg = targetRepository.averageActualScoreByTarget(target);
//        target.setActualScore(avg);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }
//
//    public boolean  updateManagerTargetScore(Target target){
//
//        double avg = targetRepository.averageManagerScoreByTarget(target);
//        target.setManagerScore(avg);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }
//
//    public boolean  updateEmployeeTargetScore(Target target){
//
//        double avg = targetRepository.averageEmployeeScoreByTarget(target);
//        target.setEmployeeScore(avg);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }


    public Target saveTarget(Target target) {
        Target savedTarget = targetRepository.save(target);
        return savedTarget;
    }

    @Transactional
    public void deleteTarget(Target target){
        targetRepository.delete(target);
    }
}
