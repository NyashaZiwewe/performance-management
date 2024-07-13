package hr.performancemanagement.service.ScoreService;
import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.OutputService;
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
    private OutputService outputService;
    @Autowired
    private TargetService targetService;
    @Autowired
    HttpSession session;

    public double calculateWeightedScore(Score score){

        Output output = score.getOutput();
        double moderatedScore = score.getModeratedScore();
        double allocatedWeight = output.getAllocatedWeight();
        double weightedRating = 0;

        try {
            weightedRating = (moderatedScore/5) * allocatedWeight;
        }catch (Exception ignored){

        }

        return weightedRating;
    }


    public Score saveEmployeeScore(Score score, Target target) {

        Score savedScore;
        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
            existingScore.setEmployeeScore(score.getEmployeeScore());
//            existingScore.setEvidence(score.getEvidence());
            existingScore.setJustification(score.getJustification());
            savedScore = scoreRepository.save(existingScore);
            Output output = existingScore.getOutput();
            output.setCurrentEmployeeScore(savedScore.getEmployeeScore());
//            target.setCurrentEvidence(savedScore.getEvidence());
//            target.setCurrentAttachmentName(savedScore.getAttachmentName());
            target.setCurrentJustification(savedScore.getJustification());
            targetService.saveTarget(target);
            updateOutputData(output);
        }else{
            savedScore = scoreRepository.save(score);
            Output output = savedScore.getOutput();
            output.setCurrentEmployeeScore(score.getEmployeeScore());
            updateOutputData(output);
        }
        return savedScore;
    }

    public Score saveEvidence(Score score, Target target) {

        Score savedScore;
        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
            if(score.getEvidence().length() > 0){
                existingScore.setEvidence(score.getEvidence());
            }
            if(score.getAttachmentName().length() > 0){
                existingScore.setAttachmentName(score.getAttachmentName());
            }
            savedScore = scoreRepository.save(existingScore);
            target.setCurrentEvidence(savedScore.getEvidence());
            target.setCurrentAttachmentName(savedScore.getAttachmentName());
            targetService.saveTarget(target);

        }else{
            savedScore = scoreRepository.save(score);
            target.setCurrentEvidence(score.getEvidence());
            target.setCurrentAttachmentName(score.getAttachmentName());
            targetService.saveTarget(target);
        }
        return savedScore;
    }

    public Score saveManagerScore(Score score) {

        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
            existingScore.setManagerScore(score.getManagerScore());
            score = scoreRepository.save(existingScore);
            Output output = score.getOutput();
            output.setCurrentManagerScore(score.getManagerScore());
            updateOutputData(output);
        }
        return score;
    }

    public Score saveAgreedScore(Score score) {

        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
            existingScore.setAgreedScore(score.getAgreedScore());
            score = scoreRepository.save(existingScore);
            Output output = score.getOutput();
            output.setCurrentAgreedScore(score.getAgreedScore());
            updateOutputData(output);
        }
        return score;
    }

    public Score saveModeratedScore(Score score) {


        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
            existingScore.setModeratedScore(score.getModeratedScore());
            existingScore.setWeightedScore(calculateWeightedScore(existingScore));
            score = scoreRepository.save(existingScore);
            Output output = score.getOutput();
            output.setCurrentModeratedScore(score.getModeratedScore());
            output.setCurrentWeightedScore(score.getWeightedScore());
            updateOutputData(output);
        }
        return score;
    }

    public boolean scoreExists(Score score){
        return scoreRepository.existsScoresByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
    }

    public boolean  updateOutputData(Output output){

        Double weightedRating = scoreRepository.totalWeightedScoreByOutput(output);
        Double employeeScore = scoreRepository.averageEmployeeScoreByOutput(output);
        Double managerScore = scoreRepository.averageManagerScoreByOutput(output);
        Double agreedScore = scoreRepository.averageAgreedScoreByOutput(output);
        Double moderatedScore = scoreRepository.averageModeratedScoreByOutput(output);

        output.setEmployeeScore(employeeScore);
        output.setManagerScore(managerScore);
        output.setAgreedScore(agreedScore);
        output.setModeratedScore(moderatedScore);
        output.setWeightedScore(weightedRating);
        try {
            outputService.saveOutput(output);
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
