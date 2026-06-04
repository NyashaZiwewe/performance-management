package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.service.OverallScoreService;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.utils.dto.CommonResponse;
import hr.performancemanagement.utils.dto.IndividualTrendResponse;
import hr.performancemanagement.utils.dto.PerformanceLevelsResponse;
import hr.performancemanagement.utils.dto.PerformanceReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/performance-reviews")
@RequiredArgsConstructor
public class PerformanceReviewResource {

    private final ReportingPeriodService reportingPeriodService;
    private final ScorecardService scorecardService;
    private final PerformanceImprovementPlanService performanceImprovementPlanService;
    private final ActionPlanService actionPlanService;
    private final GoalService goalService;
    private final AccountService accountService;
    private final OverallScoreService overallScoreService;

    @GetMapping("/reporting-period/{reportingPeriodId}/scores")
    public ResponseEntity<CommonResponse<List<Scorecard>>> getScoresByReportingPeriod(@PathVariable long reportingPeriodId) {
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
        if (reportingPeriod == null) {
            throw new ResourceNotFoundException("Reporting period not found with id " + reportingPeriodId);
        }

        List<Scorecard> scores = scorecardService.getScoresByPeriodId(reportingPeriod);
        return ResponseEntity.ok(CommonResponse.<List<Scorecard>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Scores retrieved successfully")
                .data(scores)
                .build());
    }

    @PostMapping("/performance-levels")
    public ResponseEntity<CommonResponse<PerformanceLevelsResponse>> getPerformanceLevels(@RequestBody List<Long> scorecardIds) {
        List<String> names = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        List<Scorecard> scorecards = scorecardService.getScorecardsByIds(scorecardIds);
        for (Scorecard scorecard : scorecards) {
            if (scorecard != null && scorecard.getOwner() != null) {
                names.add(scorecard.getOwner().getFullName());
                ReportingDate reportingDate = resolveLatestReportingDate(scorecard.getReportingPeriod());
                OverallScore overallScore = overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, reportingDate);
                scores.add(overallModeratedPercent(overallScore));
            }
        }

        PerformanceLevelsResponse response = new PerformanceLevelsResponse(names, scores);
        return ResponseEntity.ok(CommonResponse.<PerformanceLevelsResponse>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Performance levels retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/employee/{employeeId}/trends")
    public ResponseEntity<CommonResponse<IndividualTrendResponse>> getIndividualTrends(@PathVariable long employeeId) {
        Account employee = accountService.getAccountById(employeeId);
        if (employee == null) {
            throw new ResourceNotFoundException("Account not found with id " + employeeId);
        }

        List<String> monthNames = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        List<TrendPoint> trendPoints = buildTrendPoints(employee);

        List<ReportingDate> allReportingDates = new ArrayList<>();
        List<Scorecard> trendScorecards = new ArrayList<>();
        for (TrendPoint point : trendPoints) {
            addUniqueReportingDate(allReportingDates, point.reportingDate);
            addUniqueScorecard(trendScorecards, point.scorecard);
        }

        Map<Long, Map<Long, Double>> scoresByDate;
        try {
            scoresByDate = scorecardService.getScoresByReportingDatesAndScorecardIds(allReportingDates, trendScorecards);
        } catch (Exception exception) {
            scoresByDate = Collections.emptyMap();
        }

        for (TrendPoint point : trendPoints) {
            if (point.reportingDate == null || point.reportingDate.getId() <= 0) {
                continue;
            }
            Double score = null;
            Map<Long, Double> scoreByScorecard = scoresByDate.get(point.reportingDate.getId());
            if (scoreByScorecard != null && point.scorecard != null) {
                score = scoreByScorecard.get(point.scorecard.getId());
            }
            monthNames.add(resolveTrendPointLabel(point));
            scores.add(score == null ? 0.0 : roundTwoDecimals(score));
        }

        IndividualTrendResponse response = new IndividualTrendResponse(monthNames, scores, employee);
        return ResponseEntity.ok(CommonResponse.<IndividualTrendResponse>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Individual trends retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/scorecard/{scorecardId}/report")
    public ResponseEntity<CommonResponse<PerformanceReportResponse>> getPerformanceReport(@PathVariable long scorecardId) {
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        if (scorecard == null) {
            throw new ResourceNotFoundException("Scorecard not found with id " + scorecardId);
        }

        List<Goal> goals = goalService.listAllGoals(scorecardId);
        ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
        Account owner = scorecard.getOwner();
        List<PerformanceImprovementPlan> pips =
                performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
        List<ActionPlan> actionPlans = actionPlanService.listActionPlansByManagerAndReportingPeriod(owner, reportingPeriod);

        ReportingDate finalReportingDate = resolveLatestReportingDate(reportingPeriod);
        OverallScore finalOverallScore = overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, finalReportingDate);
        double averageModeratedScore = resolveModeratedOverall(finalOverallScore, scorecardId);
        double weightedScore = averageModeratedScore <= 0.0
                ? 0.0
                : Math.round(((averageModeratedScore / 5.0) * 100.0) * 100.0) / 100.0;

        PerformanceReportResponse response = new PerformanceReportResponse(
                scorecard,
                goals,
                pips,
                actionPlans,
                owner,
                reportingPeriod == null ? null : reportingPeriod.getStartDate(),
                reportingPeriod == null ? null : reportingPeriod.getEndDate(),
                averageModeratedScore,
                weightedScore
        );

        return ResponseEntity.ok(CommonResponse.<PerformanceReportResponse>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Performance report retrieved successfully")
                .data(response)
                .build());
    }

    private List<TrendPoint> buildTrendPoints(Account employee) {
        List<TrendPoint> points = new ArrayList<>();
        if (employee == null) {
            return points;
        }

        List<Scorecard> scorecards = scorecardService.getScorecardsByOwner(employee);
        if (scorecards == null) {
            return points;
        }

        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getReportingPeriod() == null) {
                continue;
            }
            ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
            List<ReportingDate> reportingDates = sortReportingDates(reportingPeriod.getReportingDates());
            for (ReportingDate reportingDate : reportingDates) {
                if (reportingDate == null || reportingDate.getId() <= 0) {
                    continue;
                }
                points.add(new TrendPoint(scorecard, reportingPeriod, reportingDate));
            }
        }

        points.sort(Comparator
                .comparing((TrendPoint point) -> reportingPeriodSortKey(point.reportingPeriod))
                .thenComparing(point -> sortDateKey(point.reportingDate))
                .thenComparingLong(point -> point.reportingDate == null ? 0L : point.reportingDate.getId())
                .thenComparingLong(point -> point.scorecard == null ? 0L : point.scorecard.getId()));
        return points;
    }

    private List<ReportingDate> sortReportingDates(List<ReportingDate> reportingDates) {
        List<ReportingDate> sortedReportingDates = reportingDates == null
                ? new ArrayList<>()
                : new ArrayList<>(reportingDates);
        sortedReportingDates.sort(Comparator
                .comparing(this::sortDateKey)
                .thenComparingLong(reportingDate -> reportingDate == null ? 0L : reportingDate.getId()));
        return sortedReportingDates;
    }

    private LocalDate sortDateKey(ReportingDate reportingDate) {
        LocalDate parsedDate = reportingDate == null ? null : parseLocalDate(reportingDate.getEndDate());
        return parsedDate == null ? LocalDate.MIN : parsedDate;
    }

    private LocalDate reportingPeriodSortKey(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null) {
            return LocalDate.MIN;
        }
        LocalDate endDate = parseLocalDate(reportingPeriod.getEndDate());
        if (endDate != null) {
            return endDate;
        }
        LocalDate startDate = parseLocalDate(reportingPeriod.getStartDate());
        return startDate == null ? LocalDate.MIN : startDate;
    }

    private void addUniqueReportingDate(List<ReportingDate> reportingDates, ReportingDate reportingDate) {
        if (reportingDates == null || reportingDate == null || reportingDate.getId() <= 0) {
            return;
        }
        for (ReportingDate existing : reportingDates) {
            if (existing != null && existing.getId() == reportingDate.getId()) {
                return;
            }
        }
        reportingDates.add(reportingDate);
    }

    private void addUniqueScorecard(List<Scorecard> scorecards, Scorecard scorecard) {
        if (scorecards == null || scorecard == null || scorecard.getId() <= 0) {
            return;
        }
        for (Scorecard existing : scorecards) {
            if (existing != null && existing.getId() == scorecard.getId()) {
                return;
            }
        }
        scorecards.add(scorecard);
    }

    private String resolveTrendPointLabel(TrendPoint point) {
        if (point == null) {
            return "N/A";
        }
        return resolveReportingDateLabel(point.reportingDate);
    }

    private String resolveReportingDateLabel(ReportingDate reportingDate) {
        if (reportingDate != null && reportingDate.getEndDate() != null && !reportingDate.getEndDate().trim().isEmpty()) {
            return reportingDate.getEndDate();
        }
        return "N/A";
    }

    private ReportingDate resolveLatestReportingDate(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null || reportingPeriod.getReportingDates() == null || reportingPeriod.getReportingDates().isEmpty()) {
            return null;
        }
        ReportingDate selected = null;
        LocalDate selectedDate = null;
        for (ReportingDate reportingDate : reportingPeriod.getReportingDates()) {
            if (reportingDate == null) {
                continue;
            }
            LocalDate candidateDate = parseLocalDate(reportingDate.getEndDate());
            if (candidateDate == null) {
                if (selected == null) {
                    selected = reportingDate;
                }
                continue;
            }
            if (selectedDate == null || candidateDate.isAfter(selectedDate)) {
                selected = reportingDate;
                selectedDate = candidateDate;
            }
        }
        return selected;
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private double overallModeratedPercent(OverallScore overallScore) {
        if (overallScore == null || overallScore.getModeratedOverall() == null || overallScore.getModeratedOverall() <= 0.0) {
            return 0.0;
        }
        return Math.round(((overallScore.getModeratedOverall() / 5.0) * 100.0) * 100.0) / 100.0;
    }

    private double resolveModeratedOverall(OverallScore overallScore, long scorecardId) {
        if (overallScore != null && overallScore.getModeratedOverall() != null && overallScore.getModeratedOverall() > 0.0) {
            return overallScore.getModeratedOverall();
        }
        return goalService.getAverageModeratorScore(scorecardId);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class TrendPoint {
        private final Scorecard scorecard;
        private final ReportingPeriod reportingPeriod;
        private final ReportingDate reportingDate;

        private TrendPoint(Scorecard scorecard, ReportingPeriod reportingPeriod, ReportingDate reportingDate) {
            this.scorecard = scorecard;
            this.reportingPeriod = reportingPeriod;
            this.reportingDate = reportingDate;
        }
    }
}
