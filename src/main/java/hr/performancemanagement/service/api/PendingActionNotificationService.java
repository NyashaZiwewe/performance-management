package hr.performancemanagement.service.api;

import hr.performancemanagement.utils.dto.NotificationSummary;
import hr.performancemanagement.utils.dto.PendingActionNotification;

import java.util.List;

public interface PendingActionNotificationService {

    List<PendingActionNotification> getPendingNotifications();

    NotificationSummary getPendingNotificationSummary(int previewLimit);
}
