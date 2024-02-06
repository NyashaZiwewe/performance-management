package hr.performancemanagement.service;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.ScoreCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScorecardService {

    @Autowired
    ScoreCardRepository scoreCardRepository;

    @Autowired
    AccountService accountService;
    @Autowired
    CommonService cs;
    @Autowired
    HttpSession session;

    @Autowired
    GoalService goalService;
    @Autowired
    private final ReportingPeriodService reportingPeriodService;

    public ScorecardService(ReportingPeriodService reportingPeriodService) {
        this.reportingPeriodService = reportingPeriodService;
    }

    public List<Scorecard> listAllScorecards(long clientId){
        List<Scorecard> scorecardList = new ArrayList<>();
        scoreCardRepository.findScorecardsByClientId(clientId).forEach(scorecard -> scorecardList.add(scorecard));
        return scorecardList;
    }

    public List<Scorecard> getScorecardsByOwner(Account owner){
        List<Scorecard> scorecardList = new ArrayList<>();
        scoreCardRepository.findScorecardsByOwner(owner).forEach(scorecard -> scorecardList.add(scorecard));
        return scorecardList;
    }


    public List<Scorecard> getScorecardsByReportingPeriodId(ReportingPeriod reportingPeriod){

        List<Scorecard> scorecardList = new ArrayList<>();
        Account loggedUser = cs.getLoggedUser();

        if(cs.isAdmin() || cs.hasSpecialRights()){
            scoreCardRepository.findScorecardsByReportingPeriodAndClientId(reportingPeriod, loggedUser.getClientId()).forEach(scorecard -> scorecardList.add(scorecard));
        }
        else if(loggedUser.getAccountType().equalsIgnoreCase("Employee")){

            scoreCardRepository.findScorecardsByReportingPeriodAndOwner(reportingPeriod, loggedUser).forEach(scorecard -> scorecardList.add(scorecard));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("Supervisor")){

            scoreCardRepository.findScorecardsByReportingPeriodAndOwner(reportingPeriod, loggedUser).forEach(scorecard -> scorecardList.add(scorecard));
            scoreCardRepository.findScorecardsByReportingPeriodAndOwner_Supervisor(reportingPeriod, loggedUser).forEach(scorecard -> scorecardList.add(scorecard));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("DEPARTMENT_MANAGER") || loggedUser.getAccountType().equalsIgnoreCase("DIVISIONAL_DIRECTOR")){

            scoreCardRepository.findScorecardsByReportingPeriodAndOwner_Department(reportingPeriod, loggedUser.getDepartment());

        }else if(loggedUser.getAccountType().equalsIgnoreCase("ACTING_CEO") || loggedUser.getAccountType().equalsIgnoreCase("CEO") ){

            scoreCardRepository.findScorecardsByReportingPeriodAndClientId(reportingPeriod, loggedUser.getClientId()).forEach(scorecard -> scorecardList.add(scorecard));

        }else{

        }
        return scorecardList;
    }

    public List<Scorecard> getScoresByPeriodId(ReportingPeriod reportingPeriod){

        List<Scorecard> scorecardList = getScorecardsByReportingPeriodId(reportingPeriod);

        for(Scorecard scorecard : scorecardList){
            scorecard.setEmployeeScore(scoreCardRepository.findAverageEmployeeScore(scorecard.getId()));
            scorecard.setManagerScore(scoreCardRepository.findAverageManagerScore(scorecard.getId()));
            scorecard.setAgreedScore(scoreCardRepository.findAverageAgreedScore(scorecard.getId()));
            scorecard.setModeratedScore(scoreCardRepository.findAverageModeratedScore(scorecard.getId()));
            scorecard.setWeightedScore(scoreCardRepository.findAverageWeightedScore(scorecard.getId()));
        }

        return scorecardList;
    }

    public Double getScoresByReportingDateAndScorecardId(ReportingDate date, Scorecard scorecard){

        Double weightedScore = scoreCardRepository.findAverageWeightedScoreByReportingDate(date, scorecard.getId());
        return weightedScore;
    }

    public int countPassedScorecardsByPeriodId(ReportingPeriod reportingPeriod){

        List<Scorecard> scorecardList = getScorecardsByReportingPeriodId(reportingPeriod);
        int passedScorecards = 0;

        for(Scorecard scorecard : scorecardList){
            double moderatedScore = scoreCardRepository.findAverageModeratedScore(scorecard.getId());
            if(moderatedScore >= 2.5){
                passedScorecards ++;
            }
        }
        return passedScorecards;
    }

    public int countFailedScorecardsByPeriodId(ReportingPeriod reportingPeriod){

        List<Scorecard> scorecardList = getScorecardsByReportingPeriodId(reportingPeriod);
        int failedScorecards = 0;

        for(Scorecard scorecard : scorecardList){
            double moderatedScore = scoreCardRepository.findAverageModeratedScore(scorecard.getId());
            if(moderatedScore < 2.5){
                failedScorecards ++;
            }
        }
        return failedScorecards;
    }

    public List<Double> findAverageAllocatedWeightPerGoal(){

        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        List<Goal> GoalsList = goalService.listAllGoals(reportingPeriod.getId());

        List<Double> averageWeights = new ArrayList<>();
        for(Goal Goal : GoalsList){

            averageWeights.add(scoreCardRepository.findAverageAllocatedWeightPerGoal(Goal));
        }

        return averageWeights;
    }

    public List<Double> findAverageWeightedScorePerGoal(){

        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        List<Goal> GoalsList = goalService.listAllGoals(reportingPeriod.getId());

        List<Double> averageWeightedScores = new ArrayList<>();
        for(Goal Goal : GoalsList){

            double averageScore = scoreCardRepository.findAverageWeightedScorePerGoal(Goal);
            double averageWeight = scoreCardRepository.findAverageAllocatedWeightPerGoal(Goal);
            double weightedScore = (averageScore / 5) * averageWeight;
            averageWeightedScores.add(weightedScore);
        }

        return averageWeightedScores;
    }

    public Scorecard getScorecardById(long id){

        Scorecard scorecard = scoreCardRepository.findScorecardById(id);
        scorecard.setEmployeeScore(scoreCardRepository.findAverageEmployeeScore(scorecard.getId()));
        scorecard.setManagerScore(scoreCardRepository.findAverageManagerScore(scorecard.getId()));
        scorecard.setAgreedScore(scoreCardRepository.findAverageAgreedScore(scorecard.getId()));
        scorecard.setModeratedScore(scoreCardRepository.findAverageModeratedScore(scorecard.getId()));
        scorecard.setWeightedScore(scoreCardRepository.findAverageWeightedScore(scorecard.getId()));

        return scorecard;
    }
    public Scorecard getActiveEmployeeScorecardByOwner(Account account){
        try {
            Long scorecardId = scoreCardRepository.findEmployeeActiveScorecardId(account);
            Scorecard scorecard = getScorecardById(scorecardId);
            return scorecard;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    public void addScorecard(Scorecard scorecard) {

        scoreCardRepository.save(scorecard);
    }

    public Scorecard saveScorecard(Scorecard scorecard){
        Scorecard savedScorecard = scoreCardRepository.save(scorecard);
        return savedScorecard;
    }

    public int countActiveScorecards(Account owner, ReportingPeriod reportingPeriod){
        int count = scoreCardRepository.countScorecardsByOwnerAndReportingPeriod(owner, reportingPeriod);
        return count;
    }
}
