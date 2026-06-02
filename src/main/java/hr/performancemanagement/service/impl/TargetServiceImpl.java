package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.GoalRepository;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.repository.TargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class TargetServiceImpl implements hr.performancemanagement.service.api.TargetService {
    @Autowired
    TargetRepository targetRepository;
    @Autowired
    GoalRepository goalRepository;
    @Autowired
    ScoreRepository scoreRepository;
    @Autowired
    ReportingDateService reportingDateService;

    @Override
    public List<Target> getAllTargetsByScorecard(long scorecardId){
        List<Target> targetList = new ArrayList<>();
        List<Target> targets = targetRepository.findTargetsByScorecardId(scorecardId);
        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        for (Target target : targets) {
            if (target == null) {
                continue;
            }
            populateTargetHierarchy(target);
            hydrateScoreSnapshot(target, reportingDate);
            targetList.add(target);
        }

        return targetList;
    }

    @Override
    public List<Target> getAllTargetsByGoal(Goal goal){
        List<Target> targets = targetRepository.findTargetsByGoalId(goal.getId());
        return targets;
    }

    @Override
    public List<String> listAllUnits() {
        return targetRepository.listAllUnits();
    }

    @Override
    public Target getTargetById(long id){
        Target target = targetRepository.findTargetById(id);
        populateTargetHierarchy(target);
        return target;
    }

    @Override
    public boolean checkIfGoalHasTargets(Goal goal){
        return targetRepository.countTargetsByGoal(goal) > 0;
    }

    @Override
    public boolean checkIfOutputHasTargets(Output output) {
        return targetRepository.countTargetsByOutput(output) > 0;
    }

    @Override
    @Transactional
    public void deleteTargets(List<Target> targets) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        targetRepository.deleteAll(targets);
    }

//    public boolean  updateWeightedTargetScore(Target target){
//
//        double sumActual = targetRepository.totalWeightedScoreByTarget(target);
//        target.setWeightedScore(sumActual);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }
//
//    public boolean  updateActualTargetScore(Target target){
//
//        double avg = targetRepository.averageActualScoreByTarget(target);
//        target.setActualScore(avg);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }
//
//    public boolean  updateManagerTargetScore(Target target){
//
//        double avg = targetRepository.averageManagerScoreByTarget(target);
//        target.setManagerScore(avg);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }
//
//    public boolean  updateEmployeeTargetScore(Target target){
//
//        double avg = targetRepository.averageEmployeeScoreByTarget(target);
//        target.setEmployeeScore(avg);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }


    @Override
    @Transactional
    public Target saveTarget(Target target) {
        normalizePersistedScores(target);
        Target savedTarget = targetRepository.save(target);
        return savedTarget;
    }

    @Transactional
    @Override
    public void deleteTarget(Target target){
        targetRepository.delete(target);
    }

    private void normalizePersistedScores(Target target) {
        if (target == null) {
            return;
        }
        target.setEmployeeScore(clamp(target.getEmployeeScore(), 0.0, 5.0));
        target.setManagerScore(clamp(target.getManagerScore(), 0.0, 5.0));
        target.setAgreedScore(clamp(target.getAgreedScore(), 0.0, 5.0));
        target.setModeratedScore(clamp(target.getModeratedScore(), 0.0, 5.0));
        target.setCurrentEmployeeScore(clamp(target.getCurrentEmployeeScore(), 0.0, 5.0));
        target.setCurrentManagerScore(clamp(target.getCurrentManagerScore(), 0.0, 5.0));
        target.setCurrentAgreedScore(clamp(target.getCurrentAgreedScore(), 0.0, 5.0));
        target.setCurrentModeratedScore(clamp(target.getCurrentModeratedScore(), 0.0, 5.0));
        target.setWeightedScore(clamp(target.getWeightedScore(), 0.0, 100.0));
        target.setCurrentWeightedScore(clamp(target.getCurrentWeightedScore(), 0.0, 100.0));
    }

    private Double clamp(Double value, double minimum, double maximum) {
        if (value == null) {
            return null;
        }
        if (value < minimum) {
            return minimum;
        }
        if (value > maximum) {
            return maximum;
        }
        return value;
    }

    private void populateTargetHierarchy(Target target) {
        if (target == null) {
            return;
        }
        Goal goal = resolveGoalFromTarget(target);
        if (goal != null) {
            target.setGoal(goal);
            target.setPerspective(goal.getPerspective());
            target.setStrategicObjective(goal.getStrategicObjective());
        }
        if (target.getOutput() != null) {
            target.setOutcome(target.getOutput().getOutcome());
        }
    }

    private Goal resolveGoalFromTarget(Target target) {
        if (target == null) {
            return null;
        }
        if (target.getGoal() != null) {
            return target.getGoal();
        }
        if (target.getOutput() == null) {
            return null;
        }
        Outcome outcome = target.getOutput().getOutcome();
        if (outcome == null) {
            return null;
        }
        if (outcome.getGoal() != null) {
            return outcome.getGoal();
        }
        if (outcome.getPillar() != null) {
            return outcome.getPillar().getGoal();
        }
        return null;
    }

    private void hydrateScoreSnapshot(Target target, ReportingDate reportingDate) {
        if (target == null) {
            return;
        }

        Output output = target.getOutput();

        Object[] standardAggregates = scoreRepository.aggregateStandardTargetScores(target, output);
        double weightedScore = toDouble(aggregateValue(standardAggregates, 0));
        double averageActual = toDouble(aggregateValue(standardAggregates, 1));
        double sumActual = toDouble(aggregateValue(standardAggregates, 2));
        target.setWeightedScore(weightedScore);
        if ("%".equalsIgnoreCase(target.getUnit())) {
            target.setActual(averageActual);
        } else {
            target.setActual(sumActual);
        }

        Object[] valueBasedAggregates = scoreRepository.aggregateValueBasedTargetScores(target, output);
        target.setEmployeeScore(toDouble(aggregateValue(valueBasedAggregates, 1)));
        target.setManagerScore(toDouble(aggregateValue(valueBasedAggregates, 2)));
        target.setAgreedScore(toDouble(aggregateValue(valueBasedAggregates, 3)));
        target.setModeratedScore(toDouble(aggregateValue(valueBasedAggregates, 4)));

        Score currentScore = resolveCurrentScore(target, output, reportingDate);
        if (currentScore == null) {
            target.setCurrentActual(null);
            target.setCurrentEmployeeScore(null);
            target.setCurrentManagerScore(null);
            target.setCurrentAgreedScore(null);
            target.setCurrentModeratedScore(null);
            target.setCurrentWeightedScore(null);
            target.setCurrentEvidence(null);
            target.setCurrentAttachmentName(null);
            target.setCurrentJustification(null);
        } else {
            target.setCurrentActual(currentScore.getActual());
            target.setCurrentEmployeeScore(currentScore.getEmployeeScore());
            target.setCurrentManagerScore(currentScore.getManagerScore());
            target.setCurrentAgreedScore(currentScore.getAgreedScore());
            target.setCurrentModeratedScore(currentScore.getModeratedScore());
            target.setCurrentWeightedScore(currentScore.getWeightedScore());
            target.setCurrentEvidence(currentScore.getEvidence());
            target.setCurrentAttachmentName(currentScore.getAttachmentName());
            target.setCurrentJustification(currentScore.getJustification());
        }

        List<Score> history = resolveScoreHistory(target, output);
        target.setScores(history);
    }

    private Score resolveCurrentScore(Target target, Output output, ReportingDate reportingDate) {
        if (reportingDate == null) {
            return null;
        }

        if (target != null) {
            List<Score> targetScores = scoreRepository.findScoresByTargetAndReportingDateOrderByIdDesc(target, reportingDate);
            if (targetScores != null && !targetScores.isEmpty()) {
                return targetScores.get(0);
            }
        }

        if (output == null) {
            return null;
        }

        List<Score> outputScores = scoreRepository.findScoresByOutputAndReportingDateOrderByIdDesc(output, reportingDate);
        if (outputScores == null || outputScores.isEmpty()) {
            return null;
        }

        if (target != null) {
            for (Score score : outputScores) {
                if (score != null && score.getTarget() != null && score.getTarget().getId() == target.getId()) {
                    return score;
                }
            }
            for (Score score : outputScores) {
                if (score != null && score.getTarget() == null) {
                    return score;
                }
            }
        }
        return outputScores.get(0);
    }

    private List<Score> resolveScoreHistory(Target target, Output output) {
        if (target != null) {
            List<Score> targetHistory = scoreRepository.findScoresByTargetOrderByReportingDate_DateDescIdDesc(target);
            if (targetHistory != null && !targetHistory.isEmpty()) {
                return targetHistory;
            }
        }

        if (output == null) {
            return new ArrayList<Score>();
        }

        List<Score> outputHistory = scoreRepository.findScoresByOutputOrderByReportingDate_DateDescIdDesc(output);
        if (outputHistory == null) {
            return new ArrayList<Score>();
        }
        if (target != null) {
            List<Score> filteredHistory = new ArrayList<Score>();
            for (Score score : outputHistory) {
                if (score == null) {
                    continue;
                }
                if (score.getTarget() == null || score.getTarget().getId() == target.getId()) {
                    filteredHistory.add(score);
                }
            }
            if (!filteredHistory.isEmpty()) {
                return filteredHistory;
            }
        }
        return outputHistory;
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
}
