package hr.performancemanagement.controllers.assessment;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.service.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final TargetService targetService;
    @Autowired
    private final StrategicObjectiveService strategicObjectiveService;
    @Autowired
    private final GoalService goalService;
    @Autowired
    private final AccountService accountService;
    private static final String PDF_RESOURCES = "";
    @Autowired
    private SpringTemplateEngine templateEngine;
    @Autowired
    HttpSession session;

    public AssessmentController(TargetService targetService, StrategicObjectiveService strategicObjectiveService, GoalService goalService, AccountService accountService) {
        this.targetService = targetService;
        this.strategicObjectiveService = strategicObjectiveService;
        this.goalService = goalService;
        this.accountService = accountService;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {
        List<Account> ACCOUNTS_LIST = accountService.listAllAccounts();
        modelAndView.addObject("pageDomain", "Performance Review");
        modelAndView.addObject("pageName", "Assessments");
        modelAndView.addObject("profile", "moderator");
        modelAndView.addObject("accountsList", ACCOUNTS_LIST);
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

    @RequestMapping(value="/view-performance-levels-select-year")
    public ModelAndView viewPerformanceLevelsSelectYear(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_LEVELS_SELECT_YEAR);
        modelAndView.addObject("pageTitle", "Select Reporting Period");
        List<ReportingPeriod> REPORTING_PERIODS_LIST = reportingPeriodService.listAllReportingPeriods();
        modelAndView.addObject("reportingPeriodsList", REPORTING_PERIODS_LIST);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value="/view-individual-trends-select-year")
    public ModelAndView viewIndividualTrendsSelectEmployee(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_INDIVIDUAL_TRENDS_SELECT_EMPLOYEE);
        modelAndView.addObject("pageTitle", "Select Employee");
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value="/select-scorecards/{reportingPeriodId}")
    public ModelAndView viewPerformanceLevelsSelectScorecards(@PathVariable("reportingPeriodId") long reportingPeriodId, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_LEVELS_SELECT_SCORECARDS);
        modelAndView.addObject("pageTitle", "Select Scorecards");
        ReportingPeriod period = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
        List<Scorecard> scoresList = scorecardService.getScoresByPeriodId(period);
        modelAndView.addObject("scoresList", scoresList);
        preparePage(modelAndView, request);
        return modelAndView;
    }


    @RequestMapping(value = "/view-scores-select-year", method = RequestMethod.POST)
    public String goToViewScores(HttpServletRequest request, long reportingPeriodId) {

        return "redirect:/performance-review/view-scores/"+ reportingPeriodId;
    }

    @RequestMapping(value = "/view-performance-levels-select-year", method = RequestMethod.POST)
    public String goToSelectScorecards(HttpServletRequest request, long reportingPeriodId) {

        return "redirect:/performance-review/select-scorecards/"+ reportingPeriodId;
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

    @RequestMapping(value = "/view-performance-levels", method = RequestMethod.POST)
    public ModelAndView viewPerformanceLevels(@RequestParam("scorecards") List<Long> scorecardIds, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_LEVELS);
        modelAndView.addObject("pageTitle", "View performance Levels");
        List<Scorecard> scorecardList = new ArrayList<>();
        for(Long scorecardId : scorecardIds){
            scorecardList.add(scorecardService.getScorecardById(scorecardId));
        }

        List<String> names = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        for(Scorecard scorecard : scorecardList){
            try {
                scores.add(scorecard.getWeightedScore());
                names.add(scorecard.getOwner().getFullName());

            }catch (Exception ignored){

            }

        }

        modelAndView.addObject("names", names);
        modelAndView.addObject("scores", scores);

        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/view-individual-trends", method = RequestMethod.POST)
    public ModelAndView viewIndividualTrends(@RequestParam("employeeId") Long employeeId, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_INDIVIDUAL_TRENDS);
        modelAndView.addObject("pageTitle", "View Individual Trends");
        List<Scorecard> scorecardList = scorecardService.getScorecardsByOwner(accountService.getAccountById(employeeId));

        List<String> monthNames = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        for(Scorecard scorecard : scorecardList){

               for(ReportingDate date: scorecard.getReportingPeriod().getReportingDates()){
                   try {
                       Double score = scorecardService.getScoresByReportingDateAndScorecardId(date, scorecard);
                        scores.add(score);
                        monthNames.add(date.getEndDate());

                    }catch (Exception ignored){

                    }

               }

        }

        modelAndView.addObject("monthNames", monthNames);
        modelAndView.addObject("scores", scores);
        modelAndView.addObject("employee", accountService.getAccountById(employeeId));

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


    @RequestMapping("/download-report/{id}")
    public ModelAndView downloadReport(@PathVariable("id") long id, HttpServletRequest request, HttpServletResponse response) {
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
        List<Target> targetsList = targetService.getAllTargetsByScorecard(id);
        double totalWeightedScore = 0.0;
        for(Target target: targetsList){
            totalWeightedScore += target.getWeightedScore();
        }

        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("pips", pips);
        modelAndView.addObject("owner", owner);
        modelAndView.addObject("role", role);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
        modelAndView.addObject("scorecard", scoreCard);
        modelAndView.addObject("targetsList", targetsList);
        modelAndView.addObject("averageModeratedScore", averageModeratedScore);
        modelAndView.addObject("totalWeightedScore", totalWeightedScore);
        PortletUtils.addInfoMsg("Showing scores for the period: "+ startDate + " to "+ endDate, request);
        preparePage(modelAndView, request);
        return modelAndView;
    }


    public File generatePdf(Long id, HttpServletRequest request) throws Exception{
        Context context = getContext(id, request);
        String html = loadAndFillTemplate(context, request);
        return renderPdf(html);
    }


    private File renderPdf(String html) throws Exception {
        File file = File.createTempFile("stock-sales-report", ".pdf");
        OutputStream outputStream = new FileOutputStream(file);
//        ITextRenderer renderer = new ITextRenderer(20f * 4f / 3f, 20);
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html, new ClassPathResource(PDF_RESOURCES).getURL().toExternalForm());
        renderer.layout();
        renderer.createPDF(outputStream);
        outputStream.close();
        file.deleteOnExit();
        return file;
    }


    private Context getContext(Long id, HttpServletRequest request) {
        Context context = new Context();
        String username = PortletUtils.getUsername(request);
        Scorecard scoreCard = scorecardService.getScorecardById(id);
        List<StrategicObjective> strategicObjectivesList = strategicObjectiveService.listStrategicObjectivesByScorecard(id);
        ReportingPeriod reportingPeriod = scoreCard.getReportingPeriod();
        String startDate = reportingPeriod.getStartDate();
        String endDate = reportingPeriod.getEndDate();

        Account loggedUser = (Account) session.getAttribute("loggedUser");
        Account owner = scoreCard.getOwner();
        List<PerformanceImprovementPlan> pips = performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        double averageModeratedScore = goalService.getAverageModeratorScore(id);
        List<Target> targetsList = targetService.getAllTargetsByScorecard(id);
        double totalWeightedScore = 0.0;
        for(Target target: targetsList){
            totalWeightedScore += target.getWeightedScore();
        }

        context.setVariable("loggedUserId", loggedUserId);
        context.setVariable("pips", pips);
        context.setVariable("owner", owner);
        context.setVariable("role", role);
        context.setVariable("startDate", startDate);
        context.setVariable("endDate", endDate);
        context.setVariable("scorecard", scoreCard);
        context.setVariable("strategicObjectivesList", strategicObjectivesList);
        context.setVariable("averageModeratedScore", averageModeratedScore);
        context.setVariable("totalWeightedScore", totalWeightedScore);
        context.setVariable("username", username);
        return context;
    }


    private String loadAndFillTemplate(Context context, HttpServletRequest request) {
        return templateEngine.process(Pages.DOWNLOADABLE_REPORT, context);
    }

    @GetMapping("/download-pdf/{id}")
    public void downloadPdf(@PathVariable("id") Long id, HttpServletRequest request, HttpServletResponse response){
        try {
            Path file = Paths.get(generatePdf(id, request).getAbsolutePath());
            if(Files.exists(file)){
                response.setContentType("application/pdf");
//                response.addHeader("Content-Disposition", "attachment; filename"+ file.getFileName());
                response.addHeader("Content-Disposition", "inline; filename"+ file.getFileName());
                Files.copy(file, response.getOutputStream());
                response.getOutputStream().flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
