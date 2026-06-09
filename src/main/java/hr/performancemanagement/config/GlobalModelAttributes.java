package hr.performancemanagement.config;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.service.api.SystemSettingService;
import hr.performancemanagement.service.api.AccessControlService;
import hr.performancemanagement.service.api.ReportingDateActivityPeriodService;
import hr.performancemanagement.service.api.ReportingDateService;
import hr.performancemanagement.service.api.CommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private SystemSettingService systemSettingService;
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private ReportingDateService reportingDateService;
    @Autowired
    private ReportingDateActivityPeriodService reportingDateActivityPeriodService;
    @Autowired
    private CommonService commonService;

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

    @ModelAttribute("canManageAccess")
    public boolean canManageAccess() {
        try {
            return accessControlService.canManageAccess();
        } catch (Exception ignored) {
            return false;
        }
    }

    @ModelAttribute("canManageAccounts")
    public boolean canManageAccounts() {
        try {
            return commonService.isAdmin() || commonService.hasSpecialRights();
        } catch (Exception ignored) {
            return false;
        }
    }

    @ModelAttribute("canManageLegacyWorkflowMapping")
    public boolean canManageLegacyWorkflowMapping() {
        try {
            return commonService.isAdmin() || commonService.hasSpecialRights();
        } catch (Exception ignored) {
            return false;
        }
    }

    @ModelAttribute("effectiveAccessSummary")
    public String effectiveAccessSummary() {
        try {
            return accessControlService.accessSummary();
        } catch (Exception ignored) {
            return "";
        }
    }

    @ModelAttribute("activeReportingActivitySummary")
    public String activeReportingActivitySummary() {
        try {
            ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
            return reportingDateActivityPeriodService.getCurrentActivitySummary(reportingDate);
        } catch (Exception ignored) {
            return "";
        }
    }
}
