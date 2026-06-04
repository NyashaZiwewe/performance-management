package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.OverallScoreRepository;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.repository.ScoreCardRepository;
import hr.performancemanagement.repository.ScorecardWorkflowStageRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
    ScorecardWorkflowStageRepository scorecardWorkflowStageRepository;

    @Autowired
    ReportingDateRepository reportingDateRepository;

    @Autowired
    OverallScoreRepository overallScoreRepository;

    @Autowired
    private final ReportingPeriodService reportingPeriodService;

    public ScorecardServiceImpl(ReportingPeriodService reportingPeriodService) {
        this.reportingPeriodService = reportingPeriodService;
    }

    @Override
    public List<Scorecard> listAllScorecards(long clientId){
        return scoreCardRepository.findScorecardsByClient_ClientId(clientId);
    }

    @Override
    public List<Scorecard> listAllScorecards(long clientId, long reportingPeriodId) {
        return scoreCardRepository.findScorecardsByClient_ClientIdAndReportingPeriod_Id(clientId, reportingPeriodId);
    }

    @Override
    public List<Scorecard> listActiveScorecards(long clientId) {
        return scoreCardRepository.findScorecardsByClient_ClientIdAndStatus(clientId, PMConstants.STATUS_ACTIVE);
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
        return getAccessibleScorecardsByReportingPeriod(reportingPeriod);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scorecard> searchScorecards(Long reportingPeriodId, Long reportingDateId, Long departmentId, Long employeeId, String approvalStatus) {
        Long normalizedReportingPeriodId = normalizeSearchId(reportingPeriodId);
        Long normalizedReportingDateId = normalizeSearchId(reportingDateId);
        Long normalizedDepartmentId = normalizeSearchId(departmentId);
        Long normalizedEmployeeId = normalizeSearchId(employeeId);
        String normalizedApprovalStatus = normalizeSearchText(approvalStatus);

        ReportingPeriod reportingPeriod = normalizedReportingPeriodId == null
                ? null
                : reportingPeriodService.getReportingPeriodById(normalizedReportingPeriodId);
        ReportingDate reportingDate = normalizedReportingDateId == null
                ? null
                : reportingDateRepository.findReportingDateById(normalizedReportingDateId);

        if (normalizedReportingPeriodId != null && reportingPeriod == null) {
            return new ArrayList<>();
        }
        if (normalizedReportingDateId != null && reportingDate == null) {
            return new ArrayList<>();
        }
        if (reportingDate != null && reportingDate.getReportingPeriod() == null) {
            return new ArrayList<>();
        }
        if (reportingPeriod != null && reportingDate != null
                && reportingDate.getReportingPeriod().getId() != reportingPeriod.getId()) {
            return new ArrayList<>();
        }
        if (reportingPeriod == null && reportingDate != null) {
            reportingPeriod = reportingDate.getReportingPeriod();
        }

        List<Scorecard> scorecards;
        if (reportingPeriod != null) {
            scorecards = getAccessibleScorecardsByReportingPeriod(reportingPeriod);
        } else {
            scorecards = new ArrayList<>();
            List<ReportingPeriod> reportingPeriods = reportingPeriodService.listAllReportingPeriods();
            if (reportingPeriods != null) {
                for (ReportingPeriod period : reportingPeriods) {
                    scorecards.addAll(getAccessibleScorecardsByReportingPeriod(period));
                }
            }
        }

        return filterScorecardSearchResults(
                deduplicateScorecards(scorecards),
                normalizedDepartmentId,
                normalizedEmployeeId,
                normalizedApprovalStatus
        );
    }

    private Long normalizeSearchId(Long value) {
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    private String normalizeSearchText(String value) {
        if (value == null || value.trim().isEmpty() || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ENGLISH);
    }

    private List<Scorecard> getAccessibleScorecardsByReportingPeriod(ReportingPeriod reportingPeriod) {
        List<Scorecard> scorecardList = new ArrayList<>();
        Account loggedUser = cs.getLoggedUser();
        if (reportingPeriod == null || loggedUser == null) {
            return scorecardList;
        }
        long configuredClientId = cs.getConfiguredClientId();

        if(cs.isAdmin() || cs.hasSpecialRights()){
            scoreCardRepository.findScorecardsByReportingPeriodAndClient_ClientId(reportingPeriod, configuredClientId).forEach(scorecardList::add);
            if (scorecardList.isEmpty()) {
                scoreCardRepository.findScorecardsByReportingPeriod(reportingPeriod).forEach(scorecardList::add);
            }
        }
        else if(loggedUser.getAccountType().equalsIgnoreCase("Employee")){

            scoreCardRepository.findScorecardsByReportingPeriodAndOwner(reportingPeriod, loggedUser).forEach(scorecard -> scorecardList.add(scorecard));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("Supervisor")){

            scoreCardRepository.findScorecardsByReportingPeriodAndOwner(reportingPeriod, loggedUser).forEach(scorecard -> scorecardList.add(scorecard));
            scoreCardRepository.findScorecardsByReportingPeriodAndOwner_Supervisor(reportingPeriod, loggedUser).forEach(scorecard -> scorecardList.add(scorecard));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("DEPARTMENT_MANAGER") || loggedUser.getAccountType().equalsIgnoreCase("DIVISIONAL_DIRECTOR")){

            scoreCardRepository.findScorecardsByReportingPeriodAndOwner_Department(reportingPeriod, loggedUser.getDepartment()).forEach(scorecard -> scorecardList.add(scorecard));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("ACTING_CEO") || loggedUser.getAccountType().equalsIgnoreCase("CEO") ){

            scoreCardRepository.findScorecardsByReportingPeriodAndClient_ClientId(reportingPeriod, configuredClientId).forEach(scorecardList::add);
            if (scorecardList.isEmpty()) {
                scoreCardRepository.findScorecardsByReportingPeriod(reportingPeriod).forEach(scorecardList::add);
            }

        }
        return deduplicateScorecards(scorecardList);
    }

    private List<Scorecard> filterScorecardSearchResults(List<Scorecard> scorecards, Long departmentId, Long employeeId, String approvalStatus) {
        if (scorecards == null || scorecards.isEmpty()) {
            return new ArrayList<>();
        }
        List<Scorecard> filteredScorecards = new ArrayList<>();
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null) {
                continue;
            }
            if (departmentId != null && !scorecardDepartmentMatches(scorecard, departmentId)) {
                continue;
            }
            if (employeeId != null && !scorecardOwnerMatches(scorecard, employeeId)) {
                continue;
            }
            if (approvalStatus != null && !scorecardApprovalStatusMatches(scorecard, approvalStatus)) {
                continue;
            }
            filteredScorecards.add(scorecard);
        }
        return filteredScorecards;
    }

    private boolean scorecardDepartmentMatches(Scorecard scorecard, Long departmentId) {
        return scorecard != null
                && scorecard.getOwner() != null
                && scorecard.getOwner().getDepartment() != null
                && departmentId != null
                && scorecard.getOwner().getDepartment().getId() == departmentId;
    }

    private boolean scorecardOwnerMatches(Scorecard scorecard, Long employeeId) {
        return scorecard != null
                && scorecard.getOwner() != null
                && employeeId != null
                && scorecard.getOwner().getId() == employeeId;
    }

    private boolean scorecardApprovalStatusMatches(Scorecard scorecard, String approvalStatus) {
        return scorecard != null
                && scorecard.getApprovalStatus() != null
                && approvalStatus != null
                && scorecard.getApprovalStatus().equalsIgnoreCase(approvalStatus);
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
        Double finalScorePercent = scoresByScorecardId.get(scorecard.getId());
        return finalScorePercent == null ? 0.0 : finalScorePercent;
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

        List<OverallScore> rows = overallScoreRepository.findOverallScoresByScorecardIdsAndReportingDateIds(
                scorecardIds,
                new ArrayList<>(reportingDateIds)
        );
        Map<Long, Map<Long, Double>> finalScoresByDate = new HashMap<>();
        for (OverallScore overallScore : rows) {
            if (overallScore == null
                    || overallScore.getScorecard() == null
                    || overallScore.getReportingDate() == null) {
                continue;
            }
            long scorecardId = overallScore.getScorecard().getId();
            long reportingDateId = overallScore.getReportingDate().getId();
            double finalScorePercent = moderatedOverallToPercent(overallScore.getModeratedOverall());
            Map<Long, Double> scoresByScorecard = finalScoresByDate.computeIfAbsent(reportingDateId, key -> new HashMap<>());
            scoresByScorecard.put(scorecardId, finalScorePercent);
        }
        return finalScoresByDate;
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
        alignApprovalStage(scorecard);
        scoreCardRepository.save(scorecard);
    }

    @Override
    @Transactional
    public Scorecard saveScorecard(Scorecard scorecard){
        alignApprovalStage(scorecard);
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

    private double moderatedOverallToPercent(Double moderatedOverall) {
        double moderatedScore = toDouble(moderatedOverall);
        if (moderatedScore <= 0.0) {
            return 0.0;
        }
        return Math.round(((moderatedScore / 5.0) * 100.0) * 100.0) / 100.0;
    }

    private void alignApprovalStage(Scorecard scorecard) {
        if (scorecard == null) {
            return;
        }

        long clientId = resolveClientId(scorecard);
        if (clientId > 0 && (scorecard.getClient() == null || scorecard.getClient().getClientId() <= 0)) {
            if (scorecard.getOwner() != null
                    && scorecard.getOwner().getClient() != null
                    && scorecard.getOwner().getClient().getClientId() == clientId) {
                scorecard.setClient(scorecard.getOwner().getClient());
            } else {
                scorecard.setClientId(clientId);
            }
        }

        List<ScorecardWorkflowStage> activeStages = loadActiveStages(clientId);
        ScorecardWorkflowStage resolvedStage = resolveStageFromReference(scorecard.getApprovalStage(), clientId, activeStages);
        String requestedStatus = normalizeStatus(scorecard.getApprovalStatus());

        if (resolvedStage == null && requestedStatus != null) {
            resolvedStage = resolveStageByStatus(clientId, activeStages, requestedStatus);
        }

        if (resolvedStage == null && scorecard.getId() > 0) {
            Scorecard existing = scoreCardRepository.findScorecardById(scorecard.getId());
            if (existing != null) {
                resolvedStage = resolveStageFromReference(existing.getApprovalStage(), clientId, activeStages);
                if (requestedStatus == null) {
                    requestedStatus = normalizeStatus(existing.getApprovalStatus());
                }
            }
        }

        if (resolvedStage == null) {
            String fallbackStatus = requestedStatus == null ? PMConstants.APPROVAL_STATUS_NEW : requestedStatus;
            resolvedStage = resolveStageByStatus(clientId, activeStages, fallbackStatus);
        }

        if (resolvedStage != null) {
            scorecard.setApprovalStage(resolvedStage);
            scorecard.setApprovalStatus(resolvedStage.getStatusCode());
        } else if (requestedStatus != null) {
            scorecard.setApprovalStatus(requestedStatus);
        }
    }

    private List<ScorecardWorkflowStage> loadActiveStages(long clientId) {
        if (clientId <= 0) {
            return new ArrayList<ScorecardWorkflowStage>();
        }
        List<ScorecardWorkflowStage> stages =
                scorecardWorkflowStageRepository.findScorecardWorkflowStagesByClientIdAndStatusOrderByStageOrderAsc(
                        clientId, PMConstants.STATUS_ACTIVE
                );
        return stages == null ? new ArrayList<ScorecardWorkflowStage>() : stages;
    }

    private ScorecardWorkflowStage resolveStageByStatus(long clientId,
                                                        List<ScorecardWorkflowStage> activeStages,
                                                        String statusCode) {
        if (statusCode == null) {
            return null;
        }
        ScorecardWorkflowStage direct = findStageByStatus(activeStages, statusCode);
        if (direct != null) {
            return direct;
        }
        ScorecardWorkflowStage byStatusRole = findStageByRoleKey(activeStages, statusCode);
        if (byStatusRole != null) {
            return byStatusRole;
        }
        String roleKey = mapStatusToStageRole(statusCode);
        if (roleKey == null) {
            return null;
        }
        return findStageByRoleKey(activeStages, roleKey);
    }

    private ScorecardWorkflowStage resolveStageFromReference(ScorecardWorkflowStage reference,
                                                             long clientId,
                                                             List<ScorecardWorkflowStage> activeStages) {
        if (reference == null) {
            return null;
        }
        if (reference.getId() > 0) {
            if (clientId > 0) {
                ScorecardWorkflowStage byClient =
                        scorecardWorkflowStageRepository.findScorecardWorkflowStageByIdAndClientId(reference.getId(), clientId);
                if (byClient != null) {
                    return byClient;
                }
            }
            return scorecardWorkflowStageRepository.findById(reference.getId()).orElse(null);
        }
        String statusCode = normalizeStatus(reference.getStatusCode());
        if (statusCode != null) {
            return resolveStageByStatus(clientId, activeStages, statusCode);
        }
        return null;
    }

    private ScorecardWorkflowStage findStageByStatus(List<ScorecardWorkflowStage> stages, String statusCode) {
        if (stages == null || stages.isEmpty() || statusCode == null) {
            return null;
        }
        for (ScorecardWorkflowStage stage : stages) {
            if (stage == null) {
                continue;
            }
            if (statusCode.equalsIgnoreCase(normalizeStatus(stage.getStatusCode()))) {
                return stage;
            }
            String statusCodes = stage.getStatusCodes();
            if (statusCodes == null || statusCodes.trim().isEmpty()) {
                continue;
            }
            String[] values = statusCodes.split(",");
            for (String value : values) {
                if (statusCode.equalsIgnoreCase(normalizeStatus(value))) {
                    return stage;
                }
            }
        }
        return null;
    }

    private ScorecardWorkflowStage findStageByRoleKey(List<ScorecardWorkflowStage> stages, String roleKey) {
        if (stages == null || stages.isEmpty() || roleKey == null) {
            return null;
        }
        for (ScorecardWorkflowStage stage : stages) {
            if (stage == null) {
                continue;
            }
            String candidateRoleKey = normalizeStatus(stage.getRoleKey());
            if (roleKey.equalsIgnoreCase(candidateRoleKey)) {
                return stage;
            }
        }
        return null;
    }

    private String mapStatusToStageRole(String statusCode) {
        if (statusCode == null) {
            return null;
        }
        String status = statusCode.toUpperCase(Locale.ENGLISH);
        switch (status) {
            case PMConstants.APPROVAL_STATUS_NEW:
                return PMConstants.SCORECARD_STAGE_NEW;
            case PMConstants.APPROVAL_STATUS_PENDING_APPROVAL:
                return PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR;
            case PMConstants.APPROVAL_STATUS_APPROVED_BY_SUPERVISOR:
                return PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR;
            case PMConstants.APPROVAL_STATUS_REJECTED_BY_SUPERVISOR:
                return PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS;
            case PMConstants.APPROVAL_STATUS_APPROVED_BY_HR:
                return PMConstants.SCORECARD_STAGE_OWNER_SCORING;
            case PMConstants.APPROVAL_STATUS_REJECTED_BY_HR:
                return PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR;
            case PMConstants.APPROVAL_STATUS_SCORED_BY_EMPLOYEE:
                return PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL;
            case PMConstants.APPROVAL_STATUS_APPROVED_OWNER_SCORES:
                return PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING;
            case PMConstants.APPROVAL_STATUS_SCORED_BY_SUPERVISOR:
                return PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING;
            case PMConstants.APPROVAL_STATUS_AGREED_BY_TWO:
                return PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL;
            case PMConstants.APPROVAL_STATUS_APPROVED_AGREED_SCORES:
                return PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING;
            case PMConstants.APPROVAL_STATUS_MODERATED_BY_HR:
            case PMConstants.APPROVAL_STATUS_CLOSED:
                return PMConstants.SCORECARD_STAGE_CLOSED;
            case "PENDING":
                return PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR;
            case "APPROVED":
                return PMConstants.SCORECARD_STAGE_OWNER_SCORING;
            case "REJECTED":
            case "RETURNED":
                return PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS;
            default:
                return null;
        }
    }

    private long resolveClientId(Scorecard scorecard) {
        if (scorecard == null) {
            return 0;
        }
        if (scorecard.getClientId() > 0) {
            return scorecard.getClientId();
        }
        if (scorecard.getOwner() != null && scorecard.getOwner().getClientId() > 0) {
            return scorecard.getOwner().getClientId();
        }
        return 0;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return status.trim().toUpperCase(Locale.ENGLISH);
    }

    private double safeScore(Double value) {
        return value == null ? 0.0 : value;
    }
}
