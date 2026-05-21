package hr.performancemanagement.controllers;

import hr.performancemanagement.service.api.EmailNotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailNotificationLogService emailNotificationLogService;

    @GetMapping("/email/{id}/open")
    public String openEmailNotification(@PathVariable("id") long id) {
        String target = emailNotificationLogService.openEmailNotification(id);
        return "redirect:" + target;
    }
}
