package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.PendingActionNotificationService;
import hr.performancemanagement.service.api.ScorecardService;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.dto.NotificationSummary;
import hr.performancemanagement.utils.dto.PendingActionNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PendingActionNotificationServiceImpl implements PendingActionNotificationService {
    private final CommonService commonService;
    private final ScorecardService scorecardService;

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

        List<PendingActionNotification> notifications = new ArrayList<PendingActionNotification>();
        List<Scorecard> scorecards = scorecardService.listAllScorecards(loggedUser.getClientId());
        for (Scorecard scorecard : scorecards) {
            PendingActionNotification notification = resolveScorecardNotification(loggedUser, scorecard);
            if (notification != null) {
                notifications.add(notification);
            }
        }

        int safeLimit = Math.max(0, previewLimit);
        List<PendingActionNotification> preview = notifications.stream()
                .sorted(Comparator.comparing(PendingActionNotification::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .collect(Collectors.toList());
        return new NotificationSummary(notifications.size(), preview);
    }

    private PendingActionNotification resolveScorecardNotification(Account loggedUser, Scorecard scorecard) {
        if (scorecard == null || scorecard.getOwner() == null || scorecard.getApprovalStatus() == null) {
            return null;
        }

        Account owner = scorecard.getOwner();
        String status = scorecard.getApprovalStatus().trim().toUpperCase();
        boolean canCaptureTargets = isAllowed(PMConstants.ACTIVITY_CAPTURE_TARGETS, scorecard);
        boolean canApprove = isAllowed(PMConstants.ACTIVITY_APPROVE_SCORECARD, scorecard);
        boolean canCaptureEmployeeScores = isAllowed(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, scorecard);
        boolean canCaptureManagerScores = isAllowed(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES, scorecard);
        boolean canCaptureAgreedScores = isAllowed(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES, scorecard);
        boolean canCaptureModeratedScores = isAllowed(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, scorecard);
        boolean canCloseScorecard = isAllowed(PMConstants.ACTIVITY_CLOSE_SCORECARD, scorecard);

        Date date = scorecard.getLastUpdate() != null ? scorecard.getLastUpdate() : scorecard.getDate();
        String ownerName = owner.getFullName() != null ? owner.getFullName() : "Employee";
        String periodLabel = formatPeriod(scorecard.getReportingPeriod());

        if (canCaptureTargets) {
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

        if (canApprove && PMConstants.APPROVAL_STATUS_PENDING_APPROVAL.equals(status)) {
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

        if (canApprove && PMConstants.APPROVAL_STATUS_APPROVED_BY_SUPERVISOR.equals(status)) {
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

        if (canCaptureEmployeeScores) {
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

        if (canCaptureManagerScores) {
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

        if (canCaptureAgreedScores) {
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

        if (canCaptureModeratedScores) {
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

        if (canCloseScorecard && PMConstants.APPROVAL_STATUS_MODERATED_BY_HR.equals(status)) {
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

    private boolean isAllowed(String activity, Scorecard scorecard) {
        try {
            return commonService.isUserAllowed(activity, scorecard);
        } catch (Exception ignored) {
            return false;
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
}
