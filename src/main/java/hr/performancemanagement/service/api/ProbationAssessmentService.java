package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.*;
import hr.performancemanagement.utils.constants.PMConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public interface ProbationAssessmentService {
    List<ProbationAssessment> listVisibleAssessments();
    ProbationAssessment getAssessmentById(long id);
    ProbationAssessment createAssessment(ProbationAssessment assessment);
    ProbationAssessment updateAssessmentCore(ProbationAssessment updatedAssessment);
    List<ProbationAssessmentDimension> listAssessmentDimensions(long assessmentId);
    ProbationAssessmentDimension saveDimensionResponse(long assessmentId, long dimensionTemplateId, String strengths, String areasForImprovement);
    List<ProbationKpi> listKpis(long assessmentId);
    ProbationKpi addKpi(long assessmentId, ProbationKpi kpi);
    ProbationKpi updateKpi(ProbationKpi newKpi);
    void deleteKpi(long kpiId);
    List<ProbationAssessmentApproval> listApprovalHistory(long assessmentId);
    boolean submitAssessment(long assessmentId, String remarks);
    boolean approveAssessment(long assessmentId, String remarks);
    boolean rejectAssessment(long assessmentId, String remarks);
    boolean canLoggedUserSubmit(ProbationAssessment assessment);
    boolean canLoggedUserApprove(ProbationAssessment assessment);
    boolean isLoggedUserOwner(ProbationAssessment assessment);
    boolean isLoggedUserSupervisor(ProbationAssessment assessment);
}
