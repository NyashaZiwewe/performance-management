package hr.performancemanagement.config;

import hr.performancemanagement.service.api.SystemSettingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Properties;

@Configuration
@EnableAsync
@EnableAspectJAutoProxy
public class AppConfig implements WebMvcConfigurer {

    private final BootstrapAdminInterceptor bootstrapAdminInterceptor;

    public AppConfig(BootstrapAdminInterceptor bootstrapAdminInterceptor) {
        this.bootstrapAdminInterceptor = bootstrapAdminInterceptor;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JavaMailSender javaMailSender(SystemSettingService systemSettingService, Environment environment) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        String host = hasText(systemSettingService.getMailHost())
                ? systemSettingService.getMailHost()
                : environment.getProperty("spring.mail.host");
        if (hasText(host)) {
            mailSender.setHost(host.trim());
        }

        int port = systemSettingService.getMailPort() > 0
                ? systemSettingService.getMailPort()
                : resolveIntProperty(environment, "spring.mail.port");
        if (port > 0) {
            mailSender.setPort(port);
        }

        String username = hasText(systemSettingService.getMailUsername())
                ? systemSettingService.getMailUsername()
                : environment.getProperty("spring.mail.username");
        if (hasText(username)) {
            mailSender.setUsername(username.trim());
        }

        String password = hasText(systemSettingService.getMailPassword())
                ? systemSettingService.getMailPassword()
                : environment.getProperty("spring.mail.password");
        if (hasText(password)) {
            mailSender.setPassword(password.trim());
        }

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", environment.getProperty("spring.mail.properties.mail.smtp.auth", "true"));
        properties.put("mail.smtp.starttls.enable", environment.getProperty("spring.mail.properties.mail.smtp.starttls.enable", "true"));

        return mailSender;
    }

    private int resolveIntProperty(Environment environment, String propertyName) {
        String value = environment.getProperty(propertyName);
        if (!hasText(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(bootstrapAdminInterceptor);
    }
}
