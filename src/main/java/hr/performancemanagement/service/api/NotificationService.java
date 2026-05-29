package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;

public interface NotificationService {
    boolean sendPasswordReset(Account account, String resetLink);

    boolean sendAccountSetup(Account account, String resetLink);

    void sendAccountSetupAsync(Account account, String resetLink);

    void sendScheduledTaskStatus(String taskName, boolean success, String details);

    void sendDepartmentAssignmentUpdate(Account recipient, String subject, String message);

    boolean sendUserMessage(String recipientEmail, String recipientName, String subject, String message);

    void sendUserMessageAsync(String recipientEmail, String recipientName, String subject, String message);
}
