package hr.performancemanagement.controllers.assessment;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.service.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping(value = "/performance-review")
public class AssessmentController {

    @Autowired
    ReportingPeriodService reportingPeriodService;
    @Autowired
    ScorecardService scorecardService;

    @Autowired
    PerformanceImprovementPlanService performanceImprovementPlanService;
    @Autowired
    private final GoalService goalService;

    @Autowired
    HttpSession session;

    public AssessmentController(GoalService goalService) {
        this.goalService = goalService;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {
        modelAndView.addObject("pageDomain", "Performance Review");
        modelAndView.addObject("pageName", "Assessments");
        modelAndView.addObject("profile", "moderator");
        PortletUtils.addMessagesToPage(modelAndView, request);
    }


    @RequestMapping(value="/view-scores-select-year")
    public ModelAndView viewScoresSelectYear(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORES_SELECT_YEAR);
        modelAndView.addObject("pageTitle", "Select Reporting Period");
        List<ReportingPeriod> REPORTING_PERIODS_LIST = reportingPeriodService.listAllReportingPeriods();
        modelAndView.addObject("reportingPeriodsList", REPORTING_PERIODS_LIST);
        preparePage(modelAndView, request);
        return modelAndView;
    }


    @RequestMapping(value = "/view-scores-select-year", method = RequestMethod.POST)
    public String goToViewScores(HttpServletRequest request, long reportingPeriodId) {

        return "redirect:/performance-review/view-scores/"+ reportingPeriodId;
    }


    @RequestMapping("/view-scores/{id}")
    public ModelAndView viewScores(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORES);
        modelAndView.addObject("pageTitle", "View Scores");

        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(id);
        String startDate = reportingPeriod.getStartDate();
        String endDate = reportingPeriod.getEndDate();
        List<Scorecard> scoresList = scorecardService.getScoresByPeriodId(reportingPeriod);

        Account loggedUser = (Account) session.getAttribute("loggedUser");
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        modelAndView.addObject("scoresList", scoresList);
        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("role", role);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
        PortletUtils.addInfoMsg("Showing scores for the period: "+ startDate + " to "+ endDate, request);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/view-performance-report/{id}")
    public ModelAndView viewPerformanceReport(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_REPORT);
        modelAndView.addObject("pageTitle", "View Performance Score");

        Scorecard scoreCard = scorecardService.getScorecardById(id);
        List<Goal> GOALS_LIST = goalService.listAllGoals(id);
        ReportingPeriod reportingPeriod = scoreCard.getReportingPeriod();
        String startDate = reportingPeriod.getStartDate();
        String endDate = reportingPeriod.getEndDate();

        Account loggedUser = (Account) session.getAttribute("loggedUser");
        Account owner = scoreCard.getOwner();
        List<PerformanceImprovementPlan> pips = performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        double averageModeratedScore = goalService.getAverageModeratorScore(id);
        double weightedScore;
        try {
            weightedScore = (averageModeratedScore / 5 ) * 100;
        }catch (Exception e){
            weightedScore = 0;
        }

        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("pips", pips);
        modelAndView.addObject("owner", owner);
        modelAndView.addObject("role", role);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
        modelAndView.addObject("scorecard", scoreCard);
        modelAndView.addObject("goalsList", GOALS_LIST);
        modelAndView.addObject("averageModeratedScore", averageModeratedScore);
        modelAndView.addObject("weightedScore", weightedScore);
        PortletUtils.addInfoMsg("Showing scores for the period: "+ startDate + " to "+ endDate, request);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/view-chart/{id}")
    public ModelAndView viewChart(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_CHART);
        modelAndView.addObject("pageTitle", "View Chart");
        preparePage(modelAndView, request);
        return modelAndView;
    }

}
