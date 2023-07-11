package hr.performancemanagement.service.ScoreService;

import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.repository.GoalRepository;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.TargetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StandardScorecardScoreService {
    @Autowired
    ScoreRepository scoreRepository;
    @Autowired
    private TargetService targetService;

    public double calculateWeightedScore(Score score){

        Target target = score.getTarget();
        double actual = score.getActual();
        double baseTarget = target.getBaseTarget();
        double stretchTarget = target.getStretchTarget();
        double allocatedWeight = target.getAllocatedWeight();

        double weightedRating = (actual-baseTarget)/(stretchTarget-baseTarget) * allocatedWeight;
        return weightedRating;
    }

    public boolean saveScore(Score score) {

        if(!scoreRepository.existsScoresByTargetAndReportingDate(score.getTarget(), score.getReportingDate())){
            score.setWeightedScore(calculateWeightedScore(score));
            scoreRepository.save(score);
            updateTargetData(score.getTarget());
            return false;
        }else {
            return true;
        }

    }

    public boolean  updateTargetData(Target target){

        double weightedRating = scoreRepository.totalWeightedScoreByTarget(target);
        String unit = target.getUnit();
        double actual;
        if("%".equalsIgnoreCase(unit)){
            actual = scoreRepository.averageActualByTarget(target);
        }else {
            actual = scoreRepository.sumActualByTarget(target);
        }
        target.setActual(actual);
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
