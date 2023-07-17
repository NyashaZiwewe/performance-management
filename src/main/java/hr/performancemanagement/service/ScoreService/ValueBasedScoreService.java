package hr.performancemanagement.service.ScoreService;
import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.TargetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValueBasedScoreService {
    @Autowired
    ScoreRepository scoreRepository;
    @Autowired
    private TargetService targetService;

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
