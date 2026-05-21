package hr.performancemanagement.controllers.settings;

import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.SystemSettingService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Pages;
import hr.performancemanagement.utils.wrappers.SystemSettingsWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/system-settings")
public class SystemSettingsController {

    @Autowired
    private CommonService commonService;
    @Autowired
    private SystemSettingService systemSettingService;

    @RequestMapping
    public ModelAndView viewSettings(HttpServletRequest request) {
        if (!commonService.isAdmin() && !commonService.hasSpecialRights()) {
            PortletUtils.addErrorMsg("You are not allowed to view system settings.", request);
            return new ModelAndView("redirect:/");
        }

        String runtimeHostUrl = resolveRuntimeHostUrl(request);
        systemSettingService.syncHostUrl(runtimeHostUrl);
        SystemSettingsWrapper settingsWrapper = systemSettingService.getSettingsWrapper();
        settingsWrapper.setHostUrl(runtimeHostUrl);

        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SYSTEM_SETTINGS);
        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "System Settings");
        modelAndView.addObject("pageTitle", "System Settings");
        modelAndView.addObject("settingsWrapper", settingsWrapper);
        PortletUtils.addMessagesToPage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public String saveSettings(HttpServletRequest request, SystemSettingsWrapper wrapper) {
        if (!commonService.isAdmin() && !commonService.hasSpecialRights()) {
            PortletUtils.addErrorMsg("You are not allowed to update system settings.", request);
            return "redirect:/";
        }
        try {
            String runtimeHostUrl = resolveRuntimeHostUrl(request);
            wrapper.setHostUrl(runtimeHostUrl);
            systemSettingService.syncHostUrl(runtimeHostUrl);
            systemSettingService.saveSettings(wrapper);
            PortletUtils.addInfoMsg("System settings successfully updated.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Settings update failed: " + exception.getMessage(), request);
        }
        return "redirect:/system-settings";
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
