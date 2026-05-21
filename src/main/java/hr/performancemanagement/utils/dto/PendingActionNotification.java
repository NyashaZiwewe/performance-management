package hr.performancemanagement.utils.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@Getter
@AllArgsConstructor
public class PendingActionNotification {
    private final String category;
    private final String title;
    private final String description;
    private final String link;
    private final String iconClass;
    private final String accentClass;
    private final Date date;
}
