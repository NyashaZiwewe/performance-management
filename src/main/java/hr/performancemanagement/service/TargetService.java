package hr.performancemanagement.service;

import hr.performancemanagement.entities.*;
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
    @Autowired
    ReportingDateService reportingDateService;

    public List<Target> getAllTargetsByScorecard(long scorecardId){
        List<Target> targetList = new ArrayList<>();

        List<Goal> goalList = goalRepository.findGoalsByScorecardIdOrderByPerspectiveAscStrategicObjective(scorecardId);
//        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();

        for(Goal goal: goalList){
           List<Target> targets = targetRepository.findTargetsByGoalId(goal.getId());
            for(Target target: targets){
                target.setPerspective(goal.getPerspective());
                target.setStrategicObjective(goal.getStrategicObjective());
//                target.setCurrentActual(targetRepository.currentActual(target, reportingDate));
//                target.setCurrentEmployeeScore(targetRepository.currentEmployeeScore(target,reportingDate));
//                target.setCurrentManagerScore(targetRepository.currentManagerScore(target,reportingDate));
//                target.setCurrentAgreedScore(targetRepository.currentAgreedScore(target,reportingDate));
//                target.setCurrentModeratedScore(targetRepository.currentModeratedScore(target,reportingDate));
//                target.setCurrentEvidence(targetRepository.currentEvidence(target, reportingDate));
//                target.setCurrentJustification(targetRepository.currentJustification(target, reportingDate));
//                target.setCurrentWeightedScore(targetRepository.currentWeightedScore(target,reportingDate));
                targetList.add(target);
            }
        }

        return targetList;
    }

    public List<Target> getAllTargetsByGoal(Goal goal){
        List<Target> targets = targetRepository.findTargetsByGoalId(goal.getId());
        return targets;
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
