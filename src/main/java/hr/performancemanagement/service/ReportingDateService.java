package hr.performancemanagement.service;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.repository.ScoreCardRepository;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ReportingDateService {
    @Autowired
    ReportingDateRepository reportingDateRepository;
    @Autowired
    CommonService cs;
    @Autowired
    ScoreCardRepository scoreCardRepository;
    @Autowired
    private ScoreRepository scoreRepository;

    public ReportingDate getReportingDateById(long id){

        ReportingDate reportingDate = reportingDateRepository.findReportingDateById(id);
        return reportingDate;
    }

    public ReportingDate getActiveReportingDate(){

        Account loggedUser = cs.getLoggedUser();
        ReportingDate reportingDate = reportingDateRepository.findActiveReportingDate(PMConstants.STATUS_ACTIVE,PMConstants.STATUS_ACTIVE, loggedUser.getClientId());
        return reportingDate;
    }

    public List<ReportingDate> listAllReportingDates(ReportingPeriod reportingPeriod){
        List<ReportingDate> reportingDateList = new ArrayList<>();
        reportingDateRepository.findReportingDatesByReportingPeriod(reportingPeriod).forEach(reportingDate -> reportingDateList.add(reportingDate));
        return reportingDateList;
    }

    private void deactivateReportingDates(ReportingPeriod reportingPeriod){
        List<ReportingDate> reportingDateList = reportingDateRepository.findReportingDatesByReportingPeriod(reportingPeriod);
        for(ReportingDate reportingDate: reportingDateList){
            reportingDate.setStatus(PMConstants.STATUS_IN_ACTIVE);
            resetScorecards(reportingDate);
            reportingDateRepository.save(reportingDate);
        }
    }

    public void saveReportingDate(ReportingDate reportingDate) {
        String status = reportingDate.getStatus();
        if(PMConstants.STATUS_ACTIVE.equalsIgnoreCase(status)){
            deactivateReportingDates(reportingDate.getReportingPeriod());
            resetCurrentScores(reportingDate);
        }
        reportingDateRepository.save(reportingDate);
    }

    @Transactional
    public void deleteReportingDate(long id){
        ReportingDate reportingDate = reportingDateRepository.findReportingDateById(id);
        reportingDateRepository.delete(reportingDate);
    }

    private void resetScorecards(ReportingDate reportingDate){
       List<Scorecard>  scorecards = reportingDateRepository.findScorecardsByReportingPeriod(reportingDate.getReportingPeriod());
       for(Scorecard scorecard : scorecards){
           switch (scorecard.getApprovalStatus()){

               case PMConstants.APPROVAL_STATUS_SCORED_BY_EMPLOYEE:
               case PMConstants.APPROVAL_STATUS_SCORED_BY_SUPERVISOR:
               case PMConstants.APPROVAL_STATUS_AGREED_BY_TWO:
               case PMConstants.APPROVAL_STATUS_MODERATED_BY_HR:
                   scorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_APPROVED_BY_HR);
                   break;
               default:
                  break;
           }
           scorecard.setLockStatus(PMConstants.LOCK_STATUS_LOCKED);
           System.out.println("in reset scorecard: "+ scorecard.getOwner().getFullName());
           scoreCardRepository.save(scorecard);
       }
    }

    private void resetCurrentScores(ReportingDate reportingDate){
        List<Scorecard>  scorecards = reportingDateRepository.findScorecardsByReportingPeriod(reportingDate.getReportingPeriod());
        for(Scorecard scorecard : scorecards){
            for(Output output: Objects.requireNonNull(scoreCardRepository.findOutputsFromScorecard(scorecard))){
                List<Score> scores = scoreRepository.getOutputScoresByReportingDate(output,reportingDate);
                for(Score score: scores){
                    if(score != null){
                        output.setCurrentEmployeeScore(score.getEmployeeScore());
                        output.setCurrentManagerScore(score.getManagerScore());
                        output.setCurrentAgreedScore(score.getAgreedScore());
                        output.setCurrentModeratedScore(score.getModeratedScore());
                        output.setCurrentActual(score.getActual());

                        for(Target target: output.getTargets()){
                            target.setCurrentEvidence(score.getEvidence());
                            target.setCurrentJustification(score.getJustification());
                            target.setCurrentAttachmentName(score.getAttachmentName());
                        }
                    }else{
                        output.setCurrentEmployeeScore(null);
                        output.setCurrentManagerScore(null);
                        output.setCurrentAgreedScore(null);
                        output.setCurrentModeratedScore(null);
                        output.setCurrentActual(null);

                        for(Target target: output.getTargets()){
                            target.setCurrentEvidence(null);
                            target.setCurrentJustification(null);
                            target.setCurrentAttachmentName(null);
                        }
                    }
                }

            }
            System.out.println("in reset scorecard current scores: "+ scorecard.getOwner().getFullName());
            scoreCardRepository.save(scorecard);
        }
    }
}
