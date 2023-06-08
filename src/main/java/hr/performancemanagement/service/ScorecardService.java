package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
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
    HttpSession session;

    public List<Scorecard> listAllScorecards(long clientId){
        List<Scorecard> scorecardList = new ArrayList<>();
        scoreCardRepository.findScorecardsByClientId(clientId).forEach(scorecard -> scorecardList.add(scorecard));
        return scorecardList;
    }

    public List<Scorecard> getScorecardsByReportingPeriodId(ReportingPeriod reportingPeriod){

        List<Scorecard> scorecardList = new ArrayList<>();
        Account loggedUser = (Account) session.getAttribute("loggedUser");

        if(loggedUser.getAdmin().equalsIgnoreCase("IS_ADMIN") || loggedUser.getSpecial().equalsIgnoreCase("HAS_SPECIAL_RIGHTS")){
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
            scorecard.setActualScore(scoreCardRepository.findAverageActualScore(scorecard.getId()));
        }

        return scorecardList;
    }

    public Scorecard getScorecardById(long id){

        Scorecard scorecard = scoreCardRepository.findScorecardById(id);
        return scorecard;
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
