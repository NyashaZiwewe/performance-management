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
        double moderatedScore = score.getModeratedScore();
        double allocatedWeight = target.getAllocatedWeight();
        double weightedRating = 0;

        try {
            weightedRating = (moderatedScore/5) * allocatedWeight;
        }catch (Exception ignored){

        }

        return weightedRating;
    }


    public Score saveEmployeeScore(Score score) {

        Score savedScore;
        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
            existingScore.setEmployeeScore(score.getEmployeeScore());
            existingScore.setEvidence(score.getEvidence());
            existingScore.setJustification(score.getJustification());
            savedScore = scoreRepository.save(existingScore);

        }else{
            savedScore = scoreRepository.save(score);
        }
        updateTargetData(savedScore.getTarget());
        return savedScore;
    }

    public Score saveManagerScore(Score score) {

        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
            existingScore.setManagerScore(score.getManagerScore());
            score = scoreRepository.save(existingScore);
            updateTargetData(score.getTarget());
        }
        return score;
    }

    public Score saveAgreedScore(Score score) {

        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
            existingScore.setAgreedScore(score.getAgreedScore());
            score = scoreRepository.save(existingScore);
            updateTargetData(score.getTarget());
        }
        return score;
    }

    public Score saveModeratedScore(Score score) {


        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
            existingScore.setModeratedScore(score.getModeratedScore());
            existingScore.setWeightedScore(calculateWeightedScore(existingScore));
            score = scoreRepository.save(existingScore);
            updateTargetData(score.getTarget());
        }

        return score;
    }

//    public Score saveScore(Score score) {
//
//        boolean exists = scoreExists(score);
//        Score savedScore;
//        if(exists){
//
//            Target target = targetService.getTargetById(score.getTarget().getId());
//            Scorecard scorecard = scorecardService.getScorecardById(target.getGoal().getScorecardId());
//            Account loggedUser = (Account) session.getAttribute("loggedUser");
//
//            long loggedUserId = loggedUser.getId();
//            long ownerId = scorecard.getOwner().getId();
//            long supervisorId = scorecard.getOwner().getSupervisor().getId();
////            String role = loggedUser.getRole();
//            String approvalStatus = scorecard.getApprovalStatus();
//            boolean isOwner = false;
//            boolean isSupervisor = false;
//            boolean isModerator = false;
//
//            if (loggedUserId == ownerId) {
//                isOwner = true;
//            } else if (loggedUserId == supervisorId && "SCORED_BY_EMPLOYEE".equals(approvalStatus)) {
//                isSupervisor = true;
//            } else if (loggedUserId == supervisorId && "SCORED_BY_SUPERVISOR".equals(approvalStatus)) {
//                isModerator = true;
//            }else{
//                System.out.println("User can not capture scores");
//            }
//
//            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
//            //score.setId(existingScore.getId());
//            if(isOwner){
//                existingScore.setEmployeeScore(score.getEmployeeScore());
//                existingScore.setEvidence(score.getEvidence());
//                existingScore.setJustification(score.getJustification());
//            }else if(isSupervisor){
//                existingScore.setManagerScore(score.getManagerScore());
//            }else if(isModerator){
//                existingScore.setModeratedScore(score.getModeratedScore());
//            }
//            existingScore.setWeightedScore(calculateWeightedScore(existingScore));
//            savedScore = scoreRepository.save(existingScore);
//        }else{
//            score.setWeightedScore(calculateWeightedScore(score));
//            savedScore = scoreRepository.save(score);
//        }
//
//        updateTargetData(savedScore.getTarget());
//        return savedScore;
//    }

    public boolean scoreExists(Score score){
        return scoreRepository.existsScoresByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
    }

    public boolean  updateTargetData(Target target){

        Double weightedRating = scoreRepository.totalWeightedScoreByTarget(target);
        Double employeeScore = scoreRepository.averageEmployeeScoreByTarget(target);
        Double managerScore = scoreRepository.averageManagerScoreByTarget(target);
        Double agreedScore = scoreRepository.averageAgreedScoreByTarget(target);
        Double moderatedScore = scoreRepository.averageModeratedScoreByTarget(target);

        target.setEmployeeScore(employeeScore);
        target.setManagerScore(managerScore);
        target.setAgreedScore(agreedScore);
        target.setModeratedScore(moderatedScore);
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
