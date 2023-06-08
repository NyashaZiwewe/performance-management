package hr.performancemanagement.service;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoalService {
    @Autowired
    GoalRepository goalRepository;

    public Goal getGoalById(long id){

        Goal goal = goalRepository.findGoalById(id);
        return goal;
    }
    public List<Goal> listAllGoals(long scorecardId){
        List<Goal> goalList = new ArrayList<>();
        goalRepository.findGoalsByScorecardIdOrderByPerspectiveAscStrategicObjective(scorecardId).forEach(goal -> goalList.add(goal));
        return goalList;
    }


    public void saveGoal(Goal goal) {

        goalRepository.save(goal);
    }

    public double getTotalAllocatedWeight(long scorecardId){
        try {
            double total = goalRepository.sumAllocatedWeight(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    public double getAverageManagerScore(long scorecardId){
        try {
            double total = goalRepository.averageManagerScore(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    public double getAverageEmployeeScore(long scorecardId){
        try {
            double total = goalRepository.averageEmployeeScore(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    public double getAverageModeratorScore(long scorecardId){
        try {
            double total = goalRepository.averageModeratedScore(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }
    @Transactional
    public void deleteGoal(Goal goal){
        goalRepository.delete(goal);
    }

}
