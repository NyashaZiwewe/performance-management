package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

@Service
public class CommonService {

    @Autowired
    HttpSession session;
    @Autowired
    ReportingDateRepository repository;
    private final Environment environment;

    public CommonService(Environment environment) {
        this.environment = environment;
    }

    public String getInitials(String fullName) {
        String words[] = fullName.split(" ");
        StringBuilder builder = new StringBuilder();
        for(String word : words) {
            builder.append(word.charAt(0));
        }
        return builder.toString();
    }

    public Account getLoggedUser(){
        return (Account) session.getAttribute("loggedUser");
    }

    public ReportingDate getActiveReportingDate(HttpServletRequest request){
        try {
            return getActiveReportingDate();
        }catch (Exception ignored){
            PortletUtils.addErrorMsg("There is no active reporting Date", request);
            return null;
        }
    }

    public ReportingDate getActiveReportingDate(){

        Account loggedUser = getLoggedUser();
        ReportingDate reportingDate = repository.findReportingDateByStatusAndAndReportingPeriod_ClientId(PMConstants.STATUS_ACTIVE, loggedUser.getClientId());
        return reportingDate;
    }

    public boolean isUserAllowed(String activity, Scorecard scorecard){

        boolean isUserAllowed = false;
        Account loggedUser = getLoggedUser();
        String approval_status = scorecard.getApprovalStatus();
        Account owner = scorecard.getOwner();


        if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_APPROVE_SCORECARD)){
            if(PMConstants.APPROVAL_STATUS_PENDING_APPROVAL.equalsIgnoreCase(approval_status) || PMConstants.APPROVAL_STATUS_REJECTED_BY_HR.equalsIgnoreCase(approval_status)){
                if(isSupervisor(owner)){
                    isUserAllowed = true;
                }
            } else if (PMConstants.APPROVAL_STATUS_APPROVED_BY_SUPERVISOR.equalsIgnoreCase(approval_status)) {
                if(PMConstants.MODERATOR.equalsIgnoreCase(loggedUser.getRole())){
                    isUserAllowed = true;
                }
            }

        }

        if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_TARGETS)){
            if(PMConstants.APPROVAL_STATUS_NEW.equalsIgnoreCase(approval_status)){
                if(isOwner(scorecard) || PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(loggedUser.getSpecial())){
                    isUserAllowed = true;
                }
            }
        }

        if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES)){
            if(PMConstants.APPROVAL_STATUS_APPROVED_BY_HR.equalsIgnoreCase(approval_status)){
                if(isOwner(scorecard) || PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(loggedUser.getSpecial())){
                    isUserAllowed = true;
                }
            }
        }

        if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES)){
            if(PMConstants.APPROVAL_STATUS_SCORED_BY_EMPLOYEE.equalsIgnoreCase(approval_status)){
                if(isSupervisor(owner)){
                    isUserAllowed = true;
                }
            }
        }

        if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES)){
            if(PMConstants.APPROVAL_STATUS_SCORED_BY_SUPERVISOR.equalsIgnoreCase(approval_status)){
                if(isSupervisor(owner)){
                    isUserAllowed = true;
                }
            }
        }

        if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES)){
            if(PMConstants.APPROVAL_STATUS_AGREED_BY_TWO.equalsIgnoreCase(approval_status)){
                if(PMConstants.MODERATOR.equalsIgnoreCase(loggedUser.getRole())){
                    isUserAllowed = true;
                }
            }
        }

        if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CLOSE_SCORECARD)){
            if(PMConstants.APPROVAL_STATUS_MODERATED_BY_HR.equalsIgnoreCase(approval_status)){
                if(PMConstants.IS_ADMIN.equalsIgnoreCase(loggedUser.getAdmin())){
                    isUserAllowed = true;
                }
            }
        }

        return isUserAllowed;
    }

    public boolean isSupervisor(Account employee){
        if(getLoggedUser().getId() == employee.getSupervisor().getClientId()){
            return true;
        }else{
            return false;
        }
    }

    public boolean isOwner(Scorecard scorecard){
        if(getLoggedUser().getId() == scorecard.getOwner().getId()){
            return true;
        }else{
            return false;
        }
    }

    public boolean isModerator(){
        if(PMConstants.MODERATOR.equalsIgnoreCase(getLoggedUser().getRole())){
            return true;
        }else{
            return false;
        }
    }

    public boolean isAdmin(){
        if(PMConstants.IS_ADMIN.equalsIgnoreCase(getLoggedUser().getAdmin())){
            return true;
        }else{
            return false;
        }
    }

    public boolean hasSpecialRights(){
        if(PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(getLoggedUser().getSpecial())){
            return true;
        }else{
            return false;
        }
    }

    public String getCurrentUrl(HttpServletRequest request) throws MalformedURLException {
        String urlString = request.getRequestURL().toString();

        URL url = new URL(urlString);
        String host = url.getHost();
        String protocol = url.getProtocol();
//        Integer port = url.getPort();
        Integer port = Integer.valueOf(Objects.requireNonNull(environment.getProperty("server.port")));
        host = protocol.concat("://"+ host.concat(":"+ port));

        return host;
    }

    public String encryptPassword(String pass){
        MessageDigest md = null;

        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        md.update(pass.getBytes());
        byte[] digest = md.digest();
        String password = DatatypeConverter.printHexBinary(digest).toLowerCase();
        return password;
    }
}
