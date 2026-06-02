package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.ScoreCardRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ScorecardService {
    List<Scorecard> listAllScorecards(long clientId);
    List<Scorecard> listAllScorecards(long clientId, long reportingPeriodId);
    List<Scorecard> listActiveScorecards(long clientId);
    List<Scorecard> getScorecardsByOwner(Account owner);
    List<Scorecard> getScorecardsByIds(List<Long> scorecardIds);
    List<Scorecard> getScorecardsByReportingPeriodId(ReportingPeriod reportingPeriod);
    List<Scorecard> searchScorecards(Long reportingPeriodId, Long reportingDateId, Long departmentId, Long employeeId, String approvalStatus);
    List<Scorecard> getScoresByPeriodId(ReportingPeriod reportingPeriod);
    Double getScoresByReportingDateAndScorecardId(ReportingDate date, Scorecard scorecard);
    Map<Long, Double> getScoresByReportingDateAndScorecardIds(ReportingDate date, List<Scorecard> scorecards);
    Map<Long, Map<Long, Double>> getScoresByReportingDatesAndScorecardIds(List<ReportingDate> reportingDates, List<Scorecard> scorecards);
    int countPassedScorecardsByPeriodId(ReportingPeriod reportingPeriod);
    int countFailedScorecardsByPeriodId(ReportingPeriod reportingPeriod);
    List<Double> findAverageAllocatedWeightPerStrategicObjective(ReportingPeriod reportingPeriod);
    List<Double> findAverageWeightedScorePerStrategicObjective(ReportingPeriod reportingPeriod);
    List<Double> findAverageAllocatedWeightPerStrategicObjective();
    List<Double> findAverageWeightedScorePerStrategicObjective();
    Scorecard getScorecardById(long id);
    Scorecard getActiveEmployeeScorecardByOwner(Account account);
    void addScorecard(Scorecard scorecard);
    Scorecard saveScorecard(Scorecard scorecard);
    int countActiveScorecards(Account owner, ReportingPeriod reportingPeriod);
}
