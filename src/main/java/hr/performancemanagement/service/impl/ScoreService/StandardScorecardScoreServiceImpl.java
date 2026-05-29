package hr.performancemanagement.service.impl.ScoreService;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.api.TargetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;


@Service
public class StandardScorecardScoreServiceImpl implements hr.performancemanagement.service.api.ScoreService.StandardScorecardScoreService {
    @Autowired
    ScoreRepository scoreRepository;
    @Autowired
    private TargetService targetService;

    @Override
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

    @Override
    public Score saveScore(Score score) {

            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
            if(existingScore != null){
                score.setId(existingScore.getId());
            }
            score.setWeightedScore(calculateWeightedScore(score));
            Score savedScore = scoreRepository.save(score);
            updateTargetData(score.getTarget());
            return savedScore;
    }

    @Override
    public boolean scoreExists(Score score){
        return scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate()) != null;
    }

    @Override
    public boolean  updateTargetData(Target target){

        Object[] aggregates = scoreRepository.aggregateStandardTargetScores(target);
        double weightedRating = toDouble(aggregates[0]);
        String unit = target.getUnit();
        double actual;
        if("%".equalsIgnoreCase(unit)){
            actual = toDouble(aggregates[1]);
        }else {
            actual = toDouble(aggregates[2]);
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
    @Override
    public void deleteScore(Score score){
        scoreRepository.delete(score);
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
