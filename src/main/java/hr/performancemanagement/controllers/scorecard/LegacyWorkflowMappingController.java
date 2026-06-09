package hr.performancemanagement.controllers.scorecard;

import hr.performancemanagement.entities.OverallScore;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.service.OverallScoreService;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.LegacyWorkflowMappingService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/legacy-workflow-mapping")
public class LegacyWorkflowMappingController {

    private final LegacyWorkflowMappingService legacyWorkflowMappingService;
    private final OverallScoreService overallScoreService;
    private final CommonService commonService;

    public LegacyWorkflowMappingController(LegacyWorkflowMappingService legacyWorkflowMappingService,
                                           OverallScoreService overallScoreService,
                                           CommonService commonService) {
        this.legacyWorkflowMappingService = legacyWorkflowMappingService;
        this.overallScoreService = overallScoreService;
        this.commonService = commonService;
    }

    @RequestMapping
    public ModelAndView viewMappings(HttpServletRequest request) {
        if (!hasAccess(request)) {
            return new ModelAndView("redirect:/");
        }
        List<Scorecard> scorecards = legacyWorkflowMappingService.listUnmappedScorecards();
        ModelAndView modelAndView = new ModelAndView(Pages.LEGACY_WORKFLOW_MAPPING);
        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Legacy Workflow Mapping");
        modelAndView.addObject("pageTitle", "Legacy Workflow Mapping");
        modelAndView.addObject("unmappedScorecards", scorecards);
        modelAndView.addObject("workflowStages", legacyWorkflowMappingService.listActiveWorkflowStages());
        modelAndView.addObject("mappingAudit", legacyWorkflowMappingService.listRecentAudit());
        modelAndView.addObject("scoreHistorySummaries", buildScoreHistorySummaries(scorecards));
        PortletUtils.addMessagesToPage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/assign", method = RequestMethod.POST)
    public String assignMappings(@RequestParam(value = "scorecardIds", required = false) List<Long> scorecardIds,
                                 long workflowStageId,
                                 @RequestParam(value = "initializeReportingDateStages", defaultValue = "false")
                                 boolean initializeReportingDateStages,
                                 String reason,
                                 HttpServletRequest request) {
        if (!hasAccess(request)) {
            return "redirect:/";
        }
        try {
            int mappedCount = legacyWorkflowMappingService.saveWorkflowStageMappings(
                    scorecardIds == null ? Collections.emptyList() : scorecardIds,
                    workflowStageId,
                    initializeReportingDateStages,
                    reason
            );
            PortletUtils.addInfoMsg(mappedCount + " legacy scorecard(s) mapped successfully.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Could not map legacy scorecards: " + exception.getMessage(), request);
        }
        return "redirect:/legacy-workflow-mapping";
    }

    private Map<Long, String> buildScoreHistorySummaries(List<Scorecard> scorecards) {
        Map<Long, String> summaries = new LinkedHashMap<>();
        if (scorecards == null) {
            return summaries;
        }
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null) {
                continue;
            }
            List<OverallScore> scores = overallScoreService.getOverallScoreByScorecard(scorecard);
            summaries.put(scorecard.getId(), summarizeScoreHistory(scores));
        }
        return summaries;
    }

    private String summarizeScoreHistory(List<OverallScore> scores) {
        if (scores == null || scores.isEmpty()) {
            return "No dated overall scores";
        }
        boolean employee = false;
        boolean manager = false;
        boolean agreed = false;
        boolean moderated = false;
        for (OverallScore score : scores) {
            if (score == null) {
                continue;
            }
            employee = employee || positive(score.getEmployeeOverall());
            manager = manager || positive(score.getManagerOverall());
            agreed = agreed || positive(score.getAgreedOverall());
            moderated = moderated || positive(score.getModeratedOverall());
        }
        String latestProgress = moderated ? "Moderated scores exist"
                : agreed ? "Agreed scores exist"
                : manager ? "Supervisor scores exist"
                : employee ? "Owner scores exist"
                : "No captured overall scores";
        return scores.size() + " reporting date(s); " + latestProgress;
    }

    private boolean positive(Double value) {
        return value != null && value > 0.0;
    }

    private boolean hasAccess(HttpServletRequest request) {
        if (commonService.isAdmin() || commonService.hasSpecialRights()) {
            return true;
        }
        PortletUtils.addErrorMsg("Only administrators can map legacy scorecards.", request);
        return false;
    }
}
