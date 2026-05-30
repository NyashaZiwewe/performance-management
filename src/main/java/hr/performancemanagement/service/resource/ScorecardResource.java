package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.exception.BadRequestException;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.service.api.ScoreService.StandardScorecardScoreService;
import hr.performancemanagement.service.api.ScoreService.ValueBasedScoreService;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.dto.CommonResponse;
import hr.performancemanagement.utils.dto.ScorecardWorkflowDefinition;
import hr.performancemanagement.utils.wrappers.GoalWrapper;
import hr.performancemanagement.utils.wrappers.StatusUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/scorecards")
@RequiredArgsConstructor
public class ScorecardResource {

    private final ScorecardService scorecardService;
    private final ScorecardModelService scorecardModelService;
    private final ReportingPeriodService reportingPeriodService;
    private final ReportingDateService reportingDateService;
    private final AccountService accountService;
    private final GoalService goalService;
    private final TargetService targetService;
    private final CommentService commentService;
    private final CommonService commonService;
    private final StandardScorecardScoreService standardScorecardScoreService;
    private final ValueBasedScoreService valueBasedScoreService;
    private final ScorecardWorkflowService scorecardWorkflowService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<CommonResponse<List<Scorecard>>> listByClientId(@PathVariable long clientId) {
        List<Scorecard> scorecards = scorecardService.listAllScorecards(clientId);
        return ResponseEntity.ok(CommonResponse.<List<Scorecard>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Scorecards retrieved successfully")
                .data(scorecards)
                .build());
    }

    @GetMapping("/client/{clientId}/reporting-period/{reportingPeriodId}")
    public ResponseEntity<CommonResponse<List<Scorecard>>> listByClientAndReportingPeriod(
            @PathVariable long clientId,
            @PathVariable long reportingPeriodId) {
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
        if (reportingPeriod == null) {
            throw new ResourceNotFoundException("Reporting period not found with id " + reportingPeriodId);
        }

        List<Scorecard> filteredScorecards = scorecardService.listAllScorecards(clientId, reportingPeriodId);

        return ResponseEntity.ok(CommonResponse.<List<Scorecard>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Scorecards retrieved successfully")
                .data(filteredScorecards)
                .build());
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<CommonResponse<List<Scorecard>>> listByOwner(@PathVariable long ownerId) {
        Account owner = accountService.getAccountById(ownerId);
        if (owner == null) {
            throw new ResourceNotFoundException("Account not found with id " + ownerId);
        }
        List<Scorecard> scorecards = scorecardService.getScorecardsByOwner(owner);
        return ResponseEntity.ok(CommonResponse.<List<Scorecard>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Scorecards retrieved successfully")
                .data(scorecards)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<Scorecard>> getById(@PathVariable long id) {
        Scorecard scorecard = scorecardService.getScorecardById(id);
        if (scorecard == null) {
            throw new ResourceNotFoundException("Scorecard not found with id " + id);
        }
        return ResponseEntity.ok(CommonResponse.<Scorecard>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Scorecard retrieved successfully")
                .data(scorecard)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<Scorecard>> save(@RequestBody Scorecard scorecard) {
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        normalizeScorecardReferences(scorecard);
        if (!hasText(scorecard.getStatus())) {
            scorecard.setStatus(PMConstants.STATUS_ACTIVE);
        }
        if (!hasText(scorecard.getApprovalStatus())) {
            scorecard.setApprovalStatus(workflow.getNewStatus());
        }
        if (!hasText(scorecard.getLockStatus())) {
            scorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);
        }

        Scorecard savedScorecard = scorecardService.saveScorecard(scorecard);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Scorecard>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Scorecard saved successfully")
                .data(savedScorecard)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Scorecard>> update(@PathVariable long id, @RequestBody Scorecard payload) {
        Scorecard scorecard = scorecardService.getScorecardById(id);
        if (scorecard == null) {
            throw new ResourceNotFoundException("Scorecard not found with id " + id);
        }

        if (payload.getOwner() != null && payload.getOwner().getId() > 0) {
            Account owner = accountService.getAccountById(payload.getOwner().getId());
            if (owner == null) {
                throw new ResourceNotFoundException("Account not found with id " + payload.getOwner().getId());
            }
            scorecard.setOwner(owner);
        }

        if (payload.getReportingPeriod() != null && payload.getReportingPeriod().getId() > 0) {
            ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(payload.getReportingPeriod().getId());
            if (reportingPeriod == null) {
                throw new ResourceNotFoundException("Reporting period not found with id " + payload.getReportingPeriod().getId());
            }
            scorecard.setReportingPeriod(reportingPeriod);
        }

        if (payload.getScorecardModel() != null && payload.getScorecardModel().getId() > 0) {
            ScorecardModel scorecardModel = scorecardModelService.getScorecardModelById(payload.getScorecardModel().getId());
            if (scorecardModel == null) {
                throw new ResourceNotFoundException("Scorecard model not found with id " + payload.getScorecardModel().getId());
            }
            scorecard.setScorecardModel(scorecardModel);
        }

        if (hasText(payload.getStatus())) {
            scorecard.setStatus(payload.getStatus());
        }
        if (hasText(payload.getApprovalStatus())) {
            scorecard.setApprovalStatus(payload.getApprovalStatus());
        }
        if (hasText(payload.getLockStatus())) {
            scorecard.setLockStatus(payload.getLockStatus());
        }

        scorecard.setOwnerComment(payload.getOwnerComment());
        scorecard.setSupervisorComment(payload.getSupervisorComment());
        scorecard.setModeratorComment(payload.getModeratorComment());

        Scorecard updatedScorecard = scorecardService.saveScorecard(scorecard);
        return ResponseEntity.ok(CommonResponse.<Scorecard>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Scorecard updated successfully")
                .data(updatedScorecard)
                .build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CommonResponse<Scorecard>> updateStatus(@PathVariable long id, @RequestBody StatusUpdateWrapper wrapper) {
        return updateSingleStatusField(id, wrapper, "status");
    }

    @PutMapping("/{id}/approval-status")
    public ResponseEntity<CommonResponse<Scorecard>> updateApprovalStatus(@PathVariable long id, @RequestBody StatusUpdateWrapper wrapper) {
        return updateSingleStatusField(id, wrapper, "approvalStatus");
    }

    @PutMapping("/{id}/lock-status")
    public ResponseEntity<CommonResponse<Scorecard>> updateLockStatus(@PathVariable long id, @RequestBody StatusUpdateWrapper wrapper) {
        return updateSingleStatusField(id, wrapper, "lockStatus");
    }

    @GetMapping("/{id}/goals")
    public ResponseEntity<CommonResponse<List<Goal>>> listGoals(@PathVariable long id) {
        ensureScorecardExists(id);
        List<Goal> goals = goalService.listAllGoals(id);
        return ResponseEntity.ok(CommonResponse.<List<Goal>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Goals retrieved successfully")
                .data(goals)
                .build());
    }

    @GetMapping("/{id}/targets")
    public ResponseEntity<CommonResponse<List<Target>>> listTargets(@PathVariable long id) {
        ensureScorecardExists(id);
        List<Target> targets = targetService.getAllTargetsByScorecard(id);
        return ResponseEntity.ok(CommonResponse.<List<Target>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Targets retrieved successfully")
                .data(targets)
                .build());
    }

    @PostMapping("/{id}/targets")
    public ResponseEntity<CommonResponse<Target>> saveTarget(@PathVariable long id, @RequestBody GoalWrapper goalWrapper) {
        ensureScorecardExists(id);

        Goal goal;
        if (goalWrapper.getGoalId() < 1) {
            goal = new Goal();
        } else {
            goal = goalService.getGoalById(goalWrapper.getGoalId());
            if (goal == null) {
                throw new ResourceNotFoundException("Goal not found with id " + goalWrapper.getGoalId());
            }
            if (goal.getScorecardId() != id) {
                throw new BadRequestException("Goal does not belong to scorecard " + id);
            }
        }

        goal.setScorecardId(id);
        goal.setPerspective(goalWrapper.getPerspective());
        goal.setStrategicObjective(goalWrapper.getStrategicObjective());
        goal.setName(goalWrapper.getGoalName());
        Goal savedGoal = goalService.saveGoal(goal);

        Target target;
        if (goalWrapper.getTargetId() < 1) {
            target = new Target();
        } else {
            target = targetService.getTargetById(goalWrapper.getTargetId());
            if (target == null) {
                throw new ResourceNotFoundException("Target not found with id " + goalWrapper.getTargetId());
            }
        }

        target.setGoal(savedGoal);
        target.setPerspective(savedGoal.getPerspective());
        target.setStrategicObjective(savedGoal.getStrategicObjective());
        target.setMeasure(goalWrapper.getMeasure());
        target.setUnit(goalWrapper.getUnit());
        target.setAllocatedWeight(goalWrapper.getAllocatedWeight());
        target.setNormalTarget(goalWrapper.getNormalTarget());
        target.setBaseTarget(goalWrapper.getBaseTarget());
        target.setStretchTarget(goalWrapper.getStretchTarget());

        Target savedTarget = targetService.saveTarget(target);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Target>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Target saved successfully")
                .data(savedTarget)
                .build());
    }

    @PostMapping("/{id}/targets/existing-goal")
    public ResponseEntity<CommonResponse<Target>> saveTargetToExistingGoal(@PathVariable long id, @RequestBody Target target) {
        ensureScorecardExists(id);
        if (target.getGoal() == null || target.getGoal().getId() < 1) {
            throw new BadRequestException("Goal id is required");
        }

        Goal goal = goalService.getGoalById(target.getGoal().getId());
        if (goal == null) {
            throw new ResourceNotFoundException("Goal not found with id " + target.getGoal().getId());
        }
        if (goal.getScorecardId() != id) {
            throw new BadRequestException("Goal does not belong to scorecard " + id);
        }

        target.setGoal(goal);
        target.setPerspective(goal.getPerspective());
        target.setStrategicObjective(goal.getStrategicObjective());

        Target savedTarget = targetService.saveTarget(target);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Target>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Target saved successfully")
                .data(savedTarget)
                .build());
    }

    @DeleteMapping("/targets/{targetId}")
    public ResponseEntity<CommonResponse<Void>> deleteTarget(@PathVariable long targetId) {
        Target target = targetService.getTargetById(targetId);
        if (target == null) {
            throw new ResourceNotFoundException("Target not found with id " + targetId);
        }

        Goal goal = target.getGoal() == null ? null : goalService.getGoalById(target.getGoal().getId());
        targetService.deleteTarget(target);
        if (goal != null && !targetService.checkIfGoalHasTargets(goal)) {
            goalService.deleteGoal(goal);
        }

        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Target deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping("/targets/{targetId}/comments")
    public ResponseEntity<CommonResponse<List<Comment>>> listTargetComments(@PathVariable long targetId) {
        Target target = targetService.getTargetById(targetId);
        if (target == null) {
            throw new ResourceNotFoundException("Target not found with id " + targetId);
        }
        if (target.getGoal() == null) {
            throw new BadRequestException("Target is missing an associated goal");
        }

        List<Comment> comments = commentService.getCommentsByGoalId(target.getGoal().getId());
        List<Comment> targetComments = new ArrayList<>();
        for (Comment comment : comments) {
            if (comment != null && comment.getTarget() != null && comment.getTarget().getId() == targetId) {
                targetComments.add(comment);
            }
        }

        return ResponseEntity.ok(CommonResponse.<List<Comment>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Comments retrieved successfully")
                .data(targetComments)
                .build());
    }

    @PostMapping("/targets/{targetId}/comments")
    public ResponseEntity<CommonResponse<Comment>> saveTargetComment(@PathVariable long targetId, @RequestBody Comment comment) {
        Target target = targetService.getTargetById(targetId);
        if (target == null) {
            throw new ResourceNotFoundException("Target not found with id " + targetId);
        }

        Account sender = resolveCommentSender(comment);
        comment.setTarget(target);
        comment.setSender(sender);
        commentService.saveComment(comment);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Comment>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Comment saved successfully")
                .data(comment)
                .build());
    }

    @PostMapping("/scores/standard")
    public ResponseEntity<CommonResponse<Score>> saveStandardScore(@RequestBody Score score) {
        normalizeScoreReferences(score);
        Score savedScore = standardScorecardScoreService.saveScore(score);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Score>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Standard score saved successfully")
                .data(savedScore)
                .build());
    }

    @PostMapping("/scores/value-based/employee")
    public ResponseEntity<CommonResponse<Score>> saveValueBasedEmployeeScore(@RequestBody Score score) {
        normalizeScoreReferences(score);
        Score savedScore = valueBasedScoreService.saveEmployeeScore(score);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Score>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Employee score saved successfully")
                .data(savedScore)
                .build());
    }

    @PostMapping("/scores/value-based/evidence")
    public ResponseEntity<CommonResponse<Score>> saveValueBasedEvidence(@RequestBody Score score) {
        normalizeScoreReferences(score);
        Score savedScore = valueBasedScoreService.saveEvidence(score);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Score>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Evidence saved successfully")
                .data(savedScore)
                .build());
    }

    @PostMapping("/scores/value-based/manager")
    public ResponseEntity<CommonResponse<Score>> saveValueBasedManagerScore(@RequestBody Score score) {
        normalizeScoreReferences(score);
        Score savedScore = valueBasedScoreService.saveManagerScore(score);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Score>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Manager score saved successfully")
                .data(savedScore)
                .build());
    }

    @PostMapping("/scores/value-based/agreed")
    public ResponseEntity<CommonResponse<Score>> saveValueBasedAgreedScore(@RequestBody Score score) {
        normalizeScoreReferences(score);
        Score savedScore = valueBasedScoreService.saveAgreedScore(score);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Score>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Agreed score saved successfully")
                .data(savedScore)
                .build());
    }

    @PostMapping("/scores/value-based/moderated")
    public ResponseEntity<CommonResponse<Score>> saveValueBasedModeratedScore(@RequestBody Score score) {
        normalizeScoreReferences(score);
        Score savedScore = valueBasedScoreService.saveModeratedScore(score);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Score>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Moderated score saved successfully")
                .data(savedScore)
                .build());
    }

    private void ensureScorecardExists(long scorecardId) {
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        if (scorecard == null) {
            throw new ResourceNotFoundException("Scorecard not found with id " + scorecardId);
        }
    }

    private void normalizeScorecardReferences(Scorecard scorecard) {
        if (scorecard.getOwner() != null && scorecard.getOwner().getId() > 0) {
            Account owner = accountService.getAccountById(scorecard.getOwner().getId());
            if (owner == null) {
                throw new ResourceNotFoundException("Account not found with id " + scorecard.getOwner().getId());
            }
            scorecard.setOwner(owner);
        }

        if (scorecard.getReportingPeriod() != null && scorecard.getReportingPeriod().getId() > 0) {
            ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(scorecard.getReportingPeriod().getId());
            if (reportingPeriod == null) {
                throw new ResourceNotFoundException("Reporting period not found with id " + scorecard.getReportingPeriod().getId());
            }
            scorecard.setReportingPeriod(reportingPeriod);
        }

        if (scorecard.getScorecardModel() != null && scorecard.getScorecardModel().getId() > 0) {
            ScorecardModel scorecardModel = scorecardModelService.getScorecardModelById(scorecard.getScorecardModel().getId());
            if (scorecardModel == null) {
                throw new ResourceNotFoundException("Scorecard model not found with id " + scorecard.getScorecardModel().getId());
            }
            scorecard.setScorecardModel(scorecardModel);
        }
    }

    private Account resolveCommentSender(Comment comment) {
        if (comment.getSender() != null && comment.getSender().getId() > 0) {
            Account sender = accountService.getAccountById(comment.getSender().getId());
            if (sender == null) {
                throw new ResourceNotFoundException("Account not found with id " + comment.getSender().getId());
            }
            return sender;
        }

        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser == null) {
            throw new BadRequestException("Sender is required");
        }
        return loggedUser;
    }

    private void normalizeScoreReferences(Score score) {
        if (score == null || score.getTarget() == null || score.getTarget().getId() < 1) {
            throw new BadRequestException("A valid target id is required");
        }

        Target target = targetService.getTargetById(score.getTarget().getId());
        if (target == null) {
            throw new ResourceNotFoundException("Target not found with id " + score.getTarget().getId());
        }
        score.setTarget(target);

        ReportingDate reportingDate;
        if (score.getReportingDate() != null && score.getReportingDate().getId() > 0) {
            reportingDate = reportingDateService.getReportingDateById(score.getReportingDate().getId());
        } else {
            reportingDate = reportingDateService.getActiveReportingDate();
        }

        if (reportingDate == null) {
            throw new BadRequestException("A valid reporting date is required");
        }
        if (!reportingDateService.isReportingDateOpen(reportingDate)) {
            throw new BadRequestException("Scores can only be captured for an open reporting date");
        }
        score.setReportingDate(reportingDate);
    }

    private ResponseEntity<CommonResponse<Scorecard>> updateSingleStatusField(
            long id,
            StatusUpdateWrapper wrapper,
            String field) {
        Scorecard scorecard = scorecardService.getScorecardById(id);
        if (scorecard == null) {
            throw new ResourceNotFoundException("Scorecard not found with id " + id);
        }
        if (wrapper == null || !hasText(wrapper.getStatus())) {
            throw new BadRequestException("Status value is required");
        }

        if ("status".equals(field)) {
            scorecard.setStatus(wrapper.getStatus().trim());
        } else if ("approvalStatus".equals(field)) {
            scorecard.setApprovalStatus(wrapper.getStatus().trim());
        } else if ("lockStatus".equals(field)) {
            scorecard.setLockStatus(wrapper.getStatus().trim());
        }

        Scorecard updatedScorecard = scorecardService.saveScorecard(scorecard);
        return ResponseEntity.ok(CommonResponse.<Scorecard>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Scorecard updated successfully")
                .data(updatedScorecard)
                .build());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
