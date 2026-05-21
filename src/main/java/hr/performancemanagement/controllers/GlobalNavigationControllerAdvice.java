package hr.performancemanagement.controllers;

import hr.performancemanagement.service.api.EmailNotificationLogService;
import hr.performancemanagement.service.api.PendingActionNotificationService;
import hr.performancemanagement.utils.dto.NotificationSummary;
import hr.performancemanagement.utils.dto.PendingActionNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalNavigationControllerAdvice {
    private static final int NOTIFICATION_PREVIEW_LIMIT = 6;

    private final PendingActionNotificationService pendingActionNotificationService;
    private final EmailNotificationLogService emailNotificationLogService;

    @ModelAttribute
    public void populateTopNavigation(Model model, HttpServletRequest request) {
        if (request.getServletPath() != null && request.getServletPath().startsWith("/notifications/email/")) {
            return;
        }

        NotificationSummary pendingSummary =
                pendingActionNotificationService.getPendingNotificationSummary(NOTIFICATION_PREVIEW_LIMIT);
        NotificationSummary emailSummary =
                emailNotificationLogService.getRecentEmailNotificationSummaryForLoggedUser(NOTIFICATION_PREVIEW_LIMIT);
        List<PendingActionNotification> pendingNotifications = pendingSummary.getPreview();
        List<PendingActionNotification> emailNotifications = emailSummary.getPreview();

        model.addAttribute("pendingNotifications", pendingNotifications);
        model.addAttribute("pendingNotificationCount", pendingSummary.getCount());
        model.addAttribute("pendingNotificationPreview", pendingNotifications);
        model.addAttribute("hasPendingNotifications", !pendingNotifications.isEmpty());

        model.addAttribute("emailNotifications", emailNotifications);
        model.addAttribute("emailNotificationCount", emailSummary.getCount());
        model.addAttribute("emailNotificationPreview", emailNotifications);
        model.addAttribute("hasEmailNotifications", !emailNotifications.isEmpty());
    }
}
