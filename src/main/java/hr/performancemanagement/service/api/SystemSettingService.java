package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.SystemSetting;
import hr.performancemanagement.repository.SystemSettingRepository;
import hr.performancemanagement.utils.wrappers.CredentialSettingsWrapper;
import hr.performancemanagement.utils.wrappers.SystemSettingsWrapper;
import org.springframework.core.env.Environment;
import java.util.LinkedHashMap;
import java.util.Map;

public interface SystemSettingService {
    String getCompanyName();
    String getCompanyLogo();
    String getSystemName();
    String getHostUrl();
    void syncHostUrl(String hostUrl);
    String getMultipartLocation();
    String getHREmail();
    String getAdminEmail();
    boolean isBootstrapAdminEnabled();
    boolean isBootstrapAdminAvailable();
    String getMailFromName();
    String getMailFromEmail();
    String getMailHost();
    int getMailPort();
    String getMailUsername();
    String getMailPassword();
    boolean isMailConfigured();
    CredentialSettingsWrapper getCredentialSettings();
    void saveCredentialSettings(CredentialSettingsWrapper wrapper);
    SystemSettingsWrapper getSettingsWrapper();
    void saveSettings(SystemSettingsWrapper wrapper);
    void ensureDefaults();
}
