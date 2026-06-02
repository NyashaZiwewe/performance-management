package hr.performancemanagement.service.impl.ScoreService;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.entities.Output;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.api.TargetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.BiConsumer;


@Service
public class ValueBasedScoreServiceImpl implements hr.performancemanagement.service.api.ScoreService.ValueBasedScoreService {
    @Autowired
    ScoreRepository scoreRepository;
    @Autowired
    private TargetService targetService;

    @Override
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


    @Override
    public Score saveEmployeeScore(Score score) {

        Score savedScore;
        Output output = resolveOutput(score.getTarget());
        score.setOutput(output);
        Score existingScore = resolveExistingScore(score.getTarget(), output, score.getReportingDate());
        if(existingScore != null){
            existingScore.setEmployeeScore(score.getEmployeeScore());
//            existingScore.setEvidence(score.getEvidence());
            existingScore.setJustification(score.getJustification());
            savedScore = scoreRepository.save(existingScore);
            Target target = existingScore.getTarget();
            target.setCurrentEmployeeScore(savedScore.getEmployeeScore());
//            target.setCurrentEvidence(savedScore.getEvidence());
//            target.setCurrentAttachmentName(savedScore.getAttachmentName());
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

    @Override
    public Score saveEvidence(Score score) {

        Score savedScore;
        Output output = resolveOutput(score.getTarget());
        score.setOutput(output);
        Score existingScore = resolveExistingScore(score.getTarget(), output, score.getReportingDate());
        if(existingScore != null){
            if(score.getEvidence() != null && !score.getEvidence().trim().isEmpty()){
                existingScore.setEvidence(score.getEvidence());
            }
            if(score.getAttachmentName() != null && !score.getAttachmentName().trim().isEmpty()){
                existingScore.setAttachmentName(score.getAttachmentName());
            }
            savedScore = scoreRepository.save(existingScore);
            Target target = existingScore.getTarget();
            target.setCurrentEvidence(savedScore.getEvidence());
            target.setCurrentAttachmentName(savedScore.getAttachmentName());
            updateTargetData(target);

        }else{
            savedScore = scoreRepository.save(score);
            Target target = savedScore.getTarget();
            target.setCurrentEvidence(score.getEvidence());
            target.setCurrentAttachmentName(score.getAttachmentName());
            updateTargetData(target);
        }
        return savedScore;
    }

    @Override
    public Score saveManagerScore(Score score) {
        return updateExistingScore(
                score,
                (existingScore, incomingScore) -> existingScore.setManagerScore(incomingScore.getManagerScore()),
                (target, savedScore) -> target.setCurrentManagerScore(savedScore.getManagerScore())
        );
    }

    @Override
    public Score saveAgreedScore(Score score) {
        return updateExistingScore(
                score,
                (existingScore, incomingScore) -> existingScore.setAgreedScore(incomingScore.getAgreedScore()),
                (target, savedScore) -> target.setCurrentAgreedScore(savedScore.getAgreedScore())
        );
    }

    @Override
    public Score saveModeratedScore(Score score) {
        return updateExistingScore(
                score,
                (existingScore, incomingScore) -> {
                    existingScore.setModeratedScore(incomingScore.getModeratedScore());
                    existingScore.setWeightedScore(calculateWeightedScore(existingScore));
                },
                (target, savedScore) -> {
                    target.setCurrentModeratedScore(savedScore.getModeratedScore());
                    target.setCurrentWeightedScore(savedScore.getWeightedScore());
                }
        );
    }

    private Score updateExistingScore(Score score, BiConsumer<Score, Score> scoreUpdater, BiConsumer<Target, Score> targetUpdater) {
        Output output = resolveOutput(score.getTarget());
        score.setOutput(output);
        Score existingScore = resolveExistingScore(score.getTarget(), output, score.getReportingDate());
        if(existingScore != null){
            scoreUpdater.accept(existingScore, score);
            Score savedScore = scoreRepository.save(existingScore);
            Target target = savedScore.getTarget();
            targetUpdater.accept(target, savedScore);
            updateTargetData(target);
            return savedScore;
        }
        return score;
    }

    @Override
    public boolean scoreExists(Score score){
        Output output = resolveOutput(score.getTarget());
        return resolveExistingScore(score.getTarget(), output, score.getReportingDate()) != null;
    }

    @Override
    public boolean  updateTargetData(Target target){

        Object[] aggregates = scoreRepository.aggregateValueBasedTargetScores(target, resolveOutput(target));
        Double weightedRating = toDouble(aggregateValue(aggregates, 0));
        Double employeeScore = toDouble(aggregateValue(aggregates, 1));
        Double managerScore = toDouble(aggregateValue(aggregates, 2));
        Double agreedScore = toDouble(aggregateValue(aggregates, 3));
        Double moderatedScore = toDouble(aggregateValue(aggregates, 4));

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
