package hr.performancemanagement.service.impl.ScoreService;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.api.TargetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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
        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
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
        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
            if(score.getEvidence().length() > 0){
                existingScore.setEvidence(score.getEvidence());
            }
            if(score.getAttachmentName().length() > 0){
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
        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
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
        return scoreRepository.existsScoresByTargetAndReportingDate(score.getTarget(), score.getReportingDate());
    }

    @Override
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
    @Override
    public void deleteScore(Score score){
        scoreRepository.delete(score);
    }
}
