package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.ScorecardReportingDateStage;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.PendingActionNotificationService;
import hr.performancemanagement.service.api.ReportingDateService;
import hr.performancemanagement.service.api.ScorecardReportingDateStageService;
import hr.performancemanagement.service.api.ScorecardWorkflowService;
import hr.performancemanagement.service.api.ScorecardService;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.dto.NotificationSummary;
import hr.performancemanagement.utils.dto.PendingActionNotification;
import hr.performancemanagement.utils.dto.ScorecardWorkflowDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PendingActionNotificationServiceImpl implements PendingActionNotificationService {
    private static final long SUMMARY_CACHE_TTL_MS = 15_000L;
    private final CommonService commonService;
    private final ScorecardService scorecardService;
    private final ReportingDateService reportingDateService;
    private final ScorecardReportingDateStageService scorecardReportingDateStageService;
    private final ScorecardWorkflowService scorecardWorkflowService;
    private final Map<String, CacheEntry> summaryCache = new ConcurrentHashMap<>();

    @Override
    public List<PendingActionNotification> getPendingNotifications() {
        return getPendingNotificationSummary(Integer.MAX_VALUE).getPreview();
    }

    @Override
    public NotificationSummary getPendingNotificationSummary(int previewLimit) {
        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser == null) {
            return new NotificationSummary(0, new ArrayList<PendingActionNotification>());
        }
        int safeLimit = Math.max(0, previewLimit);
        String cacheKey = loggedUser.getId() + ":" + safeLimit;
        CacheEntry cachedSummary = summaryCache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cachedSummary != null && cachedSummary.expiresAt > now) {
            return cachedSummary.summary;
        }

        List<PendingActionNotification> notifications = new ArrayList<PendingActionNotification>();
        addReportingDateOperationalNotification(loggedUser, notifications);
        List<Scorecard> scorecards = scorecardService.listActiveScorecards(loggedUser.getClientId());
        for (Scorecard scorecard : scorecards) {
            if (!isPotentiallyActionable(scorecard)) {
                continue;
            }
            PendingActionNotification notification = resolveScorecardNotification(scorecard);
            if (notification != null) {
                notifications.add(notification);
            }
        }

        List<PendingActionNotification> preview = notifications.stream()
                .sorted(Comparator.comparing(PendingActionNotification::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .collect(Collectors.toList());
        NotificationSummary summary = new NotificationSummary(notifications.size(), preview);
        summaryCache.put(cacheKey, new CacheEntry(summary, now + SUMMARY_CACHE_TTL_MS));
        pruneExpiredCacheEntries(now);
        return summary;
    }

    private void addReportingDateOperationalNotification(Account loggedUser, List<PendingActionNotification> notifications) {
        if (loggedUser == null || notifications == null) {
            return;
        }
        boolean hasReportingDateConflict = loggedUser.getClientId() > 0
                && reportingDateService.hasMultipleOpenOrActiveReportingDates(loggedUser.getClientId());
        boolean canOperateDates = commonService.isAdmin() || commonService.hasSpecialRights();

        if (hasReportingDateConflict) {
            if (canOperateDates) {
                notifications.add(new PendingActionNotification(
                        "Reporting Date",
                        "Multiple active reporting dates detected",
                        "Score capture is blocked for all users until exactly one reporting date remains OPEN.",
                        "/reporting-periods",
                        "fa fa-exclamation-triangle",
                        "danger",
                        new Date()
                ));
            } else {
                notifications.add(new PendingActionNotification(
                        "Score Capture",
                        "Capture window paused",
                        "Score capture is temporarily blocked due to reporting date configuration. Contact an administrator.",
                        "/",
                        "fa fa-pause-circle",
                        "warning",
                        new Date()
                ));
            }
            return;
        }

        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        boolean open = reportingDateService.isReportingDateOpen(reportingDate)
                || (reportingDate != null
                && reportingDate.getStatus() != null
                && PMConstants.STATUS_ACTIVE.equalsIgnoreCase(reportingDate.getStatus().trim()));

        if (canOperateDates && !open) {
            notifications.add(new PendingActionNotification(
                    "Reporting Date",
                    "No open reporting date",
                    "Score capture is blocked until a reporting date is opened.",
                    "/reporting-periods",
                    "fa fa-calendar-times-o",
                    "danger",
                    new Date()
            ));
            return;
        }

        if (!canOperateDates && !open) {
            notifications.add(new PendingActionNotification(
                    "Score Capture",
                    "Capture window currently closed",
                    "A reporting date must be opened before score capture can continue.",
                    "/",
                    "fa fa-clock-o",
                    "warning",
                    new Date()
            ));
        }
    }

    private PendingActionNotification resolveScorecardNotification(Scorecard scorecard) {
        if (scorecard == null || scorecard.getOwner() == null || scorecard.getApprovalStatus() == null) {
            return null;
        }

        Account owner = scorecard.getOwner();
        String status = scorecard.getApprovalStatus().trim();
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        String contractRole = resolveContractStageRole(scorecard);

        Date date = scorecard.getLastUpdate() != null ? scorecard.getLastUpdate() : scorecard.getDate();
        String ownerName = owner.getFullName() != null ? owner.getFullName() : "Employee";
        String periodLabel = formatPeriod(scorecard.getReportingPeriod());

        if (isTargetCaptureStage(contractRole)
                && isAllowed(PMConstants.ACTIVITY_CAPTURE_TARGETS, scorecard)) {
            return new PendingActionNotification(
                    "Scorecard",
                    "Scorecard ready for submission",
                    ownerName + " • " + periodLabel,
                    "/scorecards/view-scorecard/" + scorecard.getId(),
                    "fa fa-upload",
                    "primary",
                    date
            );
        }

        if (isSupervisorTargetApprovalStage(contractRole)
                && scorecardWorkflowService.matches(status, workflow.getPendingApprovalStatus())
                && isAllowed(PMConstants.ACTIVITY_APPROVE_SCORECARD, scorecard)) {
            return new PendingActionNotification(
                    "Approval",
                    "Scorecard awaiting your approval",
                    ownerName + " • " + periodLabel,
                    "/scorecards/view-scorecard/" + scorecard.getId(),
                    "fa fa-check-circle-o",
                    "primary",
                    date
            );
        }

        if (isHrTargetApprovalStage(contractRole)
                && scorecardWorkflowService.matches(status, workflow.getApprovedBySupervisorStatus())
                && isAllowed(PMConstants.ACTIVITY_APPROVE_SCORECARD, scorecard)) {
            return new PendingActionNotification(
                    "HR Review",
                    "Scorecard awaiting HR approval",
                    ownerName + " • " + periodLabel,
                    "/scorecards/view-scorecard/" + scorecard.getId(),
                    "fa fa-balance-scale",
                    "danger",
                    date
            );
        }

        if (!isContractReadyForScoring(scorecard, workflow)) {
            return null;
        }

        String reportingDateRole = resolveActiveReportingDateRole(scorecard);
        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_OWNER_SCORING)
                && isAllowed(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, scorecard)) {
            return new PendingActionNotification(
                    "Scores",
                    "Employee score submission pending",
                    ownerName + " • " + periodLabel,
                    "/scorecards/capture-scores/" + scorecard.getId(),
                    "fa fa-pencil-square-o",
                    "warning",
                    date
            );
        }

        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL)
                && isAllowed(PMConstants.ACTIVITY_APPROVE_OWNER_SCORES, scorecard)) {
            return new PendingActionNotification(
                    "Scores",
                    "Owner scores awaiting your approval",
                    ownerName + " • " + periodLabel,
                    "/scorecards/view-scorecard/" + scorecard.getId(),
                    "fa fa-check-square-o",
                    "warning",
                    date
            );
        }

        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING)
                && isAllowed(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES, scorecard)) {
            return new PendingActionNotification(
                    "Scores",
                    "Manager score submission pending",
                    ownerName + " • " + periodLabel,
                    "/scorecards/capture-scores/" + scorecard.getId(),
                    "fa fa-line-chart",
                    "warning",
                    date
            );
        }

        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING)
                && isAllowed(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES, scorecard)) {
            return new PendingActionNotification(
                    "Scores",
                    "Agreed score submission pending",
                    ownerName + " • " + periodLabel,
                    "/scorecards/capture-scores/" + scorecard.getId(),
                    "fa fa-exchange",
                    "warning",
                    date
            );
        }

        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL)
                && isAllowed(PMConstants.ACTIVITY_APPROVE_AGREED_SCORES, scorecard)) {
            return new PendingActionNotification(
                    "Moderation",
                    "Agreed scores awaiting your approval",
                    ownerName + " • " + periodLabel,
                    "/scorecards/view-scorecard/" + scorecard.getId(),
                    "fa fa-check-square-o",
                    "danger",
                    date
            );
        }

        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING)
                && isAllowed(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, scorecard)) {
            return new PendingActionNotification(
                    "Scores",
                    "Moderated score submission pending",
                    ownerName + " • " + periodLabel,
                    "/scorecards/capture-scores/" + scorecard.getId(),
                    "fa fa-gavel",
                    "danger",
                    date
            );
        }

        if (scorecardWorkflowService.matches(status, workflow.getModeratedByHrStatus())
                && isAllowed(PMConstants.ACTIVITY_CLOSE_SCORECARD, scorecard)) {
            return new PendingActionNotification(
                    "Scorecard",
                    "Scorecard closure pending",
                    ownerName + " • " + periodLabel,
                    "/scorecards/view-scorecard/" + scorecard.getId(),
                    "fa fa-lock",
                    "danger",
                    date
            );
        }

        return null;
    }

    private String resolveContractStageRole(Scorecard scorecard) {
        if (scorecard == null) {
            return null;
        }
        if (scorecard.getApprovalStage() != null && hasText(scorecard.getApprovalStage().getRoleKey())) {
            return normalizeRole(scorecard.getApprovalStage().getRoleKey());
        }
        return mapContractStatusToRole(scorecard.getApprovalStatus());
    }

    private String mapContractStatusToRole(String approvalStatus) {
        if (!hasText(approvalStatus)) {
            return PMConstants.SCORECARD_STAGE_NEW;
        }
        switch (approvalStatus.trim().toUpperCase(Locale.ENGLISH)) {
            case PMConstants.APPROVAL_STATUS_NEW:
                return PMConstants.SCORECARD_STAGE_NEW;
            case PMConstants.APPROVAL_STATUS_PENDING_APPROVAL:
            case PMConstants.APPROVAL_STATUS_REJECTED_BY_HR:
                return PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR;
            case PMConstants.APPROVAL_STATUS_APPROVED_BY_SUPERVISOR:
                return PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR;
            case PMConstants.APPROVAL_STATUS_REJECTED_BY_SUPERVISOR:
                return PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS;
            case PMConstants.APPROVAL_STATUS_APPROVED_BY_HR:
                return PMConstants.SCORECARD_STAGE_OWNER_SCORING;
            default:
                return null;
        }
    }

    private String resolveActiveReportingDateRole(Scorecard scorecard) {
        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        if (scorecard == null || reportingDate == null) {
            return null;
        }
        String roleKey = scorecardReportingDateStageService.getCurrentRoleKey(scorecard, reportingDate);
        if (hasText(roleKey)) {
            return normalizeRole(roleKey);
        }
        ScorecardReportingDateStage stage = scorecardReportingDateStageService.getOrCreateStage(scorecard, reportingDate);
        if (stage == null || stage.getApprovalStage() == null || !hasText(stage.getApprovalStage().getRoleKey())) {
            return null;
        }
        return normalizeRole(stage.getApprovalStage().getRoleKey());
    }

    private boolean isTargetCaptureStage(String contractRole) {
        return matchesRole(contractRole, PMConstants.SCORECARD_STAGE_NEW)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS);
    }

    private boolean isSupervisorTargetApprovalStage(String contractRole) {
        return matchesRole(contractRole, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR);
    }

    private boolean isHrTargetApprovalStage(String contractRole) {
        return matchesRole(contractRole, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR);
    }

    private boolean isContractReadyForScoring(Scorecard scorecard, ScorecardWorkflowDefinition workflow) {
        if (scorecard == null || workflow == null) {
            return false;
        }
        String contractRole = resolveContractStageRole(scorecard);
        if (matchesRole(contractRole, PMConstants.SCORECARD_STAGE_OWNER_SCORING)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_CLOSED)) {
            return true;
        }
        String approvalStatus = scorecard.getApprovalStatus();
        return scorecardWorkflowService.matches(approvalStatus, workflow.getApprovedByHrStatus())
                || scorecardWorkflowService.matches(approvalStatus, workflow.getScoredByEmployeeStatus())
                || scorecardWorkflowService.matches(approvalStatus, workflow.getApprovedOwnerScoresStatus())
                || scorecardWorkflowService.matches(approvalStatus, workflow.getScoredBySupervisorStatus())
                || scorecardWorkflowService.matches(approvalStatus, workflow.getAgreedByTwoStatus())
                || scorecardWorkflowService.matches(approvalStatus, workflow.getApprovedAgreedScoresStatus())
                || scorecardWorkflowService.matches(approvalStatus, workflow.getModeratedByHrStatus())
                || scorecardWorkflowService.matches(approvalStatus, workflow.getClosedStatus());
    }

    private boolean matchesRole(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private String normalizeRole(String roleKey) {
        return hasText(roleKey) ? roleKey.trim().toUpperCase(Locale.ENGLISH) : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isAllowed(String activity, Scorecard scorecard) {
        try {
            return commonService.isUserAllowed(activity, scorecard);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isPotentiallyActionable(Scorecard scorecard) {
        if (scorecard == null || scorecard.getApprovalStatus() == null) {
            return false;
        }
        String status = scorecard.getApprovalStatus().trim();
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        for (String candidate : workflow.getOrderedStatuses()) {
            if (scorecardWorkflowService.matches(status, candidate)) {
                return true;
            }
        }
        return false;
    }

    private void pruneExpiredCacheEntries(long now) {
        for (Map.Entry<String, CacheEntry> entry : summaryCache.entrySet()) {
            CacheEntry cacheEntry = entry.getValue();
            if (cacheEntry == null || cacheEntry.expiresAt <= now) {
                summaryCache.remove(entry.getKey());
            }
        }
    }

    private String formatPeriod(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null) {
            return "No reporting period";
        }
        String start = reportingPeriod.getStartDate() != null ? reportingPeriod.getStartDate() : "?";
        String end = reportingPeriod.getEndDate() != null ? reportingPeriod.getEndDate() : "?";
        return start + " to " + end;
    }

    private static class CacheEntry {
        private final NotificationSummary summary;
        private final long expiresAt;

        private CacheEntry(NotificationSummary summary, long expiresAt) {
            this.summary = summary;
            this.expiresAt = expiresAt;
        }
    }
}
