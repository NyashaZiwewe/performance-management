package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.EmailNotificationLog;
import hr.performancemanagement.repository.EmailNotificationLogRepository;
import hr.performancemanagement.service.api.Mailservice;
import hr.performancemanagement.service.api.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
public class MailserviceImpl implements Mailservice {

    @Autowired
    JavaMailSender javaMailSender;
    @Autowired
    SystemSettingService systemSettingService;
    @Autowired
    EmailNotificationLogRepository emailNotificationLogRepository;

    @Override
    public void sendEmail(String to, String subject, String body) throws UnsupportedEncodingException {
        if (to == null || to.trim().isEmpty()) {
            throw new RuntimeException("Recipient email is required");
        }

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper;
        try {
            messageHelper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        } catch (MessagingException exception) {
            throw new RuntimeException(exception);
        }
        try {
            messageHelper.setTo(to.trim());
            messageHelper.setSubject(subject == null ? "" : subject.trim());
            messageHelper.setText(buildHtmlEmail(subject, body), true);
        } catch (MessagingException exception) {
            throw new RuntimeException(exception);
        }

        String fromEmail = resolveFromAddress();
        String fromName = systemSettingService.getMailFromName();
        if (fromEmail != null && !fromEmail.trim().isEmpty()) {
            try {
                if (fromName != null && !fromName.trim().isEmpty()) {
                    messageHelper.setFrom(fromEmail.trim(), fromName.trim());
                } else {
                    messageHelper.setFrom(fromEmail.trim());
                }
            } catch (MessagingException exception) {
                throw new RuntimeException(exception);
            }
        }

        javaMailSender.send(message);
        recordEmailNotification(to.trim(), subject == null ? "" : subject.trim(), body);
    }

    private String resolveFromAddress() {
        String fromEmail = systemSettingService.getMailFromEmail();
        if (hasText(fromEmail)) {
            return fromEmail.trim();
        }

        String mailUsername = systemSettingService.getMailUsername();
        if (hasText(mailUsername)) {
            return mailUsername.trim();
        }

        if (javaMailSender instanceof JavaMailSenderImpl) {
            String username = ((JavaMailSenderImpl) javaMailSender).getUsername();
            if (hasText(username)) {
                return username.trim();
            }
        }
        return null;
    }

    private String buildHtmlEmail(String subject, String body) {
        List<String> paragraphs = new ArrayList<String>();
        List<String> actionLinks = new ArrayList<String>();

        String[] blocks = body == null ? new String[0] : body.split("\\n\\n");
        for (String block : blocks) {
            String trimmed = block == null ? "" : block.trim();
            if (trimmed.isEmpty()) {
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

        String companyName = escapeHtml(defaultText(systemSettingService.getCompanyName(), "ZimTrade"));
        String systemName = escapeHtml(defaultText(systemSettingService.getSystemName(), "Performance Management System"));

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
                "<h1 style=\"margin:10px 0 0;font-size:28px;line-height:1.25;font-weight:700;\">" + escapeHtml(defaultText(subject, "Notification")) + "</h1>" +
                "<div style=\"margin-top:8px;font-size:14px;opacity:0.9;\">Updates from " + companyName + "</div>" +
                "</div>" +
                "<div style=\"padding:30px 32px 22px;\">" +
                contentBuilder +
                (actionsBuilder.length() > 0
                        ? "<div style=\"margin:26px 0 10px;\">" + actionsBuilder + "</div>"
                        : "") +
                "</div>" +
                "<div style=\"padding:18px 32px 28px;border-top:1px solid #e1ebf3;background:#fbfdff;color:#6c7f92;font-size:12px;line-height:1.7;\">" +
                "<div style=\"margin-bottom:6px;font-weight:700;color:#183247;\">" + companyName + "</div>" +
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

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
        if (normalized.contains("scorecard")) {
            return "Scorecard Email";
        }
        if (normalized.contains("score")) {
            return "Score Email";
        }
        if (normalized.contains("account")) {
            return "Account Email";
        }
        if (normalized.contains("password")) {
            return "Security Email";
        }
        return "Email";
    }
}
