package hr.performancemanagement.controllers.whatsapp;

import hr.performancemanagement.service.whatsapp.OpenAIService;
import hr.performancemanagement.service.whatsapp.WhatsAppService;
import hr.performancemanagement.sessions.SessionManager;
import hr.performancemanagement.sessions.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/whatsapp")
public class WhatsAppBotController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppBotController.class);

    @Autowired
    private WhatsAppService whatsappService;

    @Autowired
    private OpenAIService openAIService;

    @PostMapping("/webhook")
    public void receiveMessage(@RequestParam("Body") String body,
                               @RequestParam("From") String from) {

        log.info("WhatsApp message received from {}: {}", from, body);

        String response = openAIService.getChatCompletion(from, body);

        log.info("AI response generated for {}: {}", from, response);

        whatsappService.sendMessage(from, response);
    }
}