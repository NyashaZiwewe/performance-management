package hr.performancemanagement.service.ScoreService;
import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.ScorecardService;
import hr.performancemanagement.service.TargetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;

@Service
public class ValueBasedScoreService {
    @Autowired
    ScoreRepository scoreRepository;
    @Autowired
    private TargetService targetService;
    @Autowired
    private ScorecardService scorecardService;
    @Autowired
    HttpSession session;

    public double calculateWeightedScore(Score score){

        Target target = score.getTarget();
        double actualScore = score.getActualScore();
        double allocatedWeight = target.getAllocatedWeight();
        double weightedRating = 0;

        try {
            weightedRating = (actualScore/5) * allocatedWeight;
        }catch (Exception ignored){

        }

        return weightedRating;
    }

    public Score saveScore(Score score) {

        boolean exists = scoreExists(score);
        Score savedScore;
        if(exists){

            Target target = targetService.getTargetById(score.getTarget().getId());
            Scorecard scorecard = scorecardService.getScorecardById(target.getGoal().getScorecardId());
            Account loggedUser = (Account) session.getAttribute("loggedUser");

            long loggedUserId = loggedUser.getId();
            long ownerId = scorecard.getOwner().getId();
            long supervisorId = scorecard.getOwner().getSupervisor().getId();
//            String role = loggedUser.getRole();
            String approvalStatus = scorecard.getApprovalStatus();
            boolean isOwner = false;
            boolean isSupervisor = false;
            boolean isModerator = false;

            if (loggedUserId == ownerId) {
                isOwner = true;
            } else if (loggedUserId == supervisorId && "SCORED_BY_EMPLOYEE".equals(approvalStatus)) {
                isSupervisor = true;
            } else if (loggedUserId == supervisorId && "SCORED_BY_SUPERVISOR".equals(approvalStatus)) {
                isModerator = true;
            }else{
                System.out.println("User can not capture scores");
            }

            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
            //score.setId(existingScore.getId());
            if(isOwner){
                existingScore.setEmployeeScore(score.getEmployeeScore());
                existingScore.setEvidence(score.getEvidence());
                existingScore.setJustification(score.getJustification());
            }else if(isSupervisor){
                existingScore.setManagerScore(score.getManagerScore());
            }else if(isModerator){
                existingScore.setActualScore(score.getActualScore());
            }
            existingScore.setWeightedScore(calculateWeightedScore(existingScore));
            savedScore = scoreRepository.save(existingScore);
        }else{
            score.setWeightedScore(calculateWeightedScore(score));
            savedScore = scoreRepository.save(score);
        }

        updateTargetData(savedScore.getTarget());
        return savedScore;
    }

    public boolean scoreExists(Score score){
        return scoreRepository.existsScoresByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
    }

    public boolean  updateTargetData(Target target){

        double weightedRating = scoreRepository.totalWeightedScoreByTarget(target);
        double employeeScore = scoreRepository.averageEmployeeScoreByTarget(target);
        double managerScore = scoreRepository.averageManagerScoreByTarget(target);
        double actualScore = scoreRepository.averageActualScoreByTarget(target);

        target.setEmployeeScore(employeeScore);
        target.setManagerScore(managerScore);
        target.setActualScore(actualScore);
        target.setWeightedScore(weightedRating);
        try {
            targetService.saveTarget(target);
            return true;
        }catch (Exception e){
            return false;
        }
    }


    @Transactional
    public void deleteScore(Score score){
        scoreRepository.delete(score);
    }
}
