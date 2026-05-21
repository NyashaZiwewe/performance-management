package hr.performancemanagement.service.api;

import java.io.UnsupportedEncodingException;

public interface Mailservice {
    void sendEmail(String to, String subject, String body) throws UnsupportedEncodingException;
}
