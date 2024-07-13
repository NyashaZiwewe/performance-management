package hr.performancemanagement.service.ScoreService;
import hr.performancemanagement.entities.Output;
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

//        Output output = score.getOutput();
//        double actual = score.getActual();
//        double baseTarget = target.getBaseTarget();
//        double stretchTarget = target.getStretchTarget();
//        double allocatedWeight = target.getAllocatedWeight();
//        double weightedRating = 0;
//
//        if(baseTarget != stretchTarget){
//            weightedRating = (actual-baseTarget)/(stretchTarget-baseTarget) * allocatedWeight;
//        }else{
//            weightedRating = (actual-baseTarget) * allocatedWeight;
//        }
//        if(weightedRating > allocatedWeight){
//            weightedRating = allocatedWeight;
//        }
//        if(weightedRating < -allocatedWeight){
//            weightedRating = -allocatedWeight;
//        }
//
//        return weightedRating;
        return 0.0;
    }

    public Score saveScore(Score score) {

            boolean exists = scoreExists(score);
            if(exists){
                Score existingScore = scoreRepository.findScoreByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
                score.setId(existingScore.getId());
            }
//            score.setWeightedScore(calculateWeightedScore(score));
            Score savedScore = scoreRepository.save(score);
            updateOutputData(score.getOutput());
            return savedScore;
    }

    public boolean scoreExists(Score score){
        return scoreRepository.existsScoresByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
    }

    public boolean  updateOutputData(Output output){

//        double weightedRating = scoreRepository.totalWeightedScoreByOutput(output);
//        String unit = target.getUnit();
//        double actual;
//        if("%".equalsIgnoreCase(unit)){
//            actual = scoreRepository.averageActualByOutput(output);
//        }else {
//            actual = scoreRepository.sumActualByOutput(output);
//        }
//        output.setActual(actual);
//        output.setWeightedScore(weightedRating);
//        try {
//            targetService.saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
        return true;
    }


    @Transactional
    public void deleteScore(Score score){
        scoreRepository.delete(score);
    }
}
