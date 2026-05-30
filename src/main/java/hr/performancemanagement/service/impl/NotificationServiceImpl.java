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
    public void sendAccountSetupAsync(Account account, String resetLink) {
        sendAccountSetup(account, resetLink);
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
        String body = buildMessage(greeting(recipientName), normalizeUserMessage(message));
        return send(recipientEmail, subject, body);
    }

    @Override
    @Async
    public void sendUserMessageAsync(String recipientEmail, String recipientName, String subject, String message) {
        sendUserMessage(recipientEmail, recipientName, subject, message);
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
        builder.append("Best regards,\n").append(resolveSignatureName());
        return builder.toString();
    }

    private String normalizeUserMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        String normalized = message.replace("\r\n", "\n").replace('\r', '\n').trim();
        normalized = normalized.replaceFirst("(?is)^good day[^\\n]*\\n+", "");
        normalized = normalized.replaceFirst("(?is)\\n*best regards,?\\s*\\n\\s*the\\s+zimtrade\\s+team\\s*$", "");
        normalized = normalized.replaceFirst("(?is)\\n*best regards,?\\s*\\n[^\\n]+\\s*$", "");
        normalized = normalized.trim();
        return normalized;
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
        String normalizedBody = body == null ? "" : body.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<String> blocks = splitBlocks(normalizedBody);
        String greeting = extractGreeting(blocks);
        String closingName = extractClosingName(blocks);

        List<String> paragraphs = new ArrayList<String>();
        List<String[]> actionLinks = new ArrayList<String[]>();
        List<String[]> details = new ArrayList<String[]>();

        for (String block : blocks) {
            appendBlockContent(block, paragraphs, details, actionLinks);
        }

        String brandName = escapeHtml(resolveBrandName());
        String systemName = escapeHtml(resolveSystemName());
        String safeSubject = escapeHtml(defaultText(subject, "Notification"));

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>")
                .append("<html><body style=\"margin:0;padding:0;background:#f3f6fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:#f3f6fb;padding:28px 12px;\">")
                .append("<tr><td align=\"center\">")
                .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:640px;background:#ffffff;border:1px solid #dbe3ef;border-radius:8px;overflow:hidden;\">")
                .append("<tr><td style=\"background:#12355b;padding:24px 28px;color:#ffffff;\">")
                .append("<div style=\"font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:#b8d7f5;font-weight:700;\">")
                .append(systemName)
                .append("</div>")
                .append("<h1 style=\"margin:10px 0 0;font-size:24px;line-height:1.3;font-weight:700;\">")
                .append(safeSubject)
                .append("</h1>")
                .append("</td></tr>")
                .append("<tr><td style=\"padding:28px;\">")
                .append("<p style=\"margin:0 0 14px;font-size:16px;line-height:1.6;color:#1f2937;\">")
                .append(escapeHtml(greeting))
                .append("</p>");

        for (String paragraph : paragraphs) {
            html.append("<p style=\"margin:0 0 14px;font-size:15px;line-height:1.7;color:#42526b;\">")
                    .append(escapeHtml(paragraph))
                    .append("</p>");
        }

        if (!details.isEmpty()) {
            html.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border:1px solid #e1e8f0;border-radius:8px;margin:0 0 24px;\">");
            for (String[] detail : details) {
                if (detail == null || detail.length < 2) {
                    continue;
                }
                html.append("<tr>")
                        .append("<td style=\"padding:12px 16px;border-bottom:1px solid #e1e8f0;color:#697386;font-size:13px;width:42%;\">")
                        .append(escapeHtml(detail[0]))
                        .append("</td>")
                        .append("<td style=\"padding:12px 16px;border-bottom:1px solid #e1e8f0;color:#1f2937;font-size:14px;font-weight:700;\">")
                        .append(escapeHtml(detail[1]))
                        .append("</td>")
                        .append("</tr>");
            }
            html.append("</table>");
        }

        for (String[] actionLink : actionLinks) {
            if (actionLink == null || actionLink.length < 2) {
                continue;
            }
            String actionText = defaultText(actionLink[0], "Open Link");
            String actionUrl = actionLink[1];
            html.append("<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin:0 0 22px;\"><tr><td>")
                    .append("<a href=\"")
                    .append(escapeHtmlAttribute(actionUrl))
                    .append("\" style=\"display:inline-block;background:#0f766e;color:#ffffff;text-decoration:none;font-size:15px;font-weight:700;padding:13px 20px;border-radius:6px;\">")
                    .append(escapeHtml(actionText))
                    .append("</a>")
                    .append("</td></tr></table>")
                    .append("<p style=\"margin:0 0 22px;font-size:12px;line-height:1.6;color:#697386;word-break:break-all;\">")
                    .append("If the button does not work, copy and paste this link into your browser:<br>")
                    .append("<a href=\"")
                    .append(escapeHtmlAttribute(actionUrl))
                    .append("\" style=\"color:#0f766e;text-decoration:underline;\">")
                    .append(escapeHtml(actionUrl))
                    .append("</a></p>");
        }

        html.append("<p style=\"margin:0;font-size:15px;line-height:1.7;color:#42526b;\">Best regards,<br>")
                .append(escapeHtml(closingName))
                .append("</p>")
                .append("</td></tr>")
                .append("<tr><td style=\"background:#f8fafc;padding:16px 28px;color:#697386;font-size:12px;line-height:1.6;border-top:1px solid #e1e8f0;\">")
                .append("This is an automated message from ")
                .append(brandName)
                .append(". Please do not reply directly to this email.")
                .append("</td></tr>")
                .append("</table></td></tr></table></body></html>");
        return html.toString();
    }

    private List<String> splitBlocks(String content) {
        List<String> blocks = new ArrayList<String>();
        if (content == null || content.trim().isEmpty()) {
            return blocks;
        }
        for (String block : content.split("\\n\\s*\\n")) {
            String trimmed = block == null ? null : block.trim();
            if (trimmed != null && !trimmed.isEmpty()) {
                blocks.add(trimmed);
            }
        }
        return blocks;
    }

    private String extractGreeting(List<String> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "Good day,";
        }
        String first = blocks.get(0);
        if (isGreetingLine(first)) {
            blocks.remove(0);
            return first;
        }
        return "Good day,";
    }

    private String extractClosingName(List<String> blocks) {
        String defaultClosing = resolveSignatureName();
        if (blocks == null || blocks.isEmpty()) {
            return defaultClosing;
        }
        String last = blocks.get(blocks.size() - 1);
        String lower = last.toLowerCase();
        if (!lower.startsWith("best regards") && !lower.startsWith("kind regards")) {
            return defaultClosing;
        }
        blocks.remove(blocks.size() - 1);
        String[] lines = last.split("\\n");
        if (lines.length > 1) {
            for (int index = 1; index < lines.length; index++) {
                String candidate = lines[index] == null ? null : lines[index].trim();
                if (candidate != null && !candidate.isEmpty()) {
                    return candidate;
                }
            }
        }
        return defaultClosing;
    }

    private void appendBlockContent(String block,
                                    List<String> paragraphs,
                                    List<String[]> details,
                                    List<String[]> actionLinks) {
        if (block == null || block.trim().isEmpty()) {
            return;
        }
        String[] lines = block.split("\\n");
        for (String rawLine : lines) {
            String line = rawLine == null ? null : rawLine.trim();
            if (line == null || line.isEmpty()) {
                continue;
            }
            int separatorIndex = line.indexOf(": ");
            if (separatorIndex > 0 && separatorIndex + 2 < line.length()) {
                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 2).trim();
                if (isHttpUrl(value)) {
                    actionLinks.add(new String[]{key, value});
                    continue;
                }
                if (isDetailEntry(key, value)) {
                    details.add(new String[]{key, value});
                    continue;
                }
            }
            paragraphs.add(line);
        }
    }

    private boolean isGreetingLine(String line) {
        if (line == null) {
            return false;
        }
        String lower = line.trim().toLowerCase();
        return lower.startsWith("good day") || lower.startsWith("dear ");
    }

    private boolean isDetailEntry(String key, String value) {
        if (key == null || value == null || key.trim().isEmpty() || value.trim().isEmpty()) {
            return false;
        }
        if (isHttpUrl(value)) {
            return false;
        }
        return key.length() <= 40;
    }

    private boolean isHttpUrl(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private String resolveBrandName() {
        return defaultText(systemSettingService.getCompanyName(), systemSettingService.getSystemName());
    }

    private String resolveSystemName() {
        return defaultText(systemSettingService.getSystemName(), "Performance Management");
    }

    private String resolveSignatureName() {
        return defaultText(resolveBrandName(), "Performance Management");
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
