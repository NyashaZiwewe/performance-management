package hr.performancemanagement.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class LoggingController {

    @GetMapping("/log-test")
    public String logTest() {
        log.info("INFO: Application started");
        log.debug("DEBUG: Debugging log");
        log.warn("WARN: Warning log");
        log.error("ERROR: Something went wrong!");

        return "Check the logs!";
    }
}
