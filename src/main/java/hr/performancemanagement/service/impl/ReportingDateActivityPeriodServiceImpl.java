package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingDateActivityPeriod;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.ScorecardReportingDateStage;
import hr.performancemanagement.repository.ReportingDateActivityPeriodRepository;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.repository.ScoreCardRepository;
import hr.performancemanagement.repository.ScorecardReportingDateStageRepository;
import hr.performancemanagement.service.api.NotificationService;
import hr.performancemanagement.service.api.ReportingDateActivityPeriodService;
import hr.performancemanagement.service.api.SystemSettingService;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.dto.ActivityPeriodProgress;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ReportingDateActivityPeriodServiceImpl implements ReportingDateActivityPeriodService {

    private final ReportingDateActivityPeriodRepository activityPeriodRepository;
    private final ReportingDateRepository reportingDateRepository;
    private final ScoreCardRepository scoreCardRepository;
    private final ScorecardReportingDateStageRepository scorecardReportingDateStageRepository;
    private final NotificationService notificationService;
    private final SystemSettingService systemSettingService;

    public ReportingDateActivityPeriodServiceImpl(ReportingDateActivityPeriodRepository activityPeriodRepository,
                                                  ReportingDateRepository reportingDateRepository,
                                                  ScoreCardRepository scoreCardRepository,
                                                  ScorecardReportingDateStageRepository scorecardReportingDateStageRepository,
                                                  NotificationService notificationService,
                                                  SystemSettingService systemSettingService) {
        this.activityPeriodRepository = activityPeriodRepository;
        this.reportingDateRepository = reportingDateRepository;
        this.scoreCardRepository = scoreCardRepository;
        this.scorecardReportingDateStageRepository = scorecardReportingDateStageRepository;
        this.notificationService = notificationService;
        this.systemSettingService = systemSettingService;
    }

    @Override
    public ReportingDateActivityPeriod getActivityPeriodById(long id) {
        return activityPeriodRepository.findReportingDateActivityPeriodById(id);
    }

    @Override
    public List<ReportingDateActivityPeriod> listActivityPeriods(ReportingDate reportingDate) {
        if (reportingDate == null || reportingDate.getId() <= 0) {
            return Collections.emptyList();
        }
        return activityPeriodRepository.findReportingDateActivityPeriodsByReportingDateOrderByStartDateAscEndDateAsc(reportingDate);
    }

    @Override
    public ReportingDateActivityPeriod getCurrentActivityPeriod(ReportingDate reportingDate) {
        return findCurrentActivityPeriod(listActivityPeriods(reportingDate));
    }

    @Override
    public boolean hasConfiguredActivityPeriods(ReportingDate reportingDate) {
        return reportingDate != null
                && reportingDate.getId() > 0
                && activityPeriodRepository.existsReportingDateActivityPeriodByReportingDate(reportingDate);
    }

    @Override
    public boolean isActivityAllowed(ReportingDate reportingDate, String systemActivity) {
        if (!hasConfiguredActivityPeriods(reportingDate)) {
            return true;
        }
        ReportingDateActivityPeriod currentPeriod = getCurrentActivityPeriod(reportingDate);
        return currentPeriod != null && activityMatches(currentPeriod.getActivityType(), systemActivity);
    }

    @Override
    public String getCurrentActivityLabel(ReportingDate reportingDate) {
        List<ReportingDateActivityPeriod> periods = listActivityPeriods(reportingDate);
        if (periods.isEmpty()) {
            return "Not configured";
        }
        ReportingDateActivityPeriod current = findCurrentActivityPeriod(periods);
        if (current != null) {
            return activityLabel(current.getActivityType());
        }
        return activityPhaseLabel(periods);
    }

    @Override
    public String getCurrentActivitySummary(ReportingDate reportingDate) {
        List<ReportingDateActivityPeriod> periods = listActivityPeriods(reportingDate);
        if (periods.isEmpty()) {
            return "";
        }
        ReportingDateActivityPeriod current = findCurrentActivityPeriod(periods);
        if (current != null) {
            return activityLabel(current.getActivityType())
                    + " | Period: " + current.getStartDate() + " to " + current.getEndDate()
                    + " | Cutoff: " + current.getEndDate();
        }
        return activityPhaseLabel(periods) + " | Actions are restricted until a configured activity period is active.";
    }

    private String activityPhaseLabel(List<ReportingDateActivityPeriod> periods) {
        LocalDate today = LocalDate.now();
        LocalDate firstStart = parseDate(periods.get(0).getStartDate(), "Activity start");
        LocalDate lastEnd = parseDate(periods.get(periods.size() - 1).getEndDate(), "Activity last");
        if (today.isBefore(firstStart)) {
            return "Scheduled";
        }
        if (today.isAfter(lastEnd)) {
            return "Completed";
        }
        return "Between activity periods";
    }

    private ReportingDateActivityPeriod findCurrentActivityPeriod(List<ReportingDateActivityPeriod> periods) {
        LocalDate today = LocalDate.now();
        for (ReportingDateActivityPeriod period : periods) {
            LocalDate startDate = parseDate(period.getStartDate(), "Activity start");
            LocalDate endDate = parseDate(period.getEndDate(), "Activity last");
            if (!today.isBefore(startDate) && !today.isAfter(endDate)) {
                return period;
            }
        }
        return null;
    }

    @Override
    public ActivityPeriodProgress getProgress(ReportingDate reportingDate) {
        List<ReportingDateActivityPeriod> periods = listActivityPeriods(reportingDate);
        ReportingDateActivityPeriod current = findCurrentActivityPeriod(periods);
        ReportingDateActivityPeriod context = current;
        if (context == null && !periods.isEmpty()) {
            context = resolveNextOrLastPeriod(periods);
        }

        List<Scorecard> scorecards = reportingDate == null || reportingDate.getReportingPeriod() == null
                ? Collections.emptyList()
                : scoreCardRepository.findScorecardsByReportingPeriod(reportingDate.getReportingPeriod());
        if (scorecards == null) {
            scorecards = Collections.emptyList();
        }

        String stageKey = context == null ? null : normalizeActivity(context.getActivityType());
        int completed = countCompletedScorecards(reportingDate, scorecards, stageKey);
        int total = scorecards.size();
        int outstanding = Math.max(0, total - completed);
        String stageLabel = current == null ? getCurrentActivityLabel(reportingDate) : activityLabel(stageKey);
        String message = progressMessage(stageLabel, total, completed, outstanding, current != null);
        return new ActivityPeriodProgress(
                stageKey,
                stageLabel,
                context == null ? null : context.getStartDate(),
                context == null ? null : context.getEndDate(),
                total,
                completed,
                outstanding,
                message,
                !periods.isEmpty(),
                current != null
        );
    }

    @Override
    @Transactional
    public void saveActivityPeriod(ReportingDateActivityPeriod requestedPeriod) {
        if (requestedPeriod == null) {
            throw new IllegalArgumentException("Activity period details are required.");
        }
        ReportingDate reportingDate = resolveReportingDate(requestedPeriod.getReportingDate());
        String activityType = normalizeActivity(requestedPeriod.getActivityType());
        validateActivityType(activityType);
        LocalDate startDate = parseDate(requestedPeriod.getStartDate(), "Activity start");
        LocalDate endDate = parseDate(requestedPeriod.getEndDate(), "Activity last");
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Activity start date cannot be after its last date.");
        }

        ReportingDateActivityPeriod period = requestedPeriod;
        if (requestedPeriod.getId() > 0) {
            period = activityPeriodRepository.findReportingDateActivityPeriodById(requestedPeriod.getId());
            if (period == null) {
                throw new IllegalArgumentException("Activity period could not be found.");
            }
            if (!Objects.equals(normalizeActivity(period.getActivityType()), activityType)
                    || !Objects.equals(period.getStartDate(), startDate.toString())
                    || !Objects.equals(period.getEndDate(), endDate.toString())) {
                period.setLastReminderDate(null);
            }
        }
        period.setReportingDate(reportingDate);
        period.setActivityType(activityType);
        period.setStartDate(startDate.toString());
        period.setEndDate(endDate.toString());
        validateSchedule(period);
        ReportingDateActivityPeriod saved = activityPeriodRepository.save(period);
        requestedPeriod.setId(saved.getId());
        requestedPeriod.setReportingDate(saved.getReportingDate());
        requestedPeriod.setActivityType(saved.getActivityType());
        requestedPeriod.setStartDate(saved.getStartDate());
        requestedPeriod.setEndDate(saved.getEndDate());
    }

    @Override
    @Transactional
    public void deleteActivityPeriod(ReportingDateActivityPeriod activityPeriod) {
        if (activityPeriod == null || activityPeriod.getId() <= 0) {
            throw new IllegalArgumentException("Activity period is required.");
        }
        ReportingDateActivityPeriod resolved = activityPeriodRepository.findReportingDateActivityPeriodById(activityPeriod.getId());
        if (resolved == null) {
            throw new IllegalArgumentException("Activity period could not be found.");
        }
        activityPeriodRepository.delete(resolved);
    }

    @Override
    @Transactional
    public void deleteActivityPeriods(ReportingDate reportingDate) {
        if (reportingDate != null && reportingDate.getId() > 0) {
            activityPeriodRepository.deleteReportingDateActivityPeriodsByReportingDate(reportingDate);
        }
    }

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void sendActivityPeriodNotices() {
        List<ReportingDate> openDates = reportingDateRepository.findReportingDatesByStatusIn(
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        );
        if (openDates == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (ReportingDate reportingDate : openDates) {
            for (ReportingDateActivityPeriod period : listActivityPeriods(reportingDate)) {
                LocalDate start = parseDate(period.getStartDate(), "Activity start");
                LocalDate cutoff = parseDate(period.getEndDate(), "Activity last");
                if (!today.equals(start) && !today.equals(cutoff)) {
                    continue;
                }
                if (today.toString().equals(period.getLastReminderDate())) {
                    continue;
                }
                ActivityPeriodProgress progress = progressForPeriod(reportingDate, period);
                sendHREmail(reportingDate, period, progress, today.equals(cutoff));
                period.setLastReminderDate(today.toString());
                activityPeriodRepository.save(period);
            }
        }
    }

    private ActivityPeriodProgress progressForPeriod(ReportingDate reportingDate, ReportingDateActivityPeriod period) {
        List<Scorecard> scorecards = reportingDate == null || reportingDate.getReportingPeriod() == null
                ? Collections.emptyList()
                : scoreCardRepository.findScorecardsByReportingPeriod(reportingDate.getReportingPeriod());
        if (scorecards == null) {
            scorecards = Collections.emptyList();
        }
        int completed = countCompletedScorecards(reportingDate, scorecards, period.getActivityType());
        int outstanding = Math.max(0, scorecards.size() - completed);
        return new ActivityPeriodProgress(
                normalizeActivity(period.getActivityType()),
                activityLabel(period.getActivityType()),
                period.getStartDate(),
                period.getEndDate(),
                scorecards.size(),
                completed,
                outstanding,
                progressMessage(activityLabel(period.getActivityType()), scorecards.size(), completed, outstanding, true),
                true,
                true
        );
    }

    private void sendHREmail(ReportingDate reportingDate,
                             ReportingDateActivityPeriod period,
                             ActivityPeriodProgress progress,
                             boolean cutoffNotice) {
        String hrEmail = systemSettingService.getHREmail();
        if (!StringUtils.hasText(hrEmail)) {
            return;
        }
        String subject = cutoffNotice
                ? activityLabel(period.getActivityType()) + " cutoff is today"
                : activityLabel(period.getActivityType()) + " activity period has started";
        String message = "Reporting date: " + (reportingDate == null ? "N/A" : reportingDate.getEndDate()) + "\n"
                + "Activity: " + activityLabel(period.getActivityType()) + "\n"
                + "Activity period: " + period.getStartDate() + " to " + period.getEndDate() + "\n"
                + "Cutoff date: " + period.getEndDate() + "\n"
                + "Progress: " + progress.getCompletedScorecards() + " of " + progress.getTotalScorecards() + " scorecards complete\n"
                + "Outstanding: " + progress.getOutstandingScorecards() + "\n"
                + (cutoffNotice
                ? "Previous-stage actions will be restricted after this cutoff."
                : "Only actions belonging to this activity period are now allowed.")
                + "\nLink: /reporting-periods/reporting-dates/"
                + (reportingDate == null || reportingDate.getReportingPeriod() == null
                ? ""
                : reportingDate.getReportingPeriod().getId());
        notificationService.sendUserMessageAsync(hrEmail.trim(), "HR", subject, message);
    }

    private void validateSchedule(ReportingDateActivityPeriod candidate) {
        List<ReportingDateActivityPeriod> periods = new ArrayList<ReportingDateActivityPeriod>(listActivityPeriods(candidate.getReportingDate()));
        periods.removeIf(period -> candidate.getId() > 0 && period.getId() == candidate.getId());
        periods.add(candidate);

        Set<String> activities = new HashSet<String>();
        for (ReportingDateActivityPeriod period : periods) {
            String activity = normalizeActivity(period.getActivityType());
            if (!activities.add(activity)) {
                throw new IllegalArgumentException(activityLabel(activity) + " already has an activity period for this reporting date.");
            }
        }

        periods.sort(Comparator.comparing(period -> parseDate(period.getStartDate(), "Activity start")));
        LocalDate previousEnd = null;
        int previousOrder = 0;
        for (ReportingDateActivityPeriod period : periods) {
            LocalDate start = parseDate(period.getStartDate(), "Activity start");
            LocalDate end = parseDate(period.getEndDate(), "Activity last");
            if (previousEnd != null && !start.isAfter(previousEnd)) {
                throw new IllegalArgumentException("Activity periods cannot overlap. The next activity must start after the previous activity's cutoff date.");
            }
            int currentOrder = activityOrder(period.getActivityType());
            if (currentOrder <= previousOrder) {
                throw new IllegalArgumentException("Activity periods must follow this order: Target Capture, Score Capture, then Moderation.");
            }
            previousEnd = end;
            previousOrder = currentOrder;
        }
    }

    private int countCompletedScorecards(ReportingDate reportingDate, List<Scorecard> scorecards, String activityType) {
        String activity = normalizeActivity(activityType);
        if (scorecards == null || scorecards.isEmpty() || activity == null) {
            return 0;
        }
        if (PMConstants.REPORTING_ACTIVITY_TARGET_CAPTURE.equals(activity)) {
            int completed = 0;
            for (Scorecard scorecard : scorecards) {
                if (isTargetStageComplete(scorecard)) {
                    completed++;
                }
            }
            return completed;
        }

        Map<Long, String> rolesByScorecardId = reportingDateStageRoles(reportingDate);
        int completed = 0;
        for (Scorecard scorecard : scorecards) {
            String role = scorecard == null ? null : rolesByScorecardId.get(scorecard.getId());
            if (PMConstants.REPORTING_ACTIVITY_SCORE_CAPTURE.equals(activity)
                    && (PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING.equals(role)
                    || PMConstants.SCORECARD_STAGE_CLOSED.equals(role))) {
                completed++;
            } else if (PMConstants.REPORTING_ACTIVITY_MODERATION.equals(activity)
                    && PMConstants.SCORECARD_STAGE_CLOSED.equals(role)) {
                completed++;
            }
        }
        return completed;
    }

    private Map<Long, String> reportingDateStageRoles(ReportingDate reportingDate) {
        Map<Long, String> roles = new HashMap<Long, String>();
        if (reportingDate == null) {
            return roles;
        }
        List<ScorecardReportingDateStage> stages =
                scorecardReportingDateStageRepository.findScorecardReportingDateStagesByReportingDate(reportingDate);
        if (stages == null) {
            return roles;
        }
        for (ScorecardReportingDateStage stage : stages) {
            if (stage != null && stage.getScorecard() != null && stage.getApprovalStage() != null) {
                roles.put(stage.getScorecard().getId(), normalizeActivity(stage.getApprovalStage().getRoleKey()));
            }
        }
        return roles;
    }

    private boolean isTargetStageComplete(Scorecard scorecard) {
        if (scorecard == null) {
            return false;
        }
        String role = scorecard.getApprovalStage() == null ? null : normalizeActivity(scorecard.getApprovalStage().getRoleKey());
        if (role != null) {
            return PMConstants.SCORECARD_STAGE_OWNER_SCORING.equals(role)
                    || PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL.equals(role)
                    || PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING.equals(role)
                    || PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING.equals(role)
                    || PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL.equals(role)
                    || PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING.equals(role)
                    || PMConstants.SCORECARD_STAGE_CLOSED.equals(role);
        }
        String status = normalizeActivity(scorecard.getApprovalStatus());
        return PMConstants.APPROVAL_STATUS_APPROVED_BY_HR.equals(status)
                || PMConstants.APPROVAL_STATUS_SCORED_BY_EMPLOYEE.equals(status)
                || PMConstants.APPROVAL_STATUS_APPROVED_OWNER_SCORES.equals(status)
                || PMConstants.APPROVAL_STATUS_SCORED_BY_SUPERVISOR.equals(status)
                || PMConstants.APPROVAL_STATUS_AGREED_BY_TWO.equals(status)
                || PMConstants.APPROVAL_STATUS_APPROVED_AGREED_SCORES.equals(status)
                || PMConstants.APPROVAL_STATUS_MODERATED_BY_HR.equals(status)
                || PMConstants.APPROVAL_STATUS_CLOSED.equals(status);
    }

    private boolean activityMatches(String configuredActivity, String systemActivity) {
        String configured = normalizeActivity(configuredActivity);
        String activity = normalizeActivity(systemActivity);
        if (configured == null || activity == null) {
            return false;
        }
        if (PMConstants.REPORTING_ACTIVITY_TARGET_CAPTURE.equals(configured)) {
            return PMConstants.ACTIVITY_CAPTURE_TARGETS.equals(activity)
                    || PMConstants.ACTIVITY_APPROVE_SCORECARD.equals(activity);
        }
        if (PMConstants.REPORTING_ACTIVITY_SCORE_CAPTURE.equals(configured)) {
            return PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES.equals(activity)
                    || PMConstants.ACTIVITY_APPROVE_OWNER_SCORES.equals(activity)
                    || PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES.equals(activity)
                    || PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES.equals(activity);
        }
        if (PMConstants.REPORTING_ACTIVITY_MODERATION.equals(configured)) {
            return PMConstants.ACTIVITY_APPROVE_AGREED_SCORES.equals(activity)
                    || PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES.equals(activity)
                    || PMConstants.ACTIVITY_CLOSE_SCORECARD.equals(activity);
        }
        return false;
    }

    private ReportingDateActivityPeriod resolveNextOrLastPeriod(List<ReportingDateActivityPeriod> periods) {
        LocalDate today = LocalDate.now();
        for (ReportingDateActivityPeriod period : periods) {
            if (today.isBefore(parseDate(period.getStartDate(), "Activity start"))) {
                return period;
            }
        }
        return periods.get(periods.size() - 1);
    }

    private String progressMessage(String stageLabel, int total, int completed, int outstanding, boolean active) {
        if (total == 0) {
            return "No scorecards exist for this reporting period.";
        }
        if (outstanding == 0) {
            return "All " + total + " scorecards have completed " + stageLabel + ".";
        }
        return completed + " of " + total + " scorecards have completed " + stageLabel
                + "; " + outstanding + " remain outstanding"
                + (active ? " before the cutoff." : ".");
    }

    private ReportingDate resolveReportingDate(ReportingDate reportingDate) {
        if (reportingDate == null || reportingDate.getId() <= 0) {
            throw new IllegalArgumentException("Reporting date is required for an activity period.");
        }
        ReportingDate resolved = reportingDateRepository.findReportingDateById(reportingDate.getId());
        if (resolved == null) {
            throw new IllegalArgumentException("Reporting date could not be found.");
        }
        return resolved;
    }

    private void validateActivityType(String activityType) {
        if (!PMConstants.REPORTING_ACTIVITY_TARGET_CAPTURE.equals(activityType)
                && !PMConstants.REPORTING_ACTIVITY_SCORE_CAPTURE.equals(activityType)
                && !PMConstants.REPORTING_ACTIVITY_MODERATION.equals(activityType)) {
            throw new IllegalArgumentException("Activity must be Target Capture, Score Capture, or Moderation.");
        }
    }

    private int activityOrder(String activityType) {
        String activity = normalizeActivity(activityType);
        if (PMConstants.REPORTING_ACTIVITY_TARGET_CAPTURE.equals(activity)) {
            return 1;
        }
        if (PMConstants.REPORTING_ACTIVITY_SCORE_CAPTURE.equals(activity)) {
            return 2;
        }
        if (PMConstants.REPORTING_ACTIVITY_MODERATION.equals(activity)) {
            return 3;
        }
        return Integer.MAX_VALUE;
    }

    private String activityLabel(String activityType) {
        String activity = normalizeActivity(activityType);
        if (PMConstants.REPORTING_ACTIVITY_TARGET_CAPTURE.equals(activity)) {
            return "Target Capture and Approval";
        }
        if (PMConstants.REPORTING_ACTIVITY_SCORE_CAPTURE.equals(activity)) {
            return "Score Capture and Agreement";
        }
        if (PMConstants.REPORTING_ACTIVITY_MODERATION.equals(activity)) {
            return "Moderation";
        }
        return "Not configured";
    }

    private String normalizeActivity(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ENGLISH);
    }

    private LocalDate parseDate(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(label + " date is required.");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(label + " date must be in YYYY-MM-DD format.");
        }
    }
}
