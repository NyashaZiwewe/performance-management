package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.SystemSetting;
import hr.performancemanagement.repository.SystemSettingRepository;
import hr.performancemanagement.utils.wrappers.CredentialSettingsWrapper;
import hr.performancemanagement.utils.wrappers.SystemSettingsWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class SystemSettingServiceImpl implements hr.performancemanagement.service.api.SystemSettingService {

    private static final String COMPANY_NAME = "company.name";
    private static final String COMPANY_LOGO = "company.logo";
    private static final String SYSTEM_NAME = "system.name";
    private static final String HOST_URL = "host.url";
    private static final String MULTIPART_LOCATION = "multipart.location";
    private static final String EMAIL_HR = "email.hr";
    private static final String EMAIL_ADMIN = "email.admin";
    private static final String BOOTSTRAP_ADMIN_ENABLED = "bootstrap.admin.enabled";
    private static final String MAIL_FROM_NAME = "mail.from.name";
    private static final String MAIL_FROM_EMAIL = "mail.from.email";
    private static final String MAIL_HOST = "mail.host";
    private static final String MAIL_PORT = "mail.port";
    private static final String MAIL_USERNAME = "mail.username";
    private static final String MAIL_PASSWORD = "mail.password";
    private static final String SCORECARD_STATUS_NEW = "scorecard.status.new";
    private static final String SCORECARD_STATUS_PENDING_APPROVAL = "scorecard.status.pendingApproval";
    private static final String SCORECARD_STATUS_APPROVED_BY_SUPERVISOR = "scorecard.status.approvedBySupervisor";
    private static final String SCORECARD_STATUS_REJECTED_BY_SUPERVISOR = "scorecard.status.rejectedBySupervisor";
    private static final String SCORECARD_STATUS_APPROVED_BY_HR = "scorecard.status.approvedByHr";
    private static final String SCORECARD_STATUS_REJECTED_BY_HR = "scorecard.status.rejectedByHr";
    private static final String SCORECARD_STATUS_SCORED_BY_EMPLOYEE = "scorecard.status.scoredByEmployee";
    private static final String SCORECARD_STATUS_SCORED_BY_SUPERVISOR = "scorecard.status.scoredBySupervisor";
    private static final String SCORECARD_STATUS_AGREED_BY_TWO = "scorecard.status.agreedByTwo";
    private static final String SCORECARD_STATUS_MODERATED_BY_HR = "scorecard.status.moderatedByHr";
    private static final String SCORECARD_STATUS_CLOSED = "scorecard.status.closed";
    private static final String SCORECARD_WORKFLOW_SEQUENCE = "scorecard.workflow.sequence";
    private static final long SETTINGS_CACHE_TTL_MS = 30_000L;

    @Autowired
    private SystemSettingRepository repository;
    private final Object settingsMonitor = new Object();
    private volatile boolean defaultsInitialized = false;
    private volatile Map<String, String> cachedSettings = Collections.emptyMap();
    private volatile long cacheLoadedAt = 0L;

    @PostConstruct
    public void initializeSettings() {
        ensureDefaults();
        refreshSettingsCache();
    }

    @Override
    public String getCompanyName() {
        return getValue(COMPANY_NAME);
    }

    @Override
    public String getCompanyLogo() {
        return getValue(COMPANY_LOGO);
    }

    @Override
    public String getSystemName() {
        return getValue(SYSTEM_NAME);
    }

    @Override
    public String getHostUrl() {
        return getValue(HOST_URL);
    }

    @Transactional
    @Override
    public void syncHostUrl(String hostUrl) {
        if (hasText(hostUrl) && !hostUrl.equals(getHostUrl())) {
            saveValue(HOST_URL, hostUrl);
        }
    }

    @Override
    public String getMultipartLocation() {
        return getValue(MULTIPART_LOCATION);
    }

    @Override
    public String getHREmail() {
        return getValue(EMAIL_HR);
    }

    @Override
    public String getAdminEmail() {
        return getValue(EMAIL_ADMIN);
    }

    @Override
    public boolean isBootstrapAdminEnabled() {
        return Boolean.parseBoolean(getValue(BOOTSTRAP_ADMIN_ENABLED));
    }

    @Override
    public boolean isBootstrapAdminAvailable() {
        return isBootstrapAdminEnabled();
    }

    @Override
    public String getMailFromName() {
        return getValue(MAIL_FROM_NAME);
    }

    @Override
    public String getMailFromEmail() {
        return getValue(MAIL_FROM_EMAIL);
    }

    @Override
    public String getMailHost() {
        return getValue(MAIL_HOST);
    }

    @Override
    public int getMailPort() {
        String value = getValue(MAIL_PORT);
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    @Override
    public String getMailUsername() {
        return getValue(MAIL_USERNAME);
    }

    @Override
    public String getMailPassword() {
        return getValue(MAIL_PASSWORD);
    }

    @Override
    public String getScorecardStatusNew() {
        return getValue(SCORECARD_STATUS_NEW);
    }

    @Override
    public String getScorecardStatusPendingApproval() {
        return getValue(SCORECARD_STATUS_PENDING_APPROVAL);
    }

    @Override
    public String getScorecardStatusApprovedBySupervisor() {
        return getValue(SCORECARD_STATUS_APPROVED_BY_SUPERVISOR);
    }

    @Override
    public String getScorecardStatusRejectedBySupervisor() {
        return getValue(SCORECARD_STATUS_REJECTED_BY_SUPERVISOR);
    }

    @Override
    public String getScorecardStatusApprovedByHr() {
        return getValue(SCORECARD_STATUS_APPROVED_BY_HR);
    }

    @Override
    public String getScorecardStatusRejectedByHr() {
        return getValue(SCORECARD_STATUS_REJECTED_BY_HR);
    }

    @Override
    public String getScorecardStatusScoredByEmployee() {
        return getValue(SCORECARD_STATUS_SCORED_BY_EMPLOYEE);
    }

    @Override
    public String getScorecardStatusScoredBySupervisor() {
        return getValue(SCORECARD_STATUS_SCORED_BY_SUPERVISOR);
    }

    @Override
    public String getScorecardStatusAgreedByTwo() {
        return getValue(SCORECARD_STATUS_AGREED_BY_TWO);
    }

    @Override
    public String getScorecardStatusModeratedByHr() {
        return getValue(SCORECARD_STATUS_MODERATED_BY_HR);
    }

    @Override
    public String getScorecardStatusClosed() {
        return getValue(SCORECARD_STATUS_CLOSED);
    }

    @Override
    public String getScorecardWorkflowSequence() {
        return getValue(SCORECARD_WORKFLOW_SEQUENCE);
    }

    @Override
    public boolean isMailConfigured() {
        return hasText(getMailHost())
                && getMailPort() > 0
                && hasText(getMailUsername())
                && hasText(getMailPassword());
    }

    @Transactional
    @Override
    public CredentialSettingsWrapper getCredentialSettings() {
        CredentialSettingsWrapper wrapper = new CredentialSettingsWrapper();
        wrapper.setMailFromName(getMailFromName());
        wrapper.setMailFromEmail(getMailFromEmail());
        wrapper.setMailHost(getMailHost());
        wrapper.setMailPort(getMailPort() > 0 ? String.valueOf(getMailPort()) : "");
        wrapper.setMailUsername(getMailUsername());
        wrapper.setMailPassword("");
        wrapper.setMailPasswordConfigured(hasText(getMailPassword()));
        return wrapper;
    }

    @Transactional
    @Override
    public void saveCredentialSettings(CredentialSettingsWrapper wrapper) {
        saveValue(MAIL_FROM_NAME, wrapper.getMailFromName());
        saveValue(MAIL_FROM_EMAIL, wrapper.getMailFromEmail());
        saveValue(MAIL_HOST, wrapper.getMailHost());
        saveValue(MAIL_PORT, wrapper.getMailPort());
        saveValue(MAIL_USERNAME, wrapper.getMailUsername());
        saveMailPassword(wrapper.getMailPassword());
    }

    @Transactional
    @Override
    public SystemSettingsWrapper getSettingsWrapper() {
        SystemSettingsWrapper wrapper = new SystemSettingsWrapper();
        wrapper.setCompanyName(getCompanyName());
        wrapper.setCompanyLogo(getCompanyLogo());
        wrapper.setSystemName(getSystemName());
        wrapper.setHostUrl(getHostUrl());
        wrapper.setMultipartLocation(getMultipartLocation());
        wrapper.setHrEmail(getHREmail());
        wrapper.setAdminEmail(getAdminEmail());
        wrapper.setBootstrapAdminEnabled(isBootstrapAdminEnabled());
        wrapper.setMailFromName(getMailFromName());
        wrapper.setMailFromEmail(getMailFromEmail());
        wrapper.setMailHost(getMailHost());
        wrapper.setMailPort(getMailPort() > 0 ? String.valueOf(getMailPort()) : "");
        wrapper.setMailUsername(getMailUsername());
        wrapper.setMailPassword("");
        wrapper.setMailPasswordConfigured(hasText(getMailPassword()));
        wrapper.setScorecardStatusNew(getScorecardStatusNew());
        wrapper.setScorecardStatusPendingApproval(getScorecardStatusPendingApproval());
        wrapper.setScorecardStatusApprovedBySupervisor(getScorecardStatusApprovedBySupervisor());
        wrapper.setScorecardStatusRejectedBySupervisor(getScorecardStatusRejectedBySupervisor());
        wrapper.setScorecardStatusApprovedByHr(getScorecardStatusApprovedByHr());
        wrapper.setScorecardStatusRejectedByHr(getScorecardStatusRejectedByHr());
        wrapper.setScorecardStatusScoredByEmployee(getScorecardStatusScoredByEmployee());
        wrapper.setScorecardStatusScoredBySupervisor(getScorecardStatusScoredBySupervisor());
        wrapper.setScorecardStatusAgreedByTwo(getScorecardStatusAgreedByTwo());
        wrapper.setScorecardStatusModeratedByHr(getScorecardStatusModeratedByHr());
        wrapper.setScorecardStatusClosed(getScorecardStatusClosed());
        wrapper.setScorecardWorkflowSequence(getScorecardWorkflowSequence());
        return wrapper;
    }

    @Transactional
    @Override
    public void saveSettings(SystemSettingsWrapper wrapper) {
        String multipartLocation = hasText(wrapper.getMultipartLocation())
                ? wrapper.getMultipartLocation()
                : getMultipartLocation();
        saveValue(COMPANY_NAME, wrapper.getCompanyName());
        saveValue(COMPANY_LOGO, wrapper.getCompanyLogo());
        saveValue(SYSTEM_NAME, wrapper.getSystemName());
        saveValue(HOST_URL, wrapper.getHostUrl());
        saveValue(MULTIPART_LOCATION, multipartLocation);
        saveValue(EMAIL_HR, wrapper.getHrEmail());
        saveValue(EMAIL_ADMIN, wrapper.getAdminEmail());
        if (wrapper.getBootstrapAdminEnabled() != null) {
            saveValue(BOOTSTRAP_ADMIN_ENABLED, String.valueOf(wrapper.getBootstrapAdminEnabled()));
        }
        saveValue(MAIL_FROM_NAME, wrapper.getMailFromName());
        saveValue(MAIL_FROM_EMAIL, wrapper.getMailFromEmail());
        saveValue(MAIL_HOST, wrapper.getMailHost());
        saveValue(MAIL_PORT, wrapper.getMailPort());
        saveValue(MAIL_USERNAME, wrapper.getMailUsername());
        saveMailPassword(wrapper.getMailPassword());
        saveValue(SCORECARD_STATUS_NEW, normalizeWorkflowStatus(wrapper.getScorecardStatusNew()));
        saveValue(SCORECARD_STATUS_PENDING_APPROVAL, normalizeWorkflowStatus(wrapper.getScorecardStatusPendingApproval()));
        saveValue(SCORECARD_STATUS_APPROVED_BY_SUPERVISOR, normalizeWorkflowStatus(wrapper.getScorecardStatusApprovedBySupervisor()));
        saveValue(SCORECARD_STATUS_REJECTED_BY_SUPERVISOR, normalizeWorkflowStatus(wrapper.getScorecardStatusRejectedBySupervisor()));
        saveValue(SCORECARD_STATUS_APPROVED_BY_HR, normalizeWorkflowStatus(wrapper.getScorecardStatusApprovedByHr()));
        saveValue(SCORECARD_STATUS_REJECTED_BY_HR, normalizeWorkflowStatus(wrapper.getScorecardStatusRejectedByHr()));
        saveValue(SCORECARD_STATUS_SCORED_BY_EMPLOYEE, normalizeWorkflowStatus(wrapper.getScorecardStatusScoredByEmployee()));
        saveValue(SCORECARD_STATUS_SCORED_BY_SUPERVISOR, normalizeWorkflowStatus(wrapper.getScorecardStatusScoredBySupervisor()));
        saveValue(SCORECARD_STATUS_AGREED_BY_TWO, normalizeWorkflowStatus(wrapper.getScorecardStatusAgreedByTwo()));
        saveValue(SCORECARD_STATUS_MODERATED_BY_HR, normalizeWorkflowStatus(wrapper.getScorecardStatusModeratedByHr()));
        saveValue(SCORECARD_STATUS_CLOSED, normalizeWorkflowStatus(wrapper.getScorecardStatusClosed()));
        saveValue(SCORECARD_WORKFLOW_SEQUENCE, normalizeWorkflowSequence(wrapper.getScorecardWorkflowSequence()));
    }

    @Transactional
    @Override
    public void ensureDefaults() {
        if (defaultsInitialized) {
            return;
        }
        synchronized (settingsMonitor) {
            if (defaultsInitialized) {
                return;
            }
            Map<String, SettingDefinition> defaults = defaults();
            Map<String, SystemSetting> existingByKey = new HashMap<>();
            List<SystemSetting> existingSettings = repository.findAll();
            boolean createdDefaults = false;
            for (SystemSetting existingSetting : existingSettings) {
                if (existingSetting != null && existingSetting.getSettingKey() != null) {
                    existingByKey.put(existingSetting.getSettingKey(), existingSetting);
                }
            }
            for (Map.Entry<String, SettingDefinition> entry : defaults.entrySet()) {
                if (existingByKey.containsKey(entry.getKey())) {
                    continue;
                }
                SystemSetting setting = new SystemSetting();
                setting.setSettingKey(entry.getKey());
                setting.setSettingValue(entry.getValue().defaultValue);
                setting.setDescription(entry.getValue().description);
                repository.save(setting);
                createdDefaults = true;
            }
            defaultsInitialized = true;
            if (createdDefaults) {
                invalidateSettingsCache();
            }
        }
    }

    private String getValue(String key) {
        Map<String, String> settings = getCachedSettings();
        String value = settings.get(key);
        if (!hasText(value)) {
            return defaults().get(key).defaultValue;
        }
        return value.trim();
    }

    private void saveValue(String key, String value) {
        SystemSetting setting = repository.findSystemSettingBySettingKey(key);
        if (setting == null) {
            setting = new SystemSetting();
            setting.setSettingKey(key);
            setting.setDescription(defaults().get(key).description);
        }
        setting.setSettingValue(value == null ? "" : value.trim());
        repository.save(setting);
        invalidateSettingsCache();
    }

    private Map<String, SettingDefinition> defaults() {
        Map<String, SettingDefinition> settings = new LinkedHashMap<>();
        settings.put(COMPANY_NAME, new SettingDefinition(
                "ZimTrade",
                "Company name displayed in the user interface"
        ));
        settings.put(COMPANY_LOGO, new SettingDefinition(
                "zimlogo.png",
                "Logo filename under /img used in the sidebar"
        ));
        settings.put(SYSTEM_NAME, new SettingDefinition(
                "Performance Management System",
                "System name displayed in top navigation"
        ));
        settings.put(HOST_URL, new SettingDefinition(
                "http://localhost:9000",
                "Base host URL used for generated links"
        ));
        settings.put(MULTIPART_LOCATION, new SettingDefinition(
                "/data/uploads/performance-management/",
                "Multipart upload location used for storing document uploads"
        ));
        settings.put(EMAIL_HR, new SettingDefinition(
                "",
                "HR email used in approval notifications"
        ));
        settings.put(EMAIL_ADMIN, new SettingDefinition(
                "",
                "Administrator email used in system alerts"
        ));
        settings.put(BOOTSTRAP_ADMIN_ENABLED, new SettingDefinition(
                "true",
                "Enables the hardcoded bootstrap setup login"
        ));
        settings.put(MAIL_FROM_NAME, new SettingDefinition(
                "Performance Management System",
                "Display name used in outgoing emails"
        ));
        settings.put(MAIL_FROM_EMAIL, new SettingDefinition(
                "",
                "Email address used as sender in outgoing emails"
        ));
        settings.put(MAIL_HOST, new SettingDefinition(
                "smtp.office365.com",
                "SMTP host for outgoing mail"
        ));
        settings.put(MAIL_PORT, new SettingDefinition(
                "587",
                "SMTP port for outgoing mail"
        ));
        settings.put(MAIL_USERNAME, new SettingDefinition(
                "",
                "SMTP username used to authenticate outgoing mail"
        ));
        settings.put(MAIL_PASSWORD, new SettingDefinition(
                "",
                "SMTP password used to authenticate outgoing mail"
        ));
        settings.put(SCORECARD_STATUS_NEW, new SettingDefinition(
                "NEW",
                "Scorecard status used immediately after scorecard creation"
        ));
        settings.put(SCORECARD_STATUS_PENDING_APPROVAL, new SettingDefinition(
                "PENDING_APPROVAL",
                "Scorecard status used after target submission for supervisor approval"
        ));
        settings.put(SCORECARD_STATUS_APPROVED_BY_SUPERVISOR, new SettingDefinition(
                "APPROVED_BY_SUPERVISOR",
                "Scorecard status used after supervisor approval"
        ));
        settings.put(SCORECARD_STATUS_REJECTED_BY_SUPERVISOR, new SettingDefinition(
                "REJECTED_BY_SUPERVISOR",
                "Scorecard status used after supervisor rejection"
        ));
        settings.put(SCORECARD_STATUS_APPROVED_BY_HR, new SettingDefinition(
                "APPROVED_BY_HR",
                "Scorecard status used after HR approval"
        ));
        settings.put(SCORECARD_STATUS_REJECTED_BY_HR, new SettingDefinition(
                "REJECTED_BY_HR",
                "Scorecard status used after HR rejection"
        ));
        settings.put(SCORECARD_STATUS_SCORED_BY_EMPLOYEE, new SettingDefinition(
                "SCORED_BY_EMPLOYEE",
                "Scorecard status used after owner sign-off and score capture"
        ));
        settings.put(SCORECARD_STATUS_SCORED_BY_SUPERVISOR, new SettingDefinition(
                "SCORED_BY_SUPERVISOR",
                "Scorecard status used after supervisor score capture"
        ));
        settings.put(SCORECARD_STATUS_AGREED_BY_TWO, new SettingDefinition(
                "AGREED_BY_TWO",
                "Scorecard status used after agreed score capture by owner and supervisor"
        ));
        settings.put(SCORECARD_STATUS_MODERATED_BY_HR, new SettingDefinition(
                "MODERATED_BY_HR",
                "Scorecard status used after HR moderation"
        ));
        settings.put(SCORECARD_STATUS_CLOSED, new SettingDefinition(
                "CLOSED",
                "Scorecard status used when scorecard is closed"
        ));
        settings.put(SCORECARD_WORKFLOW_SEQUENCE, new SettingDefinition(
                "NEW,PENDING_APPROVAL,APPROVED_BY_SUPERVISOR,REJECTED_BY_SUPERVISOR,APPROVED_BY_HR,REJECTED_BY_HR,SCORED_BY_EMPLOYEE,SCORED_BY_SUPERVISOR,AGREED_BY_TWO,MODERATED_BY_HR,CLOSED",
                "Comma-separated scorecard status order used in scorecard filters and display"
        ));
        return settings;
    }

    private void saveMailPassword(String value) {
        if (!hasText(value)) {
            return;
        }
        saveValue(MAIL_PASSWORD, value);
    }

    private String normalizeWorkflowStatus(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    private String normalizeWorkflowSequence(String value) {
        if (value == null) {
            return "";
        }
        String[] tokens = value.split("[,\\n]");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            String normalized = normalizeWorkflowStatus(token);
            if (normalized.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(normalized);
        }
        return builder.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Map<String, String> getCachedSettings() {
        if (isCacheFresh()) {
            return cachedSettings;
        }
        synchronized (settingsMonitor) {
            if (!isCacheFresh()) {
                refreshSettingsCache();
            }
            return cachedSettings;
        }
    }

    private boolean isCacheFresh() {
        return cacheLoadedAt > 0 && (System.currentTimeMillis() - cacheLoadedAt) <= SETTINGS_CACHE_TTL_MS;
    }

    private void refreshSettingsCache() {
        ensureDefaults();
        List<SystemSetting> settings = repository.findAll();
        Map<String, String> values = new HashMap<>();
        for (SystemSetting setting : settings) {
            if (setting == null || setting.getSettingKey() == null) {
                continue;
            }
            values.put(setting.getSettingKey(), setting.getSettingValue());
        }
        cachedSettings = values;
        cacheLoadedAt = System.currentTimeMillis();
    }

    private void invalidateSettingsCache() {
        cacheLoadedAt = 0L;
        cachedSettings = Collections.emptyMap();
    }

    private static class SettingDefinition {
        private final String defaultValue;
        private final String description;

        private SettingDefinition(String defaultValue, String description) {
            this.defaultValue = defaultValue;
            this.description = description;
        }
    }
}
