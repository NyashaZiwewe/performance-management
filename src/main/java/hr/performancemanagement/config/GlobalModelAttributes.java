package hr.performancemanagement.config;

import hr.performancemanagement.service.api.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private SystemSettingService systemSettingService;

    @ModelAttribute("companyName")
    public String companyName() {
        return systemSettingService.getCompanyName();
    }

    @ModelAttribute("companyLogo")
    public String companyLogo() {
        return systemSettingService.getCompanyLogo();
    }

    @ModelAttribute("systemName")
    public String systemName() {
        return systemSettingService.getSystemName();
    }

    @ModelAttribute("bootstrapAdminAvailable")
    public boolean bootstrapAdminAvailable() {
        return systemSettingService.isBootstrapAdminAvailable();
    }
}
