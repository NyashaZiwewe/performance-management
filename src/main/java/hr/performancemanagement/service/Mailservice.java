package hr.performancemanagement.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class Mailservice {

    @Autowired
    JavaMailSender javaMailSender;
    @Async
    public void sendEmail(String to, String subject, String body) throws UnsupportedEncodingException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom("ZimTrade PR System <zimtradesystems@zimtrade.co.zw>");
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message);
    }
}
