package hr.performancemanagement.config;

import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Component
public class BootstrapAdminInterceptor implements HandlerInterceptor {

    private static final List<String> ALLOWED_PATH_PREFIXES = Arrays.asList(
            "/",
            "/accounts",
            "/departments",
            "/divisions",
            "/perspectives",
            "/reporting-periods",
            "/scorecard-models",
            "/system-settings",
            "/logout",
            "/css",
            "/js",
            "/img",
            "/fonts",
            "/font-awesome"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object bootstrapAdmin = request.getSession().getAttribute("bootstrapAdmin");
        if (!(bootstrapAdmin instanceof Boolean) || !((Boolean) bootstrapAdmin)) {
            return true;
        }

        String uri = request.getRequestURI();
        boolean allowed = ALLOWED_PATH_PREFIXES.stream()
                .anyMatch(prefix -> "/".equals(prefix) ? "/".equals(uri) : uri.startsWith(prefix));

        if (allowed) {
            return true;
        }

        PortletUtils.addErrorMsg(
                "Bootstrap admin can only access setup pages for system settings, reporting periods, scorecard models, perspectives, accounts, departments, and divisions.",
                request);
        response.sendRedirect("/");
        return false;
    }
}
