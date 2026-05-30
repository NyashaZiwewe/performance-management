package hr.performancemanagement.utils.wrappers;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemSettingsWrapper {

    private String companyName;
    private String companyLogo;
    private String systemName;
    private String hostUrl;
    private String multipartLocation;
    private String hrEmail;
    private String adminEmail;
    private Boolean bootstrapAdminEnabled;
    private String mailFromName;
    private String mailFromEmail;
    private String mailHost;
    private String mailPort;
    private String mailUsername;
    private String mailPassword;
    private boolean mailPasswordConfigured;
    private String scorecardStatusNew;
    private String scorecardStatusPendingApproval;
    private String scorecardStatusApprovedBySupervisor;
    private String scorecardStatusRejectedBySupervisor;
    private String scorecardStatusApprovedByHr;
    private String scorecardStatusRejectedByHr;
    private String scorecardStatusScoredByEmployee;
    private String scorecardStatusScoredBySupervisor;
    private String scorecardStatusAgreedByTwo;
    private String scorecardStatusModeratedByHr;
    private String scorecardStatusClosed;
    private String scorecardWorkflowSequence;
}
