package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.utils.dto.CommonResponse;
import hr.performancemanagement.utils.dto.IndividualTrendResponse;
import hr.performancemanagement.utils.dto.PerformanceLevelsResponse;
import hr.performancemanagement.utils.dto.PerformanceReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
                scores.add(scorecard.getWeightedScore());
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

        List<Scorecard> scorecards = scorecardService.getScorecardsByOwner(employee);
        List<String> monthNames = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        List<ReportingDate> allReportingDates = new ArrayList<>();
        for (Scorecard scorecard : scorecards) {
            if (scorecard != null
                    && scorecard.getReportingPeriod() != null
                    && scorecard.getReportingPeriod().getReportingDates() != null) {
                allReportingDates.addAll(scorecard.getReportingPeriod().getReportingDates());
            }
        }
        Map<Long, Map<Long, Double>> scoresByDate =
                scorecardService.getScoresByReportingDatesAndScorecardIds(allReportingDates, scorecards);
        for (Scorecard scorecard : scorecards) {
            if (scorecard.getReportingPeriod() == null || scorecard.getReportingPeriod().getReportingDates() == null) {
                continue;
            }

            for (ReportingDate reportingDate : scorecard.getReportingPeriod().getReportingDates()) {
                Double score = 0.0;
                if (reportingDate != null) {
                    Map<Long, Double> scoreByScorecard = scoresByDate.get(reportingDate.getId());
                    if (scoreByScorecard != null && scoreByScorecard.containsKey(scorecard.getId())) {
                        score = scoreByScorecard.get(scorecard.getId());
                    }
                }
                monthNames.add(reportingDate.getEndDate());
                scores.add(score);
            }
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

        double averageModeratedScore = goalService.getAverageModeratorScore(scorecardId);
        double weightedScore;
        try {
            weightedScore = (averageModeratedScore / 5) * 100;
        } catch (Exception exception) {
            weightedScore = 0;
        }

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
}
