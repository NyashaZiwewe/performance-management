package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public interface CommonService {
    String getInitials(String fullName);
    Account getLoggedUser();
    ReportingDate getActiveReportingDate(HttpServletRequest request);
    ReportingDate getActiveReportingDate();
    boolean isUserAllowed(String activity, Scorecard scorecard);
    boolean isSupervisor(Account employee);
    boolean isOwner(Scorecard scorecard);
    boolean isModerator();
    boolean isAdmin();
    boolean hasSpecialRights();
    String getCurrentUrl(HttpServletRequest request) throws MalformedURLException;
    String getHREmail();
    String getAdminEmail();
    String encodePassword(String password);
    boolean matchesPassword(String rawPassword, String storedPassword);
    boolean requiresPasswordUpgrade(String storedPassword);
    String encryptPassword(String pass);
    String hashResetToken(String token);
    String getFileExtention(String fileName);
}
