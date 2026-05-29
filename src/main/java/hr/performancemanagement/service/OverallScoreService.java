package hr.performancemanagement.service;

import hr.performancemanagement.entities.OverallScore;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.OverallScoreRepository;
import hr.performancemanagement.repository.ReportingDateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OverallScoreService {
    @Autowired
    OverallScoreRepository repository;
    @Autowired
    ReportingDateRepository dateRepository;

    public OverallScore getOverallScoreByScorecardAndReportingDate(Scorecard scorecard, ReportingDate reportingDate){
        if (scorecard == null) {
            return null;
        }
        if(reportingDate == null){
            reportingDate = dateRepository.findTopByReportingPeriodOrderByEndDateDesc(scorecard.getReportingPeriod());
        }
        if (reportingDate == null) {
            return createDefaultScore(scorecard, null);
        }
        OverallScore overallScore = repository.findOverallScoreByScorecardAndReportingDate(scorecard, reportingDate);
        return overallScore != null ? overallScore : createDefaultScore(scorecard, reportingDate);
    }

    public List<OverallScore> getOverallScoreByScorecard(Scorecard scorecard){
        List<OverallScore> overallScores = new ArrayList<>();
        if(scorecard != null){
            overallScores = repository.getOverallScoresByScorecard(scorecard);
        }
        return overallScores;
    }

    public OverallScore saveOverallScore(OverallScore score){
        return repository.save(score);
    }

    public Map<Long, Map<Long, OverallScore>> getOverallScoresByScorecardsAndReportingDates(
            List<Scorecard> scorecards,
            List<ReportingDate> reportingDates
    ) {
        if (scorecards == null || scorecards.isEmpty() || reportingDates == null || reportingDates.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> scorecardIds = new ArrayList<>();
        for (Scorecard scorecard : scorecards) {
            if (scorecard != null && scorecard.getId() > 0) {
                scorecardIds.add(scorecard.getId());
            }
        }
        List<Long> reportingDateIds = new ArrayList<>();
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate != null && reportingDate.getId() > 0) {
                reportingDateIds.add(reportingDate.getId());
            }
        }
        if (scorecardIds.isEmpty() || reportingDateIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<OverallScore> overallScores =
                repository.findOverallScoresByScorecardIdsAndReportingDateIds(scorecardIds, reportingDateIds);
        Map<Long, Map<Long, OverallScore>> overallScoresByDate = new HashMap<>();
        for (OverallScore overallScore : overallScores) {
            if (overallScore == null
                    || overallScore.getReportingDate() == null
                    || overallScore.getScorecard() == null) {
                continue;
            }
            long reportingDateId = overallScore.getReportingDate().getId();
            long scorecardId = overallScore.getScorecard().getId();
            Map<Long, OverallScore> scoreByScorecard =
                    overallScoresByDate.computeIfAbsent(reportingDateId, key -> new HashMap<>());
            scoreByScorecard.put(scorecardId, overallScore);
        }
        return overallScoresByDate;
    }

    private OverallScore createDefaultScore(Scorecard scorecard, ReportingDate reportingDate) {
        OverallScore overallScore = new OverallScore();
        overallScore.setScorecard(scorecard);
        overallScore.setReportingDate(reportingDate);
        overallScore.setEmployeeOverall(0.0);
        overallScore.setManagerOverall(0.0);
        overallScore.setAgreedOverall(0.0);
        overallScore.setModeratedOverall(0.0);
        return overallScore;
    }
}
