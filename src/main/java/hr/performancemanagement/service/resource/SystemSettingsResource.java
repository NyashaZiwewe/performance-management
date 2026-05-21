package hr.performancemanagement.service.resource;

import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.SystemSettingService;
import hr.performancemanagement.entities.Account;
import hr.performancemanagement.utils.dto.CommonResponse;
import hr.performancemanagement.utils.wrappers.CredentialSettingsWrapper;
import hr.performancemanagement.utils.wrappers.SystemSettingsWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/system-settings")
@RequiredArgsConstructor
public class SystemSettingsResource {

    private final CommonService commonService;
    private final SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<CommonResponse<SystemSettingsWrapper>> getSystemSettings(HttpServletRequest request) {
        ResponseEntity<CommonResponse<SystemSettingsWrapper>> forbiddenResponse = forbiddenSettingsResponse();
        if (forbiddenResponse != null) {
            return forbiddenResponse;
        }

        String runtimeHostUrl = resolveRuntimeHostUrl(request);
        systemSettingService.syncHostUrl(runtimeHostUrl);
        SystemSettingsWrapper wrapper = systemSettingService.getSettingsWrapper();
        wrapper.setHostUrl(runtimeHostUrl);
        return ResponseEntity.ok(CommonResponse.<SystemSettingsWrapper>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("System settings retrieved successfully")
                .data(wrapper)
                .build());
    }

    @PutMapping
    public ResponseEntity<CommonResponse<SystemSettingsWrapper>> saveSystemSettings(
            @RequestBody SystemSettingsWrapper wrapper,
            HttpServletRequest request) {
        ResponseEntity<CommonResponse<SystemSettingsWrapper>> forbiddenResponse = forbiddenSettingsResponse();
        if (forbiddenResponse != null) {
            return forbiddenResponse;
        }

        String runtimeHostUrl = resolveRuntimeHostUrl(request);
        wrapper.setHostUrl(runtimeHostUrl);
        systemSettingService.syncHostUrl(runtimeHostUrl);
        systemSettingService.saveSettings(wrapper);
        SystemSettingsWrapper updatedWrapper = systemSettingService.getSettingsWrapper();
        updatedWrapper.setHostUrl(runtimeHostUrl);
        return ResponseEntity.ok(CommonResponse.<SystemSettingsWrapper>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("System settings updated successfully")
                .data(updatedWrapper)
                .build());
    }

    @GetMapping("/credentials")
    public ResponseEntity<CommonResponse<CredentialSettingsWrapper>> getCredentialSettings() {
        ResponseEntity<CommonResponse<CredentialSettingsWrapper>> forbiddenResponse = forbiddenCredentialResponse();
        if (forbiddenResponse != null) {
            return forbiddenResponse;
        }

        CredentialSettingsWrapper wrapper = systemSettingService.getCredentialSettings();
        return ResponseEntity.ok(CommonResponse.<CredentialSettingsWrapper>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Credential settings retrieved successfully")
                .data(wrapper)
                .build());
    }

    @PutMapping("/credentials")
    public ResponseEntity<CommonResponse<CredentialSettingsWrapper>> saveCredentialSettings(@RequestBody CredentialSettingsWrapper wrapper) {
        ResponseEntity<CommonResponse<CredentialSettingsWrapper>> forbiddenResponse = forbiddenCredentialResponse();
        if (forbiddenResponse != null) {
            return forbiddenResponse;
        }

        systemSettingService.saveCredentialSettings(wrapper);
        CredentialSettingsWrapper updatedWrapper = systemSettingService.getCredentialSettings();
        return ResponseEntity.ok(CommonResponse.<CredentialSettingsWrapper>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Credential settings updated successfully")
                .data(updatedWrapper)
                .build());
    }

    private ResponseEntity<CommonResponse<SystemSettingsWrapper>> forbiddenSettingsResponse() {
        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser != null && (commonService.isAdmin() || commonService.hasSpecialRights())) {
            return null;
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(CommonResponse.<SystemSettingsWrapper>builder()
                .isSuccess(false)
                .statusCode(HttpStatus.FORBIDDEN.value())
                .message("You are not allowed to access system settings.")
                .data(null)
                .build());
    }

    private ResponseEntity<CommonResponse<CredentialSettingsWrapper>> forbiddenCredentialResponse() {
        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser != null && (commonService.isAdmin() || commonService.hasSpecialRights())) {
            return null;
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(CommonResponse.<CredentialSettingsWrapper>builder()
                .isSuccess(false)
                .statusCode(HttpStatus.FORBIDDEN.value())
                .message("You are not allowed to access credential settings.")
                .data(null)
                .build());
    }

    private String resolveRuntimeHostUrl(HttpServletRequest request) {
        String scheme = firstHeaderValue(request, "X-Forwarded-Proto");
        if (!hasText(scheme)) {
            scheme = request.getScheme();
        }

        String host = firstHeaderValue(request, "X-Forwarded-Host");
        if (!hasText(host)) {
            host = request.getHeader("Host");
        }
        if (!hasText(host)) {
            host = request.getServerName();
            int port = request.getServerPort();
            boolean defaultHttp = "http".equalsIgnoreCase(scheme) && port == 80;
            boolean defaultHttps = "https".equalsIgnoreCase(scheme) && port == 443;
            if (port > 0 && !defaultHttp && !defaultHttps) {
                host = host + ":" + port;
            }
        }

        String contextPath = request.getContextPath();
        return scheme + "://" + host + (hasText(contextPath) ? contextPath : "");
    }

    private String firstHeaderValue(HttpServletRequest request, String headerName) {
        String header = request.getHeader(headerName);
        if (!hasText(header)) {
            return "";
        }
        return header.split(",")[0].trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
