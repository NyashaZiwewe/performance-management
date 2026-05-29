package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.ScoreCardRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;


@Service
public class ScorecardServiceImpl implements hr.performancemanagement.service.api.ScorecardService {

    @Autowired
    ScoreCardRepository scoreCardRepository;

    @Autowired
    AccountService accountService;
    @Autowired
    CommonService cs;
    @Autowired
    HttpSession session;

    @Autowired
    StrategicObjectiveService strategicObjectiveService;
    @Autowired
    private final ReportingPeriodService reportingPeriodService;

    public ScorecardServiceImpl(ReportingPeriodService reportingPeriodService) {
        this.reportingPeriodService = reportingPeriodService;
    }

    @Override
    public List<Scorecard> listAllScorecards(long clientId){
        return scoreCardRepository.findScorecardsByClientId(clientId);
    }

    @Override
    public List<Scorecard> listAllScorecards(long clientId, long reportingPeriodId) {
        return scoreCardRepository.findScorecardsByClientIdAndReportingPeriod_Id(clientId, reportingPeriodId);
    }

    @Override
    public List<Scorecard> listActiveScorecards(long clientId) {
        return scoreCardRepository.findScorecardsByClientIdAndStatus(clientId, PMConstants.STATUS_ACTIVE);
    }

    @Override
    public List<Scorecard> getScorecardsByOwner(Account owner){
        return scoreCardRepository.findScorecardsByOwner(owner);
    }

    @Override
    public List<Scorecard> getScorecardsByIds(List<Long> scorecardIds) {
        if (scorecardIds == null || scorecardIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Scorecard> scorecards = scoreCardRepository.findAllById(scorecardIds);
        applyScoreAverages(scorecards);
        return scorecards;
    }


    @Override
    public List<Scorecard> getScorecardsByReportingPeriodId(ReportingPeriod reportingPeriod){

        List<Scorecard> scorecardList = new ArrayList<>();
        Account loggedUser = cs.getLoggedUser();
        if (reportingPeriod == null || loggedUser == null) {
            return scorecardList;
        }

        if(cs.isAdmin() || cs.hasSpecialRights()){
            scoreCardRepository.findScorecardsByReportingPeriodAndClientId(reportingPeriod, loggedUser.getClientId()).forEach(scorecard -> scorecardList.add(scorecard));
        }
        else if(loggedUser.getAccountType().equalsIgnoreCase("Employee")){

            scoreCardRepository.findScorecardsByReportingPeriodAndOwner(reportingPeriod, loggedUser).forEach(scorecard -> scorecardList.add(scorecard));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("Supervisor")){

            scoreCardRepository.findScorecardsByReportingPeriodAndOwner(reportingPeriod, loggedUser).forEach(scorecard -> scorecardList.add(scorecard));
            scoreCardRepository.findScorecardsByReportingPeriodAndOwner_Supervisor(reportingPeriod, loggedUser).forEach(scorecard -> scorecardList.add(scorecard));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("DEPARTMENT_MANAGER") || loggedUser.getAccountType().equalsIgnoreCase("DIVISIONAL_DIRECTOR")){

            scoreCardRepository.findScorecardsByReportingPeriodAndOwner_Department(reportingPeriod, loggedUser.getDepartment()).forEach(scorecard -> scorecardList.add(scorecard));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("ACTING_CEO") || loggedUser.getAccountType().equalsIgnoreCase("CEO") ){

            scoreCardRepository.findScorecardsByReportingPeriodAndClientId(reportingPeriod, loggedUser.getClientId()).forEach(scorecard -> scorecardList.add(scorecard));

        }else{

        }
        return deduplicateScorecards(scorecardList);
    }

    @Override
    public List<Scorecard> getScoresByPeriodId(ReportingPeriod reportingPeriod){

        List<Scorecard> scorecardList = getScorecardsByReportingPeriodId(reportingPeriod);
        applyScoreAverages(scorecardList);
        return scorecardList;
    }

    @Override
    public Double getScoresByReportingDateAndScorecardId(ReportingDate date, Scorecard scorecard){
        if (date == null || scorecard == null) {
            return 0.0;
        }
        Map<Long, Double> scoresByScorecardId = getScoresByReportingDateAndScorecardIds(date, Collections.singletonList(scorecard));
        Double weightedScore = scoresByScorecardId.get(scorecard.getId());
        return weightedScore == null ? 0.0 : weightedScore;
    }

    @Override
    public Map<Long, Double> getScoresByReportingDateAndScorecardIds(ReportingDate date, List<Scorecard> scorecards) {
        if (date == null || scorecards == null || scorecards.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Map<Long, Double>> groupedScores =
                getScoresByReportingDatesAndScorecardIds(Collections.singletonList(date), scorecards);
        Map<Long, Double> scoresByScorecardId = groupedScores.get(date.getId());
        if (scoresByScorecardId == null) {
            return Collections.emptyMap();
        }
        return scoresByScorecardId;
    }

    @Override
    public Map<Long, Map<Long, Double>> getScoresByReportingDatesAndScorecardIds(
            List<ReportingDate> reportingDates,
            List<Scorecard> scorecards
    ) {
        if (reportingDates == null || reportingDates.isEmpty() || scorecards == null || scorecards.isEmpty()) {
            return Collections.emptyMap();
        }

        LinkedHashSet<Long> reportingDateIds = new LinkedHashSet<>();
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate != null && reportingDate.getId() > 0) {
                reportingDateIds.add(reportingDate.getId());
            }
        }
        if (reportingDateIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> scorecardIds = collectScorecardIds(scorecards);
        if (scorecardIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> rows = scoreCardRepository.findAverageWeightedScoresByReportingDatesAndScorecardIds(new ArrayList<>(reportingDateIds), scorecardIds);
        Map<Long, Map<Long, Double>> weightedScoresByDate = new HashMap<>();
        for (Object[] row : rows) {
            long scorecardId = toLong(row[0]);
            long reportingDateId = toLong(row[1]);
            double weightedScore = toDouble(row[2]);
            Map<Long, Double> scoresByScorecard = weightedScoresByDate.computeIfAbsent(reportingDateId, key -> new HashMap<>());
            scoresByScorecard.put(scorecardId, weightedScore);
        }
        return weightedScoresByDate;
    }

    @Override
    public int countPassedScorecardsByPeriodId(ReportingPeriod reportingPeriod){

        List<Scorecard> scorecardList = getScoresByPeriodId(reportingPeriod);
        int passedScorecards = 0;

        for(Scorecard scorecard : scorecardList){
            double moderatedScore = safeScore(scorecard.getModeratedScore());
            if(moderatedScore >= 2.5){
                passedScorecards ++;
            }
        }
        return passedScorecards;
    }

    @Override
    public int countFailedScorecardsByPeriodId(ReportingPeriod reportingPeriod){

        List<Scorecard> scorecardList = getScoresByPeriodId(reportingPeriod);
        int failedScorecards = 0;

        for(Scorecard scorecard : scorecardList){
            double moderatedScore = safeScore(scorecard.getModeratedScore());
            if(moderatedScore < 2.5){
                failedScorecards ++;
            }
        }
        return failedScorecards;
    }

    @Override
    public List<Double> findAverageAllocatedWeightPerStrategicObjective(){

        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        return findAverageAllocatedWeightPerStrategicObjective(reportingPeriod);
    }

    @Override
    public List<Double> findAverageAllocatedWeightPerStrategicObjective(ReportingPeriod reportingPeriod) {
        List<Double> averageWeights = new ArrayList<>();
        if (reportingPeriod == null) {
            return averageWeights;
        }

        List<StrategicObjective> strategicObjectivesList = strategicObjectiveService.listAllStrategicObjectives(reportingPeriod.getId());
        List<Long> strategicObjectiveIds = new ArrayList<>();
        for (StrategicObjective strategicObjective : strategicObjectivesList) {
            if (strategicObjective != null && strategicObjective.getId() > 0) {
                strategicObjectiveIds.add(strategicObjective.getId());
            }
        }
        Map<Long, Double> weightsByObjectiveId = new HashMap<>();
        if (!strategicObjectiveIds.isEmpty()) {
            List<Object[]> rows = scoreCardRepository.findAverageAllocatedWeightPerStrategicObjectiveIds(strategicObjectiveIds);
            for (Object[] row : rows) {
                weightsByObjectiveId.put(toLong(row[0]), toDouble(row[1]));
            }
        }
        for(StrategicObjective strategicObjective : strategicObjectivesList){
            if (strategicObjective == null) {
                averageWeights.add(0.0);
                continue;
            }
            averageWeights.add(weightsByObjectiveId.getOrDefault(strategicObjective.getId(), 0.0));
        }

        return averageWeights;
    }

    @Override
    public List<Double> findAverageWeightedScorePerStrategicObjective(){

        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        return findAverageWeightedScorePerStrategicObjective(reportingPeriod);
    }

    @Override
    public List<Double> findAverageWeightedScorePerStrategicObjective(ReportingPeriod reportingPeriod) {
        List<Double> averageWeightedScores = new ArrayList<>();
        if (reportingPeriod == null) {
            return averageWeightedScores;
        }

        List<StrategicObjective> strategicObjectivesList = strategicObjectiveService.listAllStrategicObjectives(reportingPeriod.getId());
        List<Long> strategicObjectiveIds = new ArrayList<>();
        for (StrategicObjective strategicObjective : strategicObjectivesList) {
            if (strategicObjective != null && strategicObjective.getId() > 0) {
                strategicObjectiveIds.add(strategicObjective.getId());
            }
        }
        Map<Long, Double> averageScoresByObjectiveId = new HashMap<>();
        Map<Long, Double> averageWeightsByObjectiveId = new HashMap<>();
        if (!strategicObjectiveIds.isEmpty()) {
            List<Object[]> scoreRows = scoreCardRepository.findAverageWeightedScorePerStrategicObjectiveIds(strategicObjectiveIds);
            for (Object[] row : scoreRows) {
                averageScoresByObjectiveId.put(toLong(row[0]), toDouble(row[1]));
            }
            List<Object[]> weightRows = scoreCardRepository.findAverageAllocatedWeightPerStrategicObjectiveIds(strategicObjectiveIds);
            for (Object[] row : weightRows) {
                averageWeightsByObjectiveId.put(toLong(row[0]), toDouble(row[1]));
            }
        }
        for(StrategicObjective strategicObjective : strategicObjectivesList){
            if (strategicObjective == null) {
                averageWeightedScores.add(0.0);
                continue;
            }
            double averageScore = averageScoresByObjectiveId.getOrDefault(strategicObjective.getId(), 0.0);
            double averageWeight = averageWeightsByObjectiveId.getOrDefault(strategicObjective.getId(), 0.0);
            double weightedScore = (averageScore / 5) * averageWeight;
            averageWeightedScores.add(weightedScore);
        }

        return averageWeightedScores;
    }

    @Override
    public Scorecard getScorecardById(long id){

        Scorecard scorecard = scoreCardRepository.findScorecardById(id);
        if (scorecard == null) {
            return null;
        }
        applyScoreAverages(Collections.singletonList(scorecard));
        return scorecard;
    }
    @Override
    public Scorecard getActiveEmployeeScorecardByOwner(Account account){
        try {
            Long scorecardId = scoreCardRepository.findEmployeeActiveScorecardId(account);
            Scorecard scorecard = getScorecardById(scorecardId);
            return scorecard;
        }catch (Exception ignored){
            return null;
        }
    }
    @Override
    @Transactional
    public void addScorecard(Scorecard scorecard) {

        scoreCardRepository.save(scorecard);
    }

    @Override
    @Transactional
    public Scorecard saveScorecard(Scorecard scorecard){
        Scorecard savedScorecard = scoreCardRepository.save(scorecard);
        return savedScorecard;
    }

    @Override
    public int countActiveScorecards(Account owner, ReportingPeriod reportingPeriod){
        int count = scoreCardRepository.countScorecardsByOwnerAndReportingPeriod(owner, reportingPeriod);
        return count;
    }

    private void applyScoreAverages(List<Scorecard> scorecards) {
        if (scorecards == null || scorecards.isEmpty()) {
            return;
        }
        List<Long> scorecardIds = collectScorecardIds(scorecards);
        Map<Long, Object[]> averagesByScorecardId = loadAverageRowsByScorecardId(scorecardIds);
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getId() <= 0) {
                continue;
            }
            Object[] averages = averagesByScorecardId.get(scorecard.getId());
            if (averages == null) {
                scorecard.setEmployeeScore(0.0);
                scorecard.setManagerScore(0.0);
                scorecard.setAgreedScore(0.0);
                scorecard.setModeratedScore(0.0);
                scorecard.setWeightedScore(0.0);
                continue;
            }
            scorecard.setEmployeeScore(toDouble(averages[1]));
            scorecard.setManagerScore(toDouble(averages[2]));
            scorecard.setAgreedScore(toDouble(averages[3]));
            scorecard.setModeratedScore(toDouble(averages[4]));
            scorecard.setWeightedScore(toDouble(averages[5]));
        }
    }

    private Map<Long, Object[]> loadAverageRowsByScorecardId(List<Long> scorecardIds) {
        if (scorecardIds == null || scorecardIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = scoreCardRepository.findScoreAveragesByScorecardIds(scorecardIds);
        Map<Long, Object[]> averagesByScorecardId = new HashMap<>();
        for (Object[] row : rows) {
            long scorecardId = toLong(row[0]);
            averagesByScorecardId.put(scorecardId, row);
        }
        return averagesByScorecardId;
    }

    private List<Long> collectScorecardIds(List<Scorecard> scorecards) {
        LinkedHashSet<Long> scorecardIds = new LinkedHashSet<>();
        if (scorecards == null) {
            return new ArrayList<>();
        }
        for (Scorecard scorecard : scorecards) {
            if (scorecard != null && scorecard.getId() > 0) {
                scorecardIds.add(scorecard.getId());
            }
        }
        return new ArrayList<>(scorecardIds);
    }

    private List<Scorecard> deduplicateScorecards(List<Scorecard> scorecards) {
        if (scorecards == null || scorecards.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Scorecard> deduplicated = new LinkedHashMap<>();
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null) {
                continue;
            }
            deduplicated.put(scorecard.getId(), scorecard);
        }
        return new ArrayList<>(deduplicated.values());
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private double safeScore(Double value) {
        return value == null ? 0.0 : value;
    }
}
