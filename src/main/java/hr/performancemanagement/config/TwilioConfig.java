package hr.performancemanagement.config;

import com.twilio.Twilio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

@Configuration
public class TwilioConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioConfig.class);

    @Value("${twilio.accountSid:}")
    private String accountSid;

    @Value("${twilio.authToken:}")
    private String authToken;

    @PostConstruct
    public void initTwilio() {
        if (!StringUtils.hasText(accountSid) || !StringUtils.hasText(authToken)) {
            LOGGER.warn("Twilio credentials are not configured; WhatsApp messaging will be disabled.");
            return;
        }

        Twilio.init(accountSid, authToken);
        LOGGER.info("Twilio initialized successfully.");
    }
}
