package hr.performancemanagement.controllers.assessment;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.service.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    private final TargetService targetService;
    @Autowired
    private final GoalService goalService;
    @Autowired
    private final OutcomeService outcomeService;
    @Autowired
    private final AccountService accountService;
    private static final String PDF_RESOURCES = "";
    @Autowired
    private SpringTemplateEngine templateEngine;
    @Autowired
    CommonService commonService;
    @Autowired
    OutputService outputService;
    @Autowired
    OverallScoreService overallScoreService;
    @Autowired
    ReportingDateService reportingDateService;
    @Autowired
    CommonService cs;
    @Autowired
    private PdfGeneratorService pdfGeneratorService;
    private List<Double> scores;

    public AssessmentController(TargetService targetService, GoalService goalService, OutcomeService outcomeService, AccountService accountService) {
        this.targetService = targetService;
        this.goalService = goalService;
        this.outcomeService = outcomeService;
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
        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();

        for(Scorecard scorecard: scoresList){
            OverallScore overallScore = overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, reportingDate);
            scorecard.setOverallScore(overallScore);
        }

        Account loggedUser = commonService.getLoggedUser();
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        modelAndView.addObject("scoresList", scoresList);
        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("role", role);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
//        PortletUtils.addInfoMsg("Showing scores for the period: "+ startDate + " to "+ endDate, request);
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
        List<Output> outputs = outputService.listAllOutputs(scoreCard);
        ReportingPeriod reportingPeriod = scoreCard.getReportingPeriod();
        String startDate = reportingPeriod.getStartDate();
        String endDate = reportingPeriod.getEndDate();

        Account loggedUser = commonService.getLoggedUser();
        Account owner = scoreCard.getOwner();
        List<PerformanceImprovementPlan> pips = performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        double averageModeratedScore = outcomeService.getAverageModeratorScore(id);
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
        modelAndView.addObject("outputs", outputs);
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
        List<Output> outputs = outputService.listAllOutputs(scoreCard);
        ReportingPeriod reportingPeriod = scoreCard.getReportingPeriod();
        String startDate = reportingPeriod.getStartDate();
        String endDate = reportingPeriod.getEndDate();

        Account loggedUser = cs.getLoggedUser();
        Account owner = scoreCard.getOwner();
        List<PerformanceImprovementPlan> pips = performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        double averageModeratedScore = outcomeService.getAverageModeratorScore(id);
        List<Target> targetsList = targetService.getAllTargetsByScorecard(scoreCard);
        double totalWeightedScore = 0.0;
//        for(Target target: targetsList){
//            totalWeightedScore += target.getWeightedScore();
//        }

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


//    public File generatePdf(Long id, HttpServletRequest request) throws Exception{
//        Context context = getContext(id, request);
//        String html = loadAndFillTemplate(context, request);
//        return renderPdf(html);
//    }


    private File renderPdf(String html) throws Exception {
        File file = File.createTempFile("performance-report", ".pdf");
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


//    private Context getContext(Long id, HttpServletRequest request) {
//        Context context = new Context();
//        String username = PortletUtils.getUsername(request);
//        Scorecard scorecard = scorecardService.getScorecardById(id);
//        ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
//        Account loggedUser = cs.getLoggedUser();
//        Account owner = scorecard.getOwner();
//        List<PerformanceImprovementPlan> pips = performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
//        long loggedUserId = loggedUser.getId();
//        String role = loggedUser.getRole();
//
//        for(ReportingDate reportingDate: reportingPeriod.getReportingDates()){
//            OverallScore overallScore = overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, reportingDate);
//            reportingDate.setOverallScore(overallScore);
//        }
//
//        context.setVariable("loggedUserId", loggedUserId);
//        context.setVariable("pips", pips);
//        context.setVariable("owner", owner);
//        context.setVariable("role", role);
//        context.setVariable("reportingPeriod", reportingPeriod);
//        context.setVariable("scorecard", scorecard);
//        context.setVariable("username", username);
//        return context;
//    }


    private String loadAndFillTemplate(Context context, HttpServletRequest request) {
        return templateEngine.process(Pages.DOWNLOADABLE_REPORT, context);
    }

    @GetMapping("/download-pdf/{id}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable("id") Long id, HttpServletRequest request, HttpServletResponse response) {
        try {
            Context context = new Context();
            String username = PortletUtils.getUsername(request);
            Scorecard scorecard = scorecardService.getScorecardById(id);
            ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
            Account loggedUser = cs.getLoggedUser();
            Account owner = scorecard.getOwner();
            List<PerformanceImprovementPlan> pips = performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
            long loggedUserId = loggedUser.getId();
            String role = loggedUser.getRole();

            for (ReportingDate reportingDate : reportingPeriod.getReportingDates()) {
                OverallScore overallScore = overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, reportingDate);
                reportingDate.setOverallScore(overallScore);
            }

            context.setVariable("loggedUserId", loggedUserId);
            context.setVariable("pips", pips);
            context.setVariable("owner", owner);
            context.setVariable("role", role);
            context.setVariable("reportingPeriod", reportingPeriod);
            context.setVariable("scorecard", scorecard);
            context.setVariable("username", username);

            String fileName = owner.getFullName().toUpperCase();
            String page = Pages.DOWNLOADABLE_REPORT;
            try {
                byte[] pdfBytes = pdfGeneratorService.generatePdfFromTemplate(page, context, false);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.builder("inline")
                        .filename(fileName.concat(fileName + "Report.pdf"))
                        .build());
                headers.setContentLength(pdfBytes.length);

                ByteArrayResource resource = new ByteArrayResource(pdfBytes);

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(pdfBytes.length)
                        .body(resource);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
