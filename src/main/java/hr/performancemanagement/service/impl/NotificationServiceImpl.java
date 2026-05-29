package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.EmailNotificationLog;
import hr.performancemanagement.repository.EmailNotificationLogRepository;
import hr.performancemanagement.service.api.NotificationService;
import hr.performancemanagement.service.api.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final JavaMailSender javaMailSender;
    private final SystemSettingService systemSettingService;
    private final EmailNotificationLogRepository emailNotificationLogRepository;

    @Value("${spring.mail.username:}")
    private String configuredMailFrom;

    @Override
    public boolean sendPasswordReset(Account account, String resetLink) {
        String body = buildMessage(
                greeting(account != null ? account.getFullName() : null),
                "We received a request to reset your account password.",
                "If you did not initiate this request, you can ignore this email.",
                "Reset link: " + resetLink
        );
        return send(account != null ? account.getEmail() : null, "Password Reset", body);
    }

    @Override
    public boolean sendAccountSetup(Account account, String resetLink) {
        String body = buildMessage(
                greeting(account != null ? account.getFullName() : null),
                "Your account has been created.",
                "Use the link below to set your password and activate access.",
                "Setup link: " + resetLink
        );
        return send(account != null ? account.getEmail() : null, "Account Setup", body);
    }

    @Override
    @Async
    public void sendScheduledTaskStatus(String taskName, boolean success, String details) {
        String subject = taskName + " " + (success ? "Succeeded" : "Failed");
        String body = buildMessage(
                greeting("Administrator"),
                "Scheduled task: " + defaultText(taskName, "Unknown Task"),
                "Status: " + (success ? "SUCCESS" : "FAILED"),
                details
        );
        send(systemSettingService.getAdminEmail(), subject, body);
    }

    @Override
    @Async
    public void sendDepartmentAssignmentUpdate(Account recipient, String subject, String message) {
        String body = buildMessage(
                greeting(recipient != null ? recipient.getFullName() : null),
                message
        );
        send(recipient != null ? recipient.getEmail() : null, subject, body);
    }

    @Override
    public boolean sendUserMessage(String recipientEmail, String recipientName, String subject, String message) {
        String body = buildMessage(greeting(recipientName), message);
        return send(recipientEmail, subject, body);
    }

    private boolean send(String to, String subject, String body) {
        if (to == null || to.trim().isEmpty()) {
            return false;
        }

        JavaMailSender activeMailSender = resolveMailSender();
        try {
            MimeMessage message = activeMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            String from = resolveFromAddress(activeMailSender);
            if (from != null && !from.trim().isEmpty()) {
                helper.setFrom(from);
            }
            helper.setSubject(subject);
            helper.setText(buildHtmlEmail(subject, body), true);
            activeMailSender.send(message);
            recordEmailNotification(to, subject, body);
            log.info("Email sent successfully to: {}", to);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
            // Mail delivery is optional; operational flows should continue even when email is unavailable.
            return false;
        }
    }

    private JavaMailSender resolveMailSender() {
        if (!systemSettingService.isMailConfigured()) {
            return javaMailSender;
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(systemSettingService.getMailHost());
        mailSender.setPort(systemSettingService.getMailPort());
        mailSender.setUsername(systemSettingService.getMailUsername());
        mailSender.setPassword(systemSettingService.getMailPassword());

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        return mailSender;
    }

    private String resolveFromAddress(JavaMailSender activeMailSender) {
        String from = systemSettingService.getMailUsername();
        if (from == null || from.trim().isEmpty()) {
            from = configuredMailFrom;
        }
        if ((from == null || from.trim().isEmpty()) && activeMailSender instanceof JavaMailSenderImpl) {
            from = ((JavaMailSenderImpl) activeMailSender).getUsername();
        }
        return from;
    }

    private String buildMessage(String... lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line != null && !line.trim().isEmpty()) {
                builder.append(line).append("\n\n");
            }
        }
        builder.append("Best regards,\n").append(systemSettingService.getSystemName());
        return builder.toString();
    }

    private void recordEmailNotification(String to, String subject, String body) {
        try {
            EmailNotificationLog log = new EmailNotificationLog();
            log.setRecipientEmail(to);
            log.setSubject(subject);
            log.setPreviewText(extractPreviewText(body));
            log.setActionLink(extractActionLink(body));
            log.setCategory(resolveCategory(subject));
            emailNotificationLogRepository.save(log);
        } catch (RuntimeException ignored) {
            // Notification logging should not interrupt operational flows.
        }
    }

    private String buildHtmlEmail(String subject, String body) {
        List<String> paragraphs = new ArrayList<String>();
        List<String> actionLinks = new ArrayList<String>();
        for (String block : body.split("\\n\\n")) {
            String trimmed = block != null ? block.trim() : null;
            if (trimmed == null || trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.contains(": http://") || trimmed.contains(": https://")) {
                int separatorIndex = trimmed.indexOf(": ");
                if (separatorIndex > 0 && separatorIndex + 2 < trimmed.length()) {
                    String label = trimmed.substring(0, separatorIndex).trim();
                    String url = trimmed.substring(separatorIndex + 2).trim();
                    actionLinks.add(buildActionButton(label, url));
                    continue;
                }
            }
            paragraphs.add(trimmed);
        }

        String brandName = escapeHtml(defaultText(systemSettingService.getCompanyName(), systemSettingService.getSystemName()));
        String systemName = escapeHtml(defaultText(systemSettingService.getSystemName(), "Performance Management"));
        StringBuilder contentBuilder = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.contains("\n")) {
                contentBuilder.append("<div style=\"margin:0 0 18px;padding:14px 16px;border-radius:14px;background:#f6f9fc;border:1px solid #d9e6f2;color:#314b63;font-size:14px;line-height:1.6;white-space:pre-line;\">")
                        .append(escapeHtml(paragraph))
                        .append("</div>");
            } else {
                contentBuilder.append("<p style=\"margin:0 0 14px;color:#314b63;font-size:15px;line-height:1.65;\">")
                        .append(escapeHtml(paragraph))
                        .append("</p>");
            }
        }

        StringBuilder actionsBuilder = new StringBuilder();
        for (String actionLink : actionLinks) {
            actionsBuilder.append(actionLink);
        }

        return "<!DOCTYPE html>" +
                "<html><body style=\"margin:0;padding:0;background:#eef4f8;font-family:'Segoe UI',Tahoma,sans-serif;color:#183247;\">" +
                "<div style=\"padding:32px 16px;\">" +
                "<div style=\"max-width:680px;margin:0 auto;background:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 18px 42px rgba(24,50,71,0.12);\">" +
                "<div style=\"padding:28px 32px;background:linear-gradient(135deg,#175ea8 0%,#103d6d 100%);color:#ffffff;\">" +
                "<div style=\"font-size:12px;letter-spacing:0.18em;text-transform:uppercase;opacity:0.8;\">"
                + systemName + "</div>" +
                "<h1 style=\"margin:10px 0 0;font-size:28px;line-height:1.25;font-weight:700;\">" + escapeHtml(subject) + "</h1>" +
                "<div style=\"margin-top:8px;font-size:14px;opacity:0.9;\">Updates from " + brandName + "</div>" +
                "</div>" +
                "<div style=\"padding:30px 32px 22px;\">" +
                contentBuilder +
                (actionsBuilder.length() > 0
                        ? "<div style=\"margin:26px 0 10px;\">" + actionsBuilder + "</div>"
                        : "") +
                "</div>" +
                "<div style=\"padding:18px 32px 28px;border-top:1px solid #e1ebf3;background:#fbfdff;color:#6c7f92;font-size:12px;line-height:1.7;\">" +
                "<div style=\"margin-bottom:6px;font-weight:700;color:#183247;\">" + brandName + "</div>" +
                "<div>This is an automated notification from " + systemName + ".</div>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</body></html>";
    }

    private String buildActionButton(String label, String url) {
        return "<a href=\"" + escapeHtmlAttribute(url) + "\" " +
                "style=\"display:inline-block;margin:0 12px 12px 0;padding:12px 18px;border-radius:999px;" +
                "background:#175ea8;color:#ffffff;text-decoration:none;font-size:14px;font-weight:700;\">" +
                escapeHtml(label) +
                "</a>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeHtmlAttribute(String value) {
        return escapeHtml(value).replace("\n", "").replace("\r", "");
    }

    private String extractPreviewText(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "Email notification";
        }
        String[] blocks = body.split("\\n\\n");
        StringBuilder builder = new StringBuilder();
        for (String block : blocks) {
            String trimmed = block != null ? block.trim() : null;
            if (trimmed == null || trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.contains(": http://") || trimmed.contains(": https://")) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(trimmed.replace('\n', ' '));
            if (builder.length() >= 220) {
                break;
            }
        }
        String preview = builder.toString().trim();
        if (preview.length() > 220) {
            preview = preview.substring(0, 217) + "...";
        }
        return preview.isEmpty() ? "Email notification" : preview;
    }

    private String extractActionLink(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        for (String block : body.split("\\n\\n")) {
            String trimmed = block != null ? block.trim() : null;
            if (trimmed == null || trimmed.isEmpty()) {
                continue;
            }
            int separatorIndex = trimmed.indexOf(": ");
            if (separatorIndex > 0 && separatorIndex + 2 < trimmed.length()) {
                String url = trimmed.substring(separatorIndex + 2).trim();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return url;
                }
            }
        }
        return null;
    }

    private String resolveCategory(String subject) {
        if (subject == null || subject.trim().isEmpty()) {
            return "Email";
        }
        String normalized = subject.trim().toLowerCase();
        if (normalized.contains("account")) {
            return "Account Email";
        }
        if (normalized.contains("password")) {
            return "Security Email";
        }
        return "Email";
    }

    private String greeting(String name) {
        return name != null && !name.trim().isEmpty() ? "Good day " + name + "," : "Good day,";
    }

    private String defaultText(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }
}
