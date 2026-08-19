package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.EmailNotificationLog;
import hr.performancemanagement.repository.EmailNotificationLogRepository;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.EmailNotificationLogService;
import hr.performancemanagement.utils.dto.NotificationSummary;
import hr.performancemanagement.utils.dto.PendingActionNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailNotificationLogServiceImpl implements EmailNotificationLogService {
    private static final String SEEN_EMAIL_NOTIFICATION_IDS = "seenEmailNotificationIds";

    private final CommonService commonService;
    private final EmailNotificationLogRepository emailNotificationLogRepository;
    private final HttpSession session;

    @Override
    public List<PendingActionNotification> getRecentEmailNotificationsForLoggedUser() {
        return getRecentEmailNotificationSummaryForLoggedUser(20).getPreview();
    }

    @Override
    public NotificationSummary getRecentEmailNotificationSummaryForLoggedUser(int previewLimit) {
        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser == null || loggedUser.getEmail() == null || loggedUser.getEmail().trim().isEmpty()) {
            return new NotificationSummary(0, new ArrayList<PendingActionNotification>());
        }

        List<EmailNotificationLog> emailLogs =
                emailNotificationLogRepository.findTop20ByRecipientEmailOrderByDateDesc(loggedUser.getEmail().trim());

        List<PendingActionNotification> unseenNotifications = emailLogs.stream()
                .filter(log -> !seenEmailNotificationIds().contains(log.getId()))
                .map(log -> {
                    boolean failedEmail = log.getCategory() != null
                            && "Email Failure".equalsIgnoreCase(log.getCategory().trim());
                    return new PendingActionNotification(
                            log.getCategory() != null ? log.getCategory() : "Email",
                            log.getSubject() != null ? log.getSubject() : "Email notification",
                            log.getPreviewText() != null ? log.getPreviewText() : "Email activity",
                            "/notifications/email/" + log.getId() + "/open",
                            failedEmail ? "fa fa-exclamation-triangle" : "fa fa-envelope-o",
                            failedEmail ? "danger" : "info",
                            log.getDate());
                })
                .collect(Collectors.toList());

        int safeLimit = Math.max(0, previewLimit);
        List<PendingActionNotification> preview = unseenNotifications.stream()
                .limit(safeLimit)
                .collect(Collectors.toList());
        return new NotificationSummary(unseenNotifications.size(), preview);
    }

    @Override
    public String openEmailNotification(long id) {
        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser == null || loggedUser.getEmail() == null || loggedUser.getEmail().trim().isEmpty()) {
            return "/";
        }

        EmailNotificationLog log = emailNotificationLogRepository.findById(id).orElse(null);
        if (log == null || log.getRecipientEmail() == null
                || !loggedUser.getEmail().trim().equalsIgnoreCase(log.getRecipientEmail().trim())) {
            return "/";
        }

        List<Long> seenIds = seenEmailNotificationIds();
        if (!seenIds.contains(log.getId())) {
            seenIds.add(log.getId());
            session.setAttribute(SEEN_EMAIL_NOTIFICATION_IDS, seenIds);
        }

        if (log.getActionLink() == null || log.getActionLink().trim().isEmpty()) {
            return "/";
        }
        return normalizeActionLinkForLocalRedirect(log.getActionLink());
    }

    private String normalizeActionLinkForLocalRedirect(String actionLink) {
        String trimmed = actionLink != null ? actionLink.trim() : null;
        if (trimmed == null || trimmed.isEmpty()) {
            return "/";
        }
        if (trimmed.startsWith("/")) {
            return trimmed;
        }
        try {
            URI uri = new URI(trimmed);
            if (uri.getScheme() == null && uri.getHost() == null) {
                return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
            }
            String path = uri.getRawPath();
            if (path == null || path.trim().isEmpty()) {
                return "/";
            }
            StringBuilder localLink = new StringBuilder(path);
            if (uri.getRawQuery() != null) {
                localLink.append("?").append(uri.getRawQuery());
            }
            if (uri.getRawFragment() != null) {
                localLink.append("#").append(uri.getRawFragment());
            }
            return localLink.toString();
        } catch (URISyntaxException exception) {
            return "/";
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> seenEmailNotificationIds() {
        Object value = session.getAttribute(SEEN_EMAIL_NOTIFICATION_IDS);
        if (value instanceof List) {
            return (List<Long>) value;
        }
        List<Long> seenIds = new ArrayList<Long>();
        session.setAttribute(SEEN_EMAIL_NOTIFICATION_IDS, seenIds);
        return seenIds;
    }
}
