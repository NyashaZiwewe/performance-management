package hr.performancemanagement.service.ScoreService;
import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.EvidenceService;
import hr.performancemanagement.service.OutputService;
import hr.performancemanagement.service.OverallScoreService;
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
    private OverallScoreService overallScoreService;
    @Autowired
    HttpSession session;
    @Autowired
    private EvidenceService evidenceService;

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


    public OverallScore saveEmployeeScore(Score score, Target target) {

        Score savedScore;
        Output output = score.getOutput();
        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
            existingScore.setEmployeeScore(score.getEmployeeScore());
//            existingScore.setJustification(score.getJustification());
              savedScore = scoreRepository.save(existingScore);
//            Output output = existingScore.getOutput();
//            output.setCurrentEmployeeScore(savedScore.getEmployeeScore());
//            target.setCurrentJustification(savedScore.getJustification());
//            targetService.saveTarget(target);
//            updateOutputData(output);
        }else{
            savedScore = scoreRepository.save(score);
//            Output output = savedScore.getOutput();
            output.setCurrentEmployeeScore(score.getEmployeeScore());
//            updateOutputData(output);
        }
        createEvidence(score,target);
        return calculateOverallScore(output.getScorecard(), score.getReportingDate(), "EMPLOYEE_SCORE");
    }

    private void createEvidence(Score score, Target target) {

        Evidence evidence = new Evidence();
        evidence.setEvidence(score.getEvidence());
        evidence.setTarget(target);
        evidence.setJustification(score.getJustification());
        evidence.setAttachmentName(score.getAttachmentName());
        evidence.setReportingDate(score.getReportingDate());
        evidenceService.saveEvidence(evidence);
    }

//    public void saveEvidence(Score score, Target target) {
//        Score savedScore;
//        if(scoreExists(score)){
//            Score existingScore = scoreRepository.findScoreByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
//            if(!score.getEvidence().isEmpty()){
//                existingScore.setEvidence(score.getEvidence());
//            }
//            if(!score.getAttachmentName().isEmpty()){
//                existingScore.setAttachmentName(score.getAttachmentName());
//            }
//            savedScore = scoreRepository.save(existingScore);
//            target.setCurrentEvidence(savedScore.getEvidence());
//            target.setCurrentAttachmentName(savedScore.getAttachmentName());
//            targetService.saveTarget(target);
//
//        }else{
//            savedScore = scoreRepository.save(score);
//            target.setCurrentEvidence(score.getEvidence());
//            target.setCurrentAttachmentName(score.getAttachmentName());
//            targetService.saveTarget(target);
//        }
//    }

    public OverallScore saveManagerScore(Score score) {
        Score savedScore;
        Output output = score.getOutput();
        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
            existingScore.setManagerScore(score.getManagerScore());
            score = scoreRepository.save(existingScore);
            output.setCurrentManagerScore(score.getManagerScore());
        }else {
            score.setEmployeeScore(1);
           score = scoreRepository.save(score);
        }
        return calculateOverallScore(output.getScorecard(), score.getReportingDate(), "MANAGER_SCORE");

    }

    public OverallScore saveAgreedScore(Score score) {

        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
            existingScore.setAgreedScore(score.getAgreedScore());
            score = scoreRepository.save(existingScore);
            Output output = score.getOutput();
            output.setCurrentAgreedScore(score.getAgreedScore());
        }else {
            score = scoreRepository.save(score);
        }
        return calculateOverallScore(score.getOutput().getScorecard(), score.getReportingDate(), "AGREED_SCORE");
    }

    public Score saveModeratedScore(Score score) {


        if(scoreExists(score)){
            Score existingScore = scoreRepository.findScoreByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
            existingScore.setModeratedScore(score.getModeratedScore());
            existingScore.setWeightedScore(calculateWeightedScore(existingScore));
            score = scoreRepository.save(existingScore);
        }
        return score;
    }

    public boolean scoreExists(Score score){
        return scoreRepository.existsScoresByOutputAndReportingDate(score.getOutput(), score.getReportingDate());
    }

//    public void updateOutputData(Output output){
//
//        Double weightedRating = scoreRepository.totalWeightedScoreByOutput(output);
//        Double employeeScore = scoreRepository.averageEmployeeScoreByOutput(output);
//        Double managerScore = scoreRepository.averageManagerScoreByOutput(output);
//        Double agreedScore = scoreRepository.averageAgreedScoreByOutput(output);
//        Double moderatedScore = scoreRepository.averageModeratedScoreByOutput(output);
//
//        output.setEmployeeScore(employeeScore);
//        output.setManagerScore(managerScore);
//        output.setAgreedScore(agreedScore);
//        output.setModeratedScore(moderatedScore);
//        output.setWeightedScore(weightedRating);
//        try {
//            outputService.saveOutput(output);
//        }catch (Exception e){
//        }
//    }

    public OverallScore calculateOverallScore(Scorecard scorecard, ReportingDate reportingDate, String scoreType){
        OverallScore overallScore =  new OverallScore();
        try {
            overallScore = overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, reportingDate);
            if (overallScore == null) {
                overallScore = new OverallScore();
                overallScore.setScorecard(scorecard);
                overallScore.setReportingDate(reportingDate);
            }
            if ("EMPLOYEE_SCORE".equalsIgnoreCase(scoreType)) {
                overallScore.setEmployeeOverall(scoreRepository.weightedEmployeeScore(scorecard, reportingDate));
            } else if ("MANAGER_SCORE".equalsIgnoreCase(scoreType)) {
                overallScore.setManagerOverall(scoreRepository.weightedManagerScore(scorecard, reportingDate));
            }
            if ("AGREED_SCORE".equalsIgnoreCase(scoreType)) {
                overallScore.setAgreedOverall(scoreRepository.weightedAgreedScore(scorecard, reportingDate));
            }

           overallScore = overallScoreService.saveOverallScore(overallScore);
        }catch (Exception e){
            e.printStackTrace();
        }
      return overallScore;
    }


    @Transactional
    public void deleteScore(Score score){
        scoreRepository.delete(score);
    }
}
