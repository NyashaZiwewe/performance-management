package hr.performancemanagement.service;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.repository.GoalRepository;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoalService {

    @Autowired
    GoalRepository goalRepository;

    public Goal getGoalById(long id){

        Goal Goal = goalRepository.findGoalById(id);
        return Goal;
    }
    public List<Goal> listAllGoals(long clientId)
    {
        List<Goal> GoalList = new ArrayList<>();
        goalRepository.findGoalsByGear_ClientId(clientId).forEach(Goal -> GoalList.add(Goal));
        return GoalList;
    }

    public List<Goal> listGoalsByScorecard(long scorecardId)
    {
        List<Goal> GoalList = new ArrayList<>();
        goalRepository.goalsByScorecard(scorecardId).forEach(Goal -> GoalList.add(Goal));
        for(Goal objective : GoalList){
            objective.setWeightedScore(goalRepository.weightedScoreByScorecardAndGoal(objective));
        }
        return GoalList;
    }

    public Goal addGoal(Goal Goal, HttpServletRequest request) {
       try {
           Goal goal = goalRepository.save(Goal);
           return goal;
       }catch (Exception e){
           PortletUtils.addErrorMsg(e.getMessage(), request);
       }
       return null;
    }

    public Goal saveGoal(Goal Goal){
        Goal savedGoal = goalRepository.save(Goal);
        return savedGoal;
    }
}
