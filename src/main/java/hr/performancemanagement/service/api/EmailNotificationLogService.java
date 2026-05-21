package hr.performancemanagement.service.api;

import hr.performancemanagement.utils.dto.NotificationSummary;
import hr.performancemanagement.utils.dto.PendingActionNotification;

import java.util.List;

public interface EmailNotificationLogService {

    List<PendingActionNotification> getRecentEmailNotificationsForLoggedUser();

    NotificationSummary getRecentEmailNotificationSummaryForLoggedUser(int previewLimit);

    String openEmailNotification(long id);
}
