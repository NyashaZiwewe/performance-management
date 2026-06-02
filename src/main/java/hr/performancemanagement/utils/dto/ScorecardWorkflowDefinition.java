package hr.performancemanagement.utils.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ScorecardWorkflowDefinition {

    private final String newStatus;
    private final String pendingApprovalStatus;
    private final String approvedBySupervisorStatus;
    private final String rejectedBySupervisorStatus;
    private final String approvedByHrStatus;
    private final String rejectedByHrStatus;
    private final String scoredByEmployeeStatus;
    private final String approvedOwnerScoresStatus;
    private final String scoredBySupervisorStatus;
    private final String agreedByTwoStatus;
    private final String approvedAgreedScoresStatus;
    private final String moderatedByHrStatus;
    private final String closedStatus;
    private final List<String> orderedStatuses;
    private final Map<String, String> statusCssClasses;
    private final Map<String, String> statusDisplayLabels;
    private final Map<String, String> statusStageNames;
    private final Map<String, String> statusActionButtonLabels;
    private final Map<String, String> roleStageNames;
    private final Map<String, String> roleActionButtonLabels;
    private final Map<String, String> roleRejectionButtonLabels;

    public ScorecardWorkflowDefinition(String newStatus,
                                       String pendingApprovalStatus,
                                       String approvedBySupervisorStatus,
                                       String rejectedBySupervisorStatus,
                                       String approvedByHrStatus,
                                       String rejectedByHrStatus,
                                       String scoredByEmployeeStatus,
                                       String approvedOwnerScoresStatus,
                                       String scoredBySupervisorStatus,
                                       String agreedByTwoStatus,
                                       String approvedAgreedScoresStatus,
                                       String moderatedByHrStatus,
                                       String closedStatus,
                                       List<String> orderedStatuses,
                                       Map<String, String> statusCssClasses,
                                       Map<String, String> statusDisplayLabels,
                                       Map<String, String> statusStageNames,
                                       Map<String, String> statusActionButtonLabels,
                                       Map<String, String> roleStageNames,
                                       Map<String, String> roleActionButtonLabels,
                                       Map<String, String> roleRejectionButtonLabels) {
        this.newStatus = newStatus;
        this.pendingApprovalStatus = pendingApprovalStatus;
        this.approvedBySupervisorStatus = approvedBySupervisorStatus;
        this.rejectedBySupervisorStatus = rejectedBySupervisorStatus;
        this.approvedByHrStatus = approvedByHrStatus;
        this.rejectedByHrStatus = rejectedByHrStatus;
        this.scoredByEmployeeStatus = scoredByEmployeeStatus;
        this.approvedOwnerScoresStatus = approvedOwnerScoresStatus;
        this.scoredBySupervisorStatus = scoredBySupervisorStatus;
        this.agreedByTwoStatus = agreedByTwoStatus;
        this.approvedAgreedScoresStatus = approvedAgreedScoresStatus;
        this.moderatedByHrStatus = moderatedByHrStatus;
        this.closedStatus = closedStatus;
        this.orderedStatuses = Collections.unmodifiableList(new ArrayList<String>(orderedStatuses));
        this.statusCssClasses = Collections.unmodifiableMap(new LinkedHashMap<String, String>(statusCssClasses));
        this.statusDisplayLabels = Collections.unmodifiableMap(new LinkedHashMap<String, String>(statusDisplayLabels));
        this.statusStageNames = Collections.unmodifiableMap(new LinkedHashMap<String, String>(statusStageNames));
        this.statusActionButtonLabels = Collections.unmodifiableMap(new LinkedHashMap<String, String>(statusActionButtonLabels));
        this.roleStageNames = Collections.unmodifiableMap(new LinkedHashMap<String, String>(roleStageNames));
        this.roleActionButtonLabels = Collections.unmodifiableMap(new LinkedHashMap<String, String>(roleActionButtonLabels));
        this.roleRejectionButtonLabels = Collections.unmodifiableMap(new LinkedHashMap<String, String>(roleRejectionButtonLabels));
    }
}
