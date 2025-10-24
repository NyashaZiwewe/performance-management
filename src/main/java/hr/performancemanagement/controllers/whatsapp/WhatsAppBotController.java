package hr.performancemanagement.controllers.whatsapp;

import hr.performancemanagement.service.whatsapp.OpenAIService;
import hr.performancemanagement.service.whatsapp.WhatsAppService;
import hr.performancemanagement.sessions.SessionManager;
import hr.performancemanagement.sessions.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whatsapp")
public class WhatsAppBotController {

    @Autowired
    private WhatsAppService whatsappService;

    @Autowired
    private OpenAIService openAIService;

    @PostMapping("/webhook")
    public void receiveMessage(@RequestParam("Body") String body,
                               @RequestParam("From") String from) {

        System.out.println("User: " + body);

        String response = openAIService.getChatCompletion(from, body);

        System.out.println("AI: " + response);

        whatsappService.sendMessage(from, response);
    }
}