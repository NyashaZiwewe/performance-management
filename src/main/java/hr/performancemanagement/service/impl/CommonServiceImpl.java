package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Client;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.ScorecardReportingDateStage;
import hr.performancemanagement.repository.ClientRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
    private final ScorecardReportingDateStageService scorecardReportingDateStageService;
    private final ClientRepository clientRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public CommonServiceImpl(Environment environment,
                             SystemSettingService systemSettingService,
                             ScorecardWorkflowService scorecardWorkflowService,
                             ScorecardReportingDateStageService scorecardReportingDateStageService,
                             ClientRepository clientRepository,
                             BCryptPasswordEncoder passwordEncoder) {
        this.environment = environment;
        this.systemSettingService = systemSettingService;
        this.scorecardWorkflowService = scorecardWorkflowService;
        this.scorecardReportingDateStageService = scorecardReportingDateStageService;
        this.clientRepository = clientRepository;
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
    public Client getConfiguredClient() {
        Account loggedUser = getLoggedUser();
        if (loggedUser != null && loggedUser.getClient() != null && loggedUser.getClient().getClientId() > 0) {
            return loggedUser.getClient();
        }

        String companyName = systemSettingService.getCompanyName();
        if (companyName != null && !companyName.trim().isEmpty()) {
            Client configuredClient = clientRepository.findClientByClientIgnoreCase(companyName.trim());
            if (configuredClient != null && configuredClient.getClientId() > 0) {
                return configuredClient;
            }
        }

        Client mandatoryClient = clientRepository.findFirstByIsMandatoryTrueOrderByClientIdAsc();
        if (mandatoryClient != null && mandatoryClient.getClientId() > 0) {
            return mandatoryClient;
        }

        if (loggedUser != null && loggedUser.getClientId() > 0) {
            Client loggedUserClient = clientRepository.findById(loggedUser.getClientId()).orElse(null);
            if (loggedUserClient != null && loggedUserClient.getClientId() > 0) {
                return loggedUserClient;
            }
        }

        return clientRepository.findFirstByOrderByClientIdAsc();
    }

    @Override
    public long getConfiguredClientId() {
        Account loggedUser = getLoggedUser();
        if (loggedUser != null && loggedUser.getClient() != null && loggedUser.getClient().getClientId() > 0) {
            return loggedUser.getClient().getClientId();
        }

        Client configuredClient = getConfiguredClient();
        if (configuredClient != null && configuredClient.getClientId() > 0) {
            return configuredClient.getClientId();
        }
        if (loggedUser != null && loggedUser.getClientId() > 0) {
            return loggedUser.getClientId();
        }
        return 1L;
    }

    @Override
    public ReportingDate getActiveReportingDate(HttpServletRequest request){
        long clientId = getConfiguredClientId();
        if (hasMultipleOpenOrActiveReportingDates(clientId)) {
            PortletUtils.addErrorMsg("Multiple OPEN/ACTIVE reporting dates were detected. Score capture is blocked until this is corrected.", request);
            return null;
        }

        ReportingDate reportingDate = getActiveReportingDate();
        if (reportingDate == null) {
            PortletUtils.addErrorMsg("There is no active reporting Date", request);
        }
        return reportingDate;
    }

    @Override
    public ReportingDate getActiveReportingDate(){
        long clientId = getConfiguredClientId();
        List<ReportingDate> reportingDates = repository.findReportingDatesByReportingPeriod_ClientIdAndStatusInOrderByDateDescIdDesc(
                clientId,
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        );
        if (reportingDates == null || reportingDates.isEmpty()) {
            return null;
        }
        for (ReportingDate reportingDate : reportingDates) {
            if (isCurrentActiveReportingPeriod(reportingDate == null ? null : reportingDate.getReportingPeriod())) {
                return reportingDate;
            }
        }
        return null;
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
        if (isScoreCaptureActivity(activity) && hasMultipleOpenOrActiveReportingDates(getConfiguredClientId())) {
            return false;
        }
        String approval_status = scorecard.getApprovalStatus();
        String contractRole = resolveContractStageRole(scorecard);
        String reportingDateRole = null;
        if (isScoreWorkflowActivity(activity)) {
            if (!isActiveScoreCaptureWindow(scorecard)) {
                return false;
            }
            reportingDateRole = resolveActiveReportingDateRole(activity, scorecard);
        }
        Account owner = scorecard.getOwner();
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();


        if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_APPROVE_SCORECARD)){
            if(matchesRole(contractRole, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR)){
                if(isSupervisor(owner)){
                    isUserAllowed = true;
                }
            } else if (matchesRole(contractRole, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR)) {
                if(PMConstants.MODERATOR.equalsIgnoreCase(loggedUser.getRole())){
                    isUserAllowed = true;
                }
            } else if (matchesStatus(approval_status, workflow.getPendingApprovalStatus())
                    || matchesStatus(approval_status, workflow.getRejectedByHrStatus())) {
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
            if((matchesRole(contractRole, PMConstants.SCORECARD_STAGE_NEW)
                    || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS)
                    || matchesStatus(approval_status, workflow.getNewStatus())
                    || matchesStatus(approval_status, workflow.getRejectedBySupervisorStatus()))
                    && isScorecardInActiveReportingPeriod(scorecard)
                    && PMConstants.LOCK_STATUS_OPEN.equalsIgnoreCase(scorecard.getLockStatus())){
                if(isOwner(scorecard)
                        || PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(loggedUser.getSpecial())
                        || PMConstants.IS_ADMIN.equalsIgnoreCase(loggedUser.getAdmin())){
                    isUserAllowed = true;
                }
            }
        }
        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES)){
            if(!isContractReadyForScoring(scorecard, workflow)){
                return false;
            }
            if(matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_OWNER_SCORING)){
                if(isOwner(scorecard) || PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(loggedUser.getSpecial())){
                    isUserAllowed = true;
                }
            }
        }

        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES)){
            if(!isContractReadyForScoring(scorecard, workflow)){
                return false;
            }
            if(matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING)){
                if(isSupervisor(owner)){
                    isUserAllowed = true;
                }
            }
        }

        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES)){
            if(!isContractReadyForScoring(scorecard, workflow)){
                return false;
            }
            if(matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING)){
                if(isSupervisor(owner)){
                    isUserAllowed = true;
                }
            }
        }

        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES)){
            if(!isContractReadyForScoring(scorecard, workflow)){
                return false;
            }
            if(matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING)){
                if(PMConstants.MODERATOR.equalsIgnoreCase(loggedUser.getRole())){
                    isUserAllowed = true;
                }
            }
        }
        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_APPROVE_OWNER_SCORES)){
            if(!isContractReadyForScoring(scorecard, workflow)){
                return false;
            }
            if(matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL)){
                if(isSupervisor(owner)){
                    isUserAllowed = true;
                }
            }
        }
        else if(activity.equalsIgnoreCase(PMConstants.ACTIVITY_APPROVE_AGREED_SCORES)){
            if(!isContractReadyForScoring(scorecard, workflow)){
                return false;
            }
            if(matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL)){
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

    private boolean isActiveScoreCaptureWindow(Scorecard scorecard) {
        ReportingDate reportingDate = getActiveReportingDate();
        return isReportingDateOpen(reportingDate) && reportingDateBelongsToScorecard(scorecard, reportingDate);
    }

    private boolean isReportingDateOpen(ReportingDate reportingDate) {
        return reportingDate != null
                && reportingDate.getStatus() != null
                && PMConstants.REPORTING_DATE_STATUS_OPEN.equalsIgnoreCase(reportingDate.getStatus().trim());
    }

    private boolean reportingDateBelongsToScorecard(Scorecard scorecard, ReportingDate reportingDate) {
        if (scorecard == null || scorecard.getReportingPeriod() == null || reportingDate == null
                || reportingDate.getReportingPeriod() == null) {
            return false;
        }
        return scorecard.getReportingPeriod().getId() == reportingDate.getReportingPeriod().getId();
    }

    private boolean isScorecardInActiveReportingPeriod(Scorecard scorecard) {
        return scorecard != null
                && scorecard.getReportingPeriod() != null
                && scorecard.getReportingPeriod().getStatus() != null
                && PMConstants.STATUS_ACTIVE.equalsIgnoreCase(scorecard.getReportingPeriod().getStatus().trim());
    }

    private boolean isScoreCaptureActivity(String activity) {
        return PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES.equalsIgnoreCase(activity)
                || PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES.equalsIgnoreCase(activity)
                || PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES.equalsIgnoreCase(activity)
                || PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES.equalsIgnoreCase(activity);
    }

    private String resolveActiveReportingDateRole(String activity, Scorecard scorecard) {
        if (!isScoreWorkflowActivity(activity) || scorecard == null) {
            return null;
        }
        ReportingDate reportingDate = getActiveReportingDate();
        if (reportingDate == null) {
            return null;
        }
        String roleKey = scorecardReportingDateStageService.getCurrentRoleKey(scorecard, reportingDate);
        if (roleKey != null) {
            return roleKey;
        }
        ScorecardReportingDateStage stage = scorecardReportingDateStageService.getOrCreateStage(scorecard, reportingDate);
        if (stage == null || stage.getApprovalStage() == null || stage.getApprovalStage().getRoleKey() == null) {
            return null;
        }
        return stage.getApprovalStage().getRoleKey();
    }

    private boolean isScoreWorkflowActivity(String activity) {
        return isScoreCaptureActivity(activity)
                || PMConstants.ACTIVITY_APPROVE_OWNER_SCORES.equalsIgnoreCase(activity)
                || PMConstants.ACTIVITY_APPROVE_AGREED_SCORES.equalsIgnoreCase(activity);
    }

    private boolean isContractReadyForScoring(Scorecard scorecard, ScorecardWorkflowDefinition workflow) {
        if (scorecard == null || workflow == null) {
            return false;
        }
        if (scorecard.getApprovalStage() != null && scorecard.getApprovalStage().getRoleKey() != null) {
            String roleKey = scorecard.getApprovalStage().getRoleKey().trim().toUpperCase();
            if (PMConstants.SCORECARD_STAGE_OWNER_SCORING.equals(roleKey)
                    || PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL.equals(roleKey)
                    || PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING.equals(roleKey)
                    || PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING.equals(roleKey)
                    || PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL.equals(roleKey)
                    || PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING.equals(roleKey)
                    || PMConstants.SCORECARD_STAGE_CLOSED.equals(roleKey)) {
                return true;
            }
        }
        String approvalStatus = scorecard.getApprovalStatus();
        return matchesStatus(approvalStatus, workflow.getApprovedByHrStatus())
                || matchesStatus(approvalStatus, workflow.getScoredByEmployeeStatus())
                || matchesStatus(approvalStatus, workflow.getApprovedOwnerScoresStatus())
                || matchesStatus(approvalStatus, workflow.getScoredBySupervisorStatus())
                || matchesStatus(approvalStatus, workflow.getAgreedByTwoStatus())
                || matchesStatus(approvalStatus, workflow.getApprovedAgreedScoresStatus())
                || matchesStatus(approvalStatus, workflow.getModeratedByHrStatus())
                || matchesStatus(approvalStatus, workflow.getClosedStatus());
    }

    private boolean matchesRole(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private String resolveContractStageRole(Scorecard scorecard) {
        if (scorecard == null) {
            return null;
        }
        if (scorecard.getApprovalStage() != null && scorecard.getApprovalStage().getRoleKey() != null
                && !scorecard.getApprovalStage().getRoleKey().trim().isEmpty()) {
            return scorecard.getApprovalStage().getRoleKey().trim().toUpperCase();
        }
        return mapStatusToContractRole(scorecard.getApprovalStatus());
    }

    private String mapStatusToContractRole(String approvalStatus) {
        if (approvalStatus == null || approvalStatus.trim().isEmpty()) {
            return PMConstants.SCORECARD_STAGE_NEW;
        }
        String status = approvalStatus.trim().toUpperCase();
        switch (status) {
            case PMConstants.APPROVAL_STATUS_NEW:
                return PMConstants.SCORECARD_STAGE_NEW;
            case PMConstants.APPROVAL_STATUS_PENDING_APPROVAL:
            case PMConstants.APPROVAL_STATUS_REJECTED_BY_HR:
                return PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR;
            case PMConstants.APPROVAL_STATUS_APPROVED_BY_SUPERVISOR:
                return PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR;
            case PMConstants.APPROVAL_STATUS_REJECTED_BY_SUPERVISOR:
                return PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS;
            case PMConstants.APPROVAL_STATUS_APPROVED_BY_HR:
                return PMConstants.SCORECARD_STAGE_OWNER_SCORING;
            default:
                return null;
        }
    }

    private boolean hasMultipleOpenOrActiveReportingDates(long clientId) {
        if (clientId <= 0) {
            return false;
        }
        List<ReportingDate> reportingDates = repository.findReportingDatesByReportingPeriod_ClientIdAndStatusInOrderByDateDescIdDesc(
                clientId,
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        );
        List<ReportingDate> validReportingDates = new ArrayList<>();
        for (ReportingDate reportingDate : reportingDates) {
            if (isCurrentActiveReportingPeriod(reportingDate == null ? null : reportingDate.getReportingPeriod())) {
                validReportingDates.add(reportingDate);
            }
        }
        return validReportingDates.size() > 1;
    }

    private boolean isCurrentActiveReportingPeriod(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null
                || reportingPeriod.getStatus() == null
                || !PMConstants.STATUS_ACTIVE.equalsIgnoreCase(reportingPeriod.getStatus().trim())) {
            return false;
        }
        try {
            LocalDate startDate = parsePeriodDate(reportingPeriod.getStartDate());
            LocalDate endDate = parsePeriodDate(reportingPeriod.getEndDate());
            LocalDate today = LocalDate.now();
            return !startDate.isAfter(endDate) && !today.isBefore(startDate) && !today.isAfter(endDate);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private LocalDate parsePeriodDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Reporting period date is required.");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Reporting period date must be in YYYY-MM-DD format.");
        }
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
