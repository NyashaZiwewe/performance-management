package hr.performancemanagement.service.impl.ScoreService;

import org.springframework.stereotype.Service;
import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.entities.Output;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.repository.ScoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class StandardScorecardScoreServiceImpl implements hr.performancemanagement.service.api.ScoreService.StandardScorecardScoreService {
    @Autowired
    ScoreRepository scoreRepository;

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

            Output output = resolveOutput(score.getTarget());
            score.setOutput(output);
            Score existingScore = resolveExistingScore(score.getTarget(), output, score.getReportingDate());
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
        Output output = resolveOutput(score.getTarget());
        return resolveExistingScore(score.getTarget(), output, score.getReportingDate()) != null;
    }

    @Override
    public boolean  updateTargetData(Target target){

        Object[] aggregates = scoreRepository.aggregateStandardTargetScores(target, resolveOutput(target));
        double weightedRating = toDouble(aggregateValue(aggregates, 0));
        String unit = target.getUnit();
        double actual;
        if("%".equalsIgnoreCase(unit)){
            actual = toDouble(aggregateValue(aggregates, 1));
        }else {
            actual = toDouble(aggregateValue(aggregates, 2));
        }
        target.setActual(actual);
        target.setWeightedScore(weightedRating);
        return true;
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

    private Object aggregateValue(Object[] aggregates, int index) {
        Object[] row = unwrapAggregateRow(aggregates);
        if (row == null || index < 0 || index >= row.length) {
            return null;
        }
        return row[index];
    }

    private Object[] unwrapAggregateRow(Object[] aggregates) {
        if (aggregates == null) {
            return null;
        }
        if (aggregates.length == 1 && aggregates[0] instanceof Object[]) {
            return (Object[]) aggregates[0];
        }
        return aggregates;
    }

    private Output resolveOutput(Target target) {
        return target == null ? null : target.getOutput();
    }

    private Score resolveExistingScore(Target target, Output output, ReportingDate reportingDate) {
        if (reportingDate == null) {
            return null;
        }

        if (target != null) {
            List<Score> byTarget = scoreRepository.findScoresByTargetAndReportingDateOrderByIdDesc(target, reportingDate);
            if (byTarget != null && !byTarget.isEmpty()) {
                return byTarget.get(0);
            }
        }

        if (output == null) {
            return null;
        }

        List<Score> byOutput = scoreRepository.findScoresByOutputAndReportingDateOrderByIdDesc(output, reportingDate);
        if (byOutput == null || byOutput.isEmpty()) {
            return null;
        }

        if (target != null) {
            for (Score candidate : byOutput) {
                if (candidate != null && candidate.getTarget() != null && candidate.getTarget().getId() == target.getId()) {
                    return candidate;
                }
            }
            for (Score candidate : byOutput) {
                if (candidate != null && candidate.getTarget() == null) {
                    return candidate;
                }
            }
        }

        return byOutput.get(0);
    }
}
