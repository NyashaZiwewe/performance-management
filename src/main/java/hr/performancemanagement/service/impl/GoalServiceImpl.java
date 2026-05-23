package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.GoalRepository;
import hr.performancemanagement.repository.TargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class GoalServiceImpl implements hr.performancemanagement.service.api.GoalService {
    @Autowired
    GoalRepository goalRepository;
    @Autowired
    private TargetRepository targetRepository;

    @Override
    public Goal getGoalById(long id){

        Goal goal = goalRepository.findGoalById(id);
        return goal;
    }
    @Override
    public List<Goal> listAllGoals(long scorecardId){
        List<Goal> goalList = new ArrayList<>();
        goalRepository.findGoalsByScorecardIdOrderByPerspectiveAscStrategicObjective(scorecardId).forEach(goal -> goalList.add(goal));
        return goalList;
    }


    @Override
    public Goal saveGoal(Goal goal) {

       Goal savedGoal = goalRepository.save(goal);
       return savedGoal;
    }

    @Override
    public double getTotalAllocatedWeight(long scorecardId){
        try {
            double total = goalRepository.sumAllocatedWeight(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    @Override
    public double getAverageManagerScore(long scorecardId){
        try {
            double total = goalRepository.averageManagerScore(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    @Override
    public double getAverageEmployeeScore(long scorecardId){
        try {
            double total = goalRepository.averageEmployeeScore(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    @Override
    public double getAverageModeratorScore(long scorecardId){
        try {
            double total = goalRepository.averageModeratedScore(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    @Override
    public double getAverageAgreedScore(long scorecardId){
        try {
            double total = goalRepository.averageAgreedScore(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    @Transactional
    @Override
    public void deleteGoal(Goal goal){
        goalRepository.delete(goal);
    }

}
