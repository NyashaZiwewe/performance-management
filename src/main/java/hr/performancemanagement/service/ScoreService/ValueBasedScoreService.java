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
            Target target = existingScore.getTarget();
            target.setCurrentEmployeeScore(savedScore.getEmployeeScore());
            target.setCurrentEvidence(savedScore.getEvidence());
            target.setCurrentJustification(savedScore.getJustification());
            updateTargetData(target);

        }else{
            savedScore = scoreRepository.save(score);
            Target target = savedScore.getTarget();
            target.setCurrentEmployeeScore(score.getEmployeeScore());
            updateTargetData(target);
        }
        return savedScore;
    }

    public Score saveManagerScore(Score score) {

        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
            existingScore.setManagerScore(score.getManagerScore());
            score = scoreRepository.save(existingScore);
            Target target = score.getTarget();
            target.setCurrentManagerScore(score.getManagerScore());
            updateTargetData(target);
        }
        return score;
    }

    public Score saveAgreedScore(Score score) {

        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
            existingScore.setAgreedScore(score.getAgreedScore());
            score = scoreRepository.save(existingScore);
            Target target = score.getTarget();
            target.setCurrentAgreedScore(score.getAgreedScore());
            updateTargetData(target);
        }
        return score;
    }

    public Score saveModeratedScore(Score score) {


        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
            existingScore.setModeratedScore(score.getModeratedScore());
            existingScore.setWeightedScore(calculateWeightedScore(existingScore));
            score = scoreRepository.save(existingScore);
            Target target = score.getTarget();
            target.setCurrentModeratedScore(score.getModeratedScore());
            target.setCurrentWeightedScore(score.getWeightedScore());
            updateTargetData(target);
        }
        return score;
    }

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
