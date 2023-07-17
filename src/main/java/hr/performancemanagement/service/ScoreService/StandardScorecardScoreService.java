package hr.performancemanagement.service.ScoreService;
import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Target;
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
        double weightedRating = 0;

        if(baseTarget != stretchTarget){
            weightedRating = (actual-baseTarget)/(stretchTarget-baseTarget) * allocatedWeight;
        }else{
            weightedRating = (actual-baseTarget) * allocatedWeight;
        }
        if(weightedRating > allocatedWeight){
            weightedRating = allocatedWeight;
        }
        if(weightedRating < -allocatedWeight){
            weightedRating = -allocatedWeight;
        }

        return weightedRating;
    }

    public Score saveScore(Score score) {

            boolean exists = scoreExists(score);
            if(exists){
                Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
                score.setId(existingScore.getId());
            }
            score.setWeightedScore(calculateWeightedScore(score));
            Score savedScore = scoreRepository.save(score);
            updateTargetData(score.getTarget());
            return savedScore;
    }

    public boolean scoreExists(Score score){
        return scoreRepository.existsScoresByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
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
