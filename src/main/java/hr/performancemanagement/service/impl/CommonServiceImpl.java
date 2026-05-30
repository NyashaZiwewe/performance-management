package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.utils.dto.ScorecardWorkflowDefinition;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


@Service
public class CommonServiceImpl implements hr.performancemanagement.service.api.CommonService {
    private static final String BCRYPT_PREFIX = "$2";

    @Autowired
    HttpSession session;
    @Autowired
    ReportingDateRepository repository;
    private final Environment environment;
    private final SystemSettingService systemSettingService;
    private final ScorecardWorkflowService scorecardWorkflowService;
    private final BCryptPasswordEncoder passwordEncoder;

    public CommonServiceImpl(Environment environment,
                             SystemSettingService systemSettingService,
                             ScorecardWorkflowService scorecardWorkflowService,
                             BCryptPasswordEncoder passwordEncoder) {
        this.environment = environment;
        this.systemSettingService = systemSettingService;
        this.scorecardWorkflowService = scorecardWorkflowService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String getInitials(String fullName) {
        String words[] = fullName.split(" ");
        StringBuilder builder = new StringBuilder();
        for(String word : words) {
            builder.append(word.charAt(0));
        }
        return builder.toString();
    }

    @Override
    public Account getLoggedUser(){
        return (Account) session.getAttribute("loggedUser");
    }

    @Override
    public ReportingDate getActiveReportingDate(HttpServletRequest request){
        try {
            return getActiveReportingDate();
        }catch (Exception ignored){
            PortletUtils.addErrorMsg("There is no active reporting Date", request);
            return null;
        }
    }

    @Override
    public ReportingDate getActiveReportingDate(){

        Account loggedUser = getLoggedUser();
        ReportingDate reportingDate = repository.findReportingDateByStatusAndAndReportingPeriod_ClientId(PMConstants.REPORTING_DATE_STATUS_OPEN, loggedUser.getClientId());
        if (reportingDate == null) {
            reportingDate = repository.findReportingDateByStatusAndAndReportingPeriod_ClientId(PMConstants.STATUS_ACTIVE, loggedUser.getClientId());
        }
        return reportingDate;
    }

    @Override
    public boolean isUserAllowed(String activity, Scorecard scorecard){

        if (scorecard == null) {
            return false;
        }

        boolean isUserAllowed = false;
        Account loggedUser = getLoggedUser();
        if (loggedUser == null) {
            return false;
        }
        String approval_status = scorecard.getApprovalStatus();
        Account owner = scorecard.getOwner();
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();


        if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_APPROVE_SCORECARD)){
            if(matchesStatus(approval_status, workflow.getPendingApprovalStatus())
                    || matchesStatus(approval_status, workflow.getRejectedByHrStatus())){
                if(isSupervisor(owner)){
                    isUserAllowed = true;
                }
            } else if (matchesStatus(approval_status, workflow.getApprovedBySupervisorStatus())) {
                if(PMConstants.MODERATOR.equalsIgnoreCase(loggedUser.getRole())){
                    isUserAllowed = true;
                }
            }

        }
        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_TARGETS)){
            if((matchesStatus(approval_status, workflow.getNewStatus())
                    || matchesStatus(approval_status, workflow.getRejectedBySupervisorStatus()))
                    && PMConstants.LOCK_STATUS_OPEN.equalsIgnoreCase(scorecard.getLockStatus())){
                if(isOwner(scorecard) || PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(loggedUser.getSpecial())){
                    isUserAllowed = true;
                }
            }
        }
        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES)){
            if(matchesStatus(approval_status, workflow.getApprovedByHrStatus())){
                if(isOwner(scorecard) || PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(loggedUser.getSpecial())){
                    isUserAllowed = true;
                }
            }
        }

        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES)){
            if(matchesStatus(approval_status, workflow.getApprovedOwnerScoresStatus())){
                if(isSupervisor(owner)){
                    isUserAllowed = true;
                }
            }
        }

        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES)){
            if(matchesStatus(approval_status, workflow.getScoredBySupervisorStatus())){
                if(isSupervisor(owner)){
                    isUserAllowed = true;
                }
            }
        }

        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES)){
            if(matchesStatus(approval_status, workflow.getApprovedAgreedScoresStatus())){
                if(PMConstants.MODERATOR.equalsIgnoreCase(loggedUser.getRole())){
                    isUserAllowed = true;
                }
            }
        }
        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_APPROVE_OWNER_SCORES)){
            if(matchesStatus(approval_status, workflow.getScoredByEmployeeStatus())){
                if(isSupervisor(owner)){
                    isUserAllowed = true;
                }
            }
        }
        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_APPROVE_AGREED_SCORES)){
            if(matchesStatus(approval_status, workflow.getAgreedByTwoStatus())){
                if(PMConstants.MODERATOR.equalsIgnoreCase(loggedUser.getRole())){
                    isUserAllowed = true;
                }
            }
        }

        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CLOSE_SCORECARD)){
            if(matchesStatus(approval_status, workflow.getModeratedByHrStatus())){
                if(PMConstants.IS_ADMIN.equalsIgnoreCase(loggedUser.getAdmin())){
                    isUserAllowed = true;
                }
            }
        }

        return isUserAllowed;
    }

    private boolean matchesStatus(String actualStatus, String expectedStatus) {
        return scorecardWorkflowService.matches(actualStatus, expectedStatus);
    }

    @Override
    public boolean isSupervisor(Account employee){
        Account loggedUser = getLoggedUser();
        if(loggedUser != null && employee != null && employee.getSupervisor() != null && loggedUser.getId() == employee.getSupervisor().getId()){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean isOwner(Scorecard scorecard){
        Account loggedUser = getLoggedUser();
        if(loggedUser != null && scorecard != null && scorecard.getOwner() != null && loggedUser.getId() == scorecard.getOwner().getId()){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean isModerator(){
        if(PMConstants.MODERATOR.equalsIgnoreCase(getLoggedUser().getRole())){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean isAdmin(){
        if(PMConstants.IS_ADMIN.equalsIgnoreCase(getLoggedUser().getAdmin())){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean hasSpecialRights(){
        if(PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(getLoggedUser().getSpecial())){
            return true;
        }else{
            return false;
        }
    }

    public String getCurrentUrl(HttpServletRequest request) throws MalformedURLException {
//        String urlString = request.getRequestURL().toString();
//
//        URL url = new URL(urlString);
//        String host = url.getHost();
//        String protocol = url.getProtocol();
//        Integer port = Integer.valueOf(Objects.requireNonNull(environment.getProperty("server.port")));
//        host = protocol.concat("://"+ host.concat(":"+ port));
        String host = systemSettingService.getHostUrl();
        if (host == null || host.trim().isEmpty()) {
            host = environment.getProperty("host.url");
        }
        return host;
    }

    @Override
    public String getHREmail(){
        String hrEmail = systemSettingService.getHREmail();
        if (hrEmail == null || hrEmail.trim().isEmpty()) {
            hrEmail = environment.getProperty("email.hr");
        }
        return hrEmail;
    }

    @Override
    public String getAdminEmail(){
        String adminEmail = systemSettingService.getAdminEmail();
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            adminEmail = environment.getProperty("email.admin");
        }
        return adminEmail;
    }

    @Override
    public String encodePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        return passwordEncoder.encode(password);
    }

    @Override
    public boolean matchesPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.trim().isEmpty()) {
            return false;
        }
        if (storedPassword.startsWith(BCRYPT_PREFIX)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword) || storedPassword.equals(encryptPassword(rawPassword));
    }

    @Override
    public boolean requiresPasswordUpgrade(String storedPassword) {
        return storedPassword != null && !storedPassword.startsWith(BCRYPT_PREFIX);
    }

    @Override
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

    @Override
    public String hashResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Reset token cannot be empty");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printHexBinary(digest).toLowerCase();
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException(exception);
        }
    }


    @Override
    public String getFileExtention(String fileName) {
        int index = fileName.lastIndexOf('.');
        try {
            if (index > 0) {
                String extension = fileName.substring(index + 1);
                return extension;
            } else {
                return null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}
