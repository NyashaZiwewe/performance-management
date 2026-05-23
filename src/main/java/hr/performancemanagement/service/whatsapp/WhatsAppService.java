package hr.performancemanagement.service.whatsapp;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WhatsAppService {

    @Value("${twilio.whatsapp.from:}")
    private String fromNumber; // your Twilio sandbox number

    public void sendMessage(String to, String text) {
        if (!StringUtils.hasText(fromNumber)) {
            throw new IllegalStateException("Twilio sender number is not configured (twilio.whatsapp.from).");
        }

        int limit = 1600;

        for (int i = 0; i < text.length(); i += limit) {
            int end = Math.min(i + limit, text.length());
            String part = text.substring(i, end);
            Message message = Message.creator(
                    new PhoneNumber(to),          // recipient number (sender)
                    new PhoneNumber(fromNumber),  // your Twilio sandbox number
                    part
            ).create();

            System.out.println("✅ Message sent! SID: " + message.getSid());
        }


    }
}
