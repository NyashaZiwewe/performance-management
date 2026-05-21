package hr.performancemanagement.utils.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NotificationSummary {
    private final long count;
    private final List<PendingActionNotification> preview;
}
