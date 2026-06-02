package hr.performancemanagement.controllers.assessment;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.OutcomeService;
import hr.performancemanagement.service.OutputService;
import hr.performancemanagement.service.OverallCommentService;
import hr.performancemanagement.service.OverallScoreService;
import hr.performancemanagement.service.PdfGeneratorService;
import hr.performancemanagement.service.api.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/performance-review")
public class AssessmentController {

    private static final Logger log = LoggerFactory.getLogger(AssessmentController.class);

    @Autowired
    ReportingPeriodService reportingPeriodService;
    @Autowired
    ScorecardService scorecardService;

    @Autowired
    PerformanceImprovementPlanService performanceImprovementPlanService;
    @Autowired
    ActionPlanService actionPlanService;
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
    @Autowired
    OverallCommentService overallCommentService;
    @Autowired
    ScoreRepository scoreRepository;
    private List<Double> scores;

    public AssessmentController(TargetService targetService, GoalService goalService, OutcomeService outcomeService, AccountService accountService) {
        this.targetService = targetService;
        this.goalService = goalService;
        this.outcomeService = outcomeService;
        this.accountService = accountService;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {
        List<Account> ACCOUNTS_LIST = accountService.listAllAccounts();
        ReportingDate activeReportingDate = reportingDateService.getActiveReportingDate();
        boolean captureWindowOpen = reportingDateService.isReportingDateOpen(activeReportingDate);
        modelAndView.addObject("pageDomain", "Performance Review");
        modelAndView.addObject("pageName", "Assessments");
        modelAndView.addObject("profile", "moderator");
        modelAndView.addObject("accountsList", ACCOUNTS_LIST);
        modelAndView.addObject("captureWindowOpen", captureWindowOpen);
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
        List<Scorecard> scorecards = scorecardService.getScorecardsByReportingPeriodId(reportingPeriod);
        List<ReportingDate> reportingDates = reportingPeriod.getReportingDates();
        Map<Long, Map<Long, OverallScore>> overallScoresByDate =
                overallScoreService.getOverallScoresByScorecardsAndReportingDates(scorecards, reportingDates);
        Map<Long, Integer> pipCountsByScorecardId = new HashMap<>();
        Map<Long, Integer> actionPlanCountsByScorecardId = new HashMap<>();

        Account loggedUser = commonService.getLoggedUser();
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getOwner() == null) {
                continue;
            }
            Account owner = scorecard.getOwner();
            pipCountsByScorecardId.put(scorecard.getId(),
                    performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod).size());
            actionPlanCountsByScorecardId.put(scorecard.getId(),
                    actionPlanService.listActionPlansByManagerAndReportingPeriod(owner, reportingPeriod).size());
        }

        for(ReportingDate reportingDate : reportingDates) {
            List<OverallScore> overallScores = new ArrayList<>();
            Map<Long, OverallScore> scoreByScorecard = reportingDate == null ? null : overallScoresByDate.get(reportingDate.getId());

            for(Scorecard scorecard : scorecards) {
                OverallScore overallScore = resolveOverallScore(scoreByScorecard, scorecard, reportingDate);
                overallScores.add(overallScore);
            }
            reportingDate.setOverallScores(overallScores);

        }

        modelAndView.addObject("reportingDates", reportingDates);
        modelAndView.addObject("scoresList", scorecards);
        modelAndView.addObject("pipCountsByScorecardId", pipCountsByScorecardId);
        modelAndView.addObject("actionPlanCountsByScorecardId", actionPlanCountsByScorecardId);
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
        List<Scorecard> scorecardList = scorecardService.getScorecardsByIds(scorecardIds);

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
        List<ReportingDate> reportingDates = new ArrayList<>();
        for (Scorecard scorecard : scorecardList) {
            if (scorecard != null
                    && scorecard.getReportingPeriod() != null
                    && scorecard.getReportingPeriod().getReportingDates() != null) {
                reportingDates.addAll(scorecard.getReportingPeriod().getReportingDates());
            }
        }
        Map<Long, Map<Long, Double>> scoresByDate =
                scorecardService.getScoresByReportingDatesAndScorecardIds(reportingDates, scorecardList);
        for(Scorecard scorecard : scorecardList){
            List<ReportingDate> dates = scorecard.getReportingPeriod().getReportingDates();

               for(ReportingDate date: dates){
                   try {
                       Double score = 0.0;
                       if (date != null) {
                           Map<Long, Double> scoreByScorecard = scoresByDate.get(date.getId());
                           if (scoreByScorecard != null && scoreByScorecard.containsKey(scorecard.getId())) {
                               score = scoreByScorecard.get(scorecard.getId());
                           }
                       }
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
        List<Target> targetsList = targetService.getAllTargetsByScorecard(scoreCard.getId());
        ReportingPeriod reportingPeriod = scoreCard.getReportingPeriod();
        String startDate = reportingPeriod.getStartDate();
        String endDate = reportingPeriod.getEndDate();

        Account loggedUser = commonService.getLoggedUser();
        Account owner = scoreCard.getOwner();
        List<PerformanceImprovementPlan> pips = performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
        List<ActionPlan> actionPlans = actionPlanService.listActionPlansByManagerAndReportingPeriod(owner, reportingPeriod);
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        double averageModeratedScore = outcomeService.getAverageModeratorScore(id);
        double weightedScore;
        try {
            weightedScore = (averageModeratedScore / 5 ) * 100;
        }catch (Exception e){
            weightedScore = 0;
        }
        double totalWeightedScore = 0.0;
        for (Target target : targetsList) {
            if (target != null && target.getWeightedScore() != null) {
                totalWeightedScore += target.getWeightedScore();
            }
        }

        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("pips", pips);
        modelAndView.addObject("actionPlans", actionPlans);
        modelAndView.addObject("owner", owner);
        modelAndView.addObject("role", role);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
        modelAndView.addObject("scorecard", scoreCard);
        modelAndView.addObject("outputs", outputs);
        modelAndView.addObject("targetsList", targetsList);
        modelAndView.addObject("averageModeratedScore", averageModeratedScore);
        modelAndView.addObject("weightedScore", weightedScore);
        modelAndView.addObject("totalWeightedScore", totalWeightedScore);
        populatePerformanceInsights(modelAndView, scoreCard, reportingPeriod, targetsList, pips, actionPlans);
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
        List<ActionPlan> actionPlans = actionPlanService.listActionPlansByManagerAndReportingPeriod(owner, reportingPeriod);
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        double averageModeratedScore = outcomeService.getAverageModeratorScore(id);
        List<Target> targetsList = targetService.getAllTargetsByScorecard(scoreCard.getId());
        double totalWeightedScore = 0.0;
        for(Target target: targetsList){
            if (target != null && target.getWeightedScore() != null) {
                totalWeightedScore += target.getWeightedScore();
            }
        }

        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("pips", pips);
        modelAndView.addObject("actionPlans", actionPlans);
        modelAndView.addObject("owner", owner);
        modelAndView.addObject("role", role);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
        modelAndView.addObject("scorecard", scoreCard);
        modelAndView.addObject("targetsList", targetsList);
        modelAndView.addObject("averageModeratedScore", averageModeratedScore);
        modelAndView.addObject("totalWeightedScore", totalWeightedScore);
        populatePerformanceInsights(modelAndView, scoreCard, reportingPeriod, targetsList, pips, actionPlans);
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
            List<ActionPlan> actionPlans = actionPlanService.listActionPlansByManagerAndReportingPeriod(owner, reportingPeriod);
            List<Target> targetsList = targetService.getAllTargetsByScorecard(scorecard.getId());
            List<OverallComment> overallComments = overallCommentService.getOverallCommentsByScorecard(scorecard);
            long loggedUserId = loggedUser.getId();
            String role = loggedUser.getRole();
            List<ReportingDate> reportingDates = reportingPeriod == null || reportingPeriod.getReportingDates() == null
                    ? Collections.emptyList()
                    : reportingPeriod.getReportingDates();
            Map<Long, Map<Long, OverallScore>> overallScoresByDate =
                    overallScoreService.getOverallScoresByScorecardsAndReportingDates(Collections.singletonList(scorecard), reportingDates);
            ReportingDate finalReportingDate = resolveInsightReportingDate(reportingPeriod);

            for (ReportingDate reportingDate : reportingDates) {
                Map<Long, OverallScore> scoreByScorecard = reportingDate == null ? null : overallScoresByDate.get(reportingDate.getId());
                OverallScore overallScore = resolveOverallScore(scoreByScorecard, scorecard, reportingDate);
                reportingDate.setOverallScore(overallScore);
            }
            OverallScore finalOverallScore = finalReportingDate == null
                    ? null
                    : overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, finalReportingDate);

            context.setVariable("loggedUserId", loggedUserId);
            context.setVariable("pips", pips);
            context.setVariable("actionPlans", actionPlans);
            context.setVariable("overallComments", overallComments);
            context.setVariable("owner", owner);
            context.setVariable("role", role);
            context.setVariable("reportingPeriod", reportingPeriod);
            context.setVariable("scorecard", scorecard);
            context.setVariable("username", username);
            context.setVariable("finalReportingDate", finalReportingDate);
            context.setVariable("finalOverallScore", finalOverallScore);
            ReportInsight reportInsight = buildReportInsight(scorecard, reportingPeriod, targetsList, pips, actionPlans);
            context.setVariable("reportInsight", reportInsight);
            context.setVariable("averageEmployeeScore", reportInsight.averageEmployeeScore);
            context.setVariable("averageManagerScore", reportInsight.averageManagerScore);
            context.setVariable("averageAgreedScore", reportInsight.averageAgreedScore);
            context.setVariable("averageModeratedScore", reportInsight.averageModeratedScore);
            context.setVariable("alignmentGapEmployeeManager", reportInsight.alignmentGapEmployeeManager);
            context.setVariable("alignmentGapManagerAgreed", reportInsight.alignmentGapManagerAgreed);
            context.setVariable("alignmentGapAgreedModerated", reportInsight.alignmentGapAgreedModerated);
            context.setVariable("totalTargets", reportInsight.totalTargets);
            context.setVariable("targetsWithScore", reportInsight.targetsWithScore);
            context.setVariable("targetsWithoutScore", reportInsight.targetsWithoutScore);
            context.setVariable("scoreCoveragePercent", reportInsight.scoreCoveragePercent);
            context.setVariable("riskRedTargets", reportInsight.riskRedTargets);
            context.setVariable("riskAmberTargets", reportInsight.riskAmberTargets);
            context.setVariable("riskGreenTargets", reportInsight.riskGreenTargets);
            context.setVariable("targetsWithEvidence", reportInsight.targetsWithEvidence);
            context.setVariable("targetsWithoutEvidence", reportInsight.targetsWithoutEvidence);
            context.setVariable("evidenceCoveragePercent", reportInsight.evidenceCoveragePercent);
            context.setVariable("insightReportingDateLabel", reportInsight.insightReportingDateLabel);
            context.setVariable("pipOpenCount", reportInsight.pipOpenCount);
            context.setVariable("pipInProgressCount", reportInsight.pipInProgressCount);
            context.setVariable("pipClosedCount", reportInsight.pipClosedCount);
            context.setVariable("actionOpenCount", reportInsight.actionOpenCount);
            context.setVariable("actionInProgressCount", reportInsight.actionInProgressCount);
            context.setVariable("actionClosedCount", reportInsight.actionClosedCount);

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
                log.error("Error generating PDF for assessment", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }catch (Exception e){
            log.error("Error in assessment download endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void populatePerformanceInsights(ModelAndView modelAndView,
                                             Scorecard scorecard,
                                             ReportingPeriod reportingPeriod,
                                             List<Target> targetsList,
                                             List<PerformanceImprovementPlan> pips,
                                             List<ActionPlan> actionPlans) {
        ReportInsight reportInsight = buildReportInsight(scorecard, reportingPeriod, targetsList, pips, actionPlans);
        modelAndView.addObject("reportInsight", reportInsight);
        modelAndView.addObject("averageEmployeeScore", reportInsight.averageEmployeeScore);
        modelAndView.addObject("averageManagerScore", reportInsight.averageManagerScore);
        modelAndView.addObject("averageAgreedScore", reportInsight.averageAgreedScore);
        modelAndView.addObject("averageModeratedScore", reportInsight.averageModeratedScore);
        modelAndView.addObject("alignmentGapEmployeeManager", reportInsight.alignmentGapEmployeeManager);
        modelAndView.addObject("alignmentGapManagerAgreed", reportInsight.alignmentGapManagerAgreed);
        modelAndView.addObject("alignmentGapAgreedModerated", reportInsight.alignmentGapAgreedModerated);
        modelAndView.addObject("totalTargets", reportInsight.totalTargets);
        modelAndView.addObject("targetsWithScore", reportInsight.targetsWithScore);
        modelAndView.addObject("targetsWithoutScore", reportInsight.targetsWithoutScore);
        modelAndView.addObject("scoreCoveragePercent", reportInsight.scoreCoveragePercent);
        modelAndView.addObject("riskRedTargets", reportInsight.riskRedTargets);
        modelAndView.addObject("riskAmberTargets", reportInsight.riskAmberTargets);
        modelAndView.addObject("riskGreenTargets", reportInsight.riskGreenTargets);
        modelAndView.addObject("targetsWithEvidence", reportInsight.targetsWithEvidence);
        modelAndView.addObject("targetsWithoutEvidence", reportInsight.targetsWithoutEvidence);
        modelAndView.addObject("evidenceCoveragePercent", reportInsight.evidenceCoveragePercent);
        modelAndView.addObject("insightReportingDateLabel", reportInsight.insightReportingDateLabel);
        modelAndView.addObject("pipOpenCount", reportInsight.pipOpenCount);
        modelAndView.addObject("pipInProgressCount", reportInsight.pipInProgressCount);
        modelAndView.addObject("pipClosedCount", reportInsight.pipClosedCount);
        modelAndView.addObject("actionOpenCount", reportInsight.actionOpenCount);
        modelAndView.addObject("actionInProgressCount", reportInsight.actionInProgressCount);
        modelAndView.addObject("actionClosedCount", reportInsight.actionClosedCount);
    }

    private ReportInsight buildReportInsight(Scorecard scorecard,
                                             ReportingPeriod reportingPeriod,
                                             List<Target> targetsList,
                                             List<PerformanceImprovementPlan> pips,
                                             List<ActionPlan> actionPlans) {
        ReportInsight insight = new ReportInsight();
        if (scorecard == null) {
            return insight;
        }

        List<Target> safeTargets = targetsList == null ? Collections.emptyList() : targetsList;
        ReportingDate insightReportingDate = resolveInsightReportingDate(reportingPeriod);
        insight.insightReportingDateLabel = insightReportingDate == null || insightReportingDate.getEndDate() == null
                ? "Latest available context"
                : insightReportingDate.getEndDate();

        applyReportInsightAverages(insight, scorecard, safeTargets, insightReportingDate);
        insight.alignmentGapEmployeeManager = roundTwoDecimals(Math.abs(insight.averageEmployeeScore - insight.averageManagerScore));
        insight.alignmentGapManagerAgreed = roundTwoDecimals(Math.abs(insight.averageManagerScore - insight.averageAgreedScore));
        insight.alignmentGapAgreedModerated = roundTwoDecimals(Math.abs(insight.averageAgreedScore - insight.averageModeratedScore));

        insight.totalTargets = safeTargets.size();

        for (Target target : safeTargets) {
            if (target == null) {
                continue;
            }
            Score score = insightReportingDate == null ? null : resolveInsightScore(target, insightReportingDate);
            double scoreValue = insightReportingDate == null
                    ? resolveTargetInsightScore(target)
                    : resolveInsightScoreValue(score);
            if (scoreValue > 0.0) {
                insight.targetsWithScore++;
                if (scoreValue < 2.5) {
                    insight.riskRedTargets++;
                } else if (scoreValue < 3.5) {
                    insight.riskAmberTargets++;
                } else {
                    insight.riskGreenTargets++;
                }
            }

            boolean hasEvidence = false;
            if (insightReportingDate != null) {
                hasEvidence = score != null && (hasText(score.getEvidence()) || hasText(score.getAttachmentName()));
            } else {
                hasEvidence = hasText(target.getCurrentEvidence()) || hasText(target.getCurrentAttachmentName());
            }
            if (hasEvidence) {
                insight.targetsWithEvidence++;
            }
        }

        insight.targetsWithoutScore = Math.max(0, insight.totalTargets - insight.targetsWithScore);
        insight.targetsWithoutEvidence = Math.max(0, insight.totalTargets - insight.targetsWithEvidence);
        insight.scoreCoveragePercent = percentage(insight.targetsWithScore, insight.totalTargets);
        insight.evidenceCoveragePercent = percentage(insight.targetsWithEvidence, insight.totalTargets);

        List<PerformanceImprovementPlan> safePips = pips == null ? Collections.emptyList() : pips;
        for (PerformanceImprovementPlan pip : safePips) {
            if (pip == null) {
                continue;
            }
            int statusCategory = classifyProgressStatus(pip.getStatus(), pip.getProgress());
            if (statusCategory == 2) {
                insight.pipClosedCount++;
            } else if (statusCategory == 1) {
                insight.pipInProgressCount++;
            } else {
                insight.pipOpenCount++;
            }
        }

        List<ActionPlan> safeActionPlans = actionPlans == null ? Collections.emptyList() : actionPlans;
        for (ActionPlan actionPlan : safeActionPlans) {
            if (actionPlan == null) {
                continue;
            }
            int statusCategory = classifyProgressStatus(actionPlan.getStatus(), actionPlan.getProgress());
            if (statusCategory == 2) {
                insight.actionClosedCount++;
            } else if (statusCategory == 1) {
                insight.actionInProgressCount++;
            } else {
                insight.actionOpenCount++;
            }
        }
        return insight;
    }

    private void applyReportInsightAverages(ReportInsight insight,
                                            Scorecard scorecard,
                                            List<Target> targets,
                                            ReportingDate reportingDate) {
        if (reportingDate == null) {
            insight.averageEmployeeScore = safeScore(goalService.getAverageEmployeeScore(scorecard.getId()));
            insight.averageManagerScore = safeScore(goalService.getAverageManagerScore(scorecard.getId()));
            insight.averageAgreedScore = safeScore(goalService.getAverageAgreedScore(scorecard.getId()));
            insight.averageModeratedScore = safeScore(goalService.getAverageModeratorScore(scorecard.getId()));
            return;
        }

        OverallScore overallScore = overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, reportingDate);
        if (hasAnyOverallScore(overallScore)) {
            insight.averageEmployeeScore = safeScore(orZero(overallScore.getEmployeeOverall()));
            insight.averageManagerScore = safeScore(orZero(overallScore.getManagerOverall()));
            insight.averageAgreedScore = safeScore(orZero(overallScore.getAgreedOverall()));
            insight.averageModeratedScore = safeScore(orZero(overallScore.getModeratedOverall()));
            return;
        }

        Map<Long, Score> finalScoresById = new HashMap<Long, Score>();
        for (Target target : targets) {
            Score score = resolveInsightScore(target, reportingDate);
            if (score != null && score.getId() > 0) {
                finalScoresById.put(score.getId(), score);
            }
        }

        double employeeTotal = 0.0;
        int employeeCount = 0;
        double managerTotal = 0.0;
        int managerCount = 0;
        double agreedTotal = 0.0;
        int agreedCount = 0;
        double moderatedTotal = 0.0;
        int moderatedCount = 0;

        for (Score score : finalScoresById.values()) {
            if (score.getEmployeeScore() > 0.0) {
                employeeTotal += score.getEmployeeScore();
                employeeCount++;
            }
            if (score.getManagerScore() > 0.0) {
                managerTotal += score.getManagerScore();
                managerCount++;
            }
            if (score.getAgreedScore() > 0.0) {
                agreedTotal += score.getAgreedScore();
                agreedCount++;
            }
            if (score.getModeratedScore() > 0.0) {
                moderatedTotal += score.getModeratedScore();
                moderatedCount++;
            }
        }

        insight.averageEmployeeScore = averageScore(employeeTotal, employeeCount);
        insight.averageManagerScore = averageScore(managerTotal, managerCount);
        insight.averageAgreedScore = averageScore(agreedTotal, agreedCount);
        insight.averageModeratedScore = averageScore(moderatedTotal, moderatedCount);
    }

    private boolean hasAnyOverallScore(OverallScore overallScore) {
        return overallScore != null
                && (positive(overallScore.getEmployeeOverall())
                || positive(overallScore.getManagerOverall())
                || positive(overallScore.getAgreedOverall())
                || positive(overallScore.getModeratedOverall()));
    }

    private boolean positive(Double value) {
        return value != null && value > 0.0;
    }

    private double orZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private double averageScore(double total, int count) {
        if (count <= 0) {
            return 0.0;
        }
        return roundTwoDecimals(total / count);
    }

    private Score resolveInsightScore(Target target, ReportingDate reportingDate) {
        if (target == null || reportingDate == null) {
            return null;
        }
        List<Score> byTarget = scoreRepository.findScoresByTargetAndReportingDateOrderByIdDesc(target, reportingDate);
        if (byTarget != null && !byTarget.isEmpty()) {
            return byTarget.get(0);
        }
        if (target.getOutput() == null) {
            return null;
        }
        List<Score> byOutput = scoreRepository.findScoresByOutputAndReportingDateOrderByIdDesc(target.getOutput(), reportingDate);
        if (byOutput == null || byOutput.isEmpty()) {
            return null;
        }
        for (Score score : byOutput) {
            if (score != null && score.getTarget() != null && score.getTarget().getId() == target.getId()) {
                return score;
            }
        }
        for (Score score : byOutput) {
            if (score != null && score.getTarget() == null) {
                return score;
            }
        }
        return byOutput.get(0);
    }

    private ReportingDate resolveInsightReportingDate(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null || reportingPeriod.getReportingDates() == null || reportingPeriod.getReportingDates().isEmpty()) {
            return null;
        }
        ReportingDate selected = null;
        LocalDate selectedDate = null;
        for (ReportingDate reportingDate : reportingPeriod.getReportingDates()) {
            if (reportingDate == null) {
                continue;
            }
            LocalDate candidateDate = parseLocalDate(reportingDate.getEndDate());
            if (candidateDate == null) {
                if (selected == null) {
                    selected = reportingDate;
                }
                continue;
            }
            if (selectedDate == null || candidateDate.isAfter(selectedDate)) {
                selected = reportingDate;
                selectedDate = candidateDate;
            }
        }
        return selected;
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private double resolveTargetInsightScore(Target target) {
        if (target == null) {
            return 0.0;
        }
        Double[] candidates = new Double[]{
                target.getCurrentModeratedScore(),
                target.getModeratedScore(),
                target.getCurrentAgreedScore(),
                target.getAgreedScore(),
                target.getCurrentManagerScore(),
                target.getManagerScore(),
                target.getCurrentEmployeeScore(),
                target.getEmployeeScore()
        };
        for (Double candidate : candidates) {
            if (candidate != null && candidate > 0.0) {
                return candidate;
            }
        }
        return 0.0;
    }

    private double resolveInsightScoreValue(Score score) {
        if (score == null) {
            return 0.0;
        }
        double[] candidates = new double[]{
                score.getModeratedScore(),
                score.getAgreedScore(),
                score.getManagerScore(),
                score.getEmployeeScore(),
                score.getActual()
        };
        for (double candidate : candidates) {
            if (candidate > 0.0) {
                return candidate;
            }
        }
        return 0.0;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private double safeScore(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return roundTwoDecimals(value);
    }

    private double percentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return roundTwoDecimals((numerator * 100.0) / denominator);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private int classifyProgressStatus(String status, double progress) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (progress >= 100.0
                || "COMPLETED".equals(normalized)
                || "DONE".equals(normalized)
                || "CLOSED".equals(normalized)
                || "RESOLVED".equals(normalized)) {
            return 2;
        }
        if (progress > 0.0
                || "IN_PROGRESS".equals(normalized)
                || "INPROGRESS".equals(normalized)
                || "ACTIVE".equals(normalized)
                || "ONGOING".equals(normalized)) {
            return 1;
        }
        return 0;
    }

    private static class ReportInsight {
        private double averageEmployeeScore = 0.0;
        private double averageManagerScore = 0.0;
        private double averageAgreedScore = 0.0;
        private double averageModeratedScore = 0.0;
        private double alignmentGapEmployeeManager = 0.0;
        private double alignmentGapManagerAgreed = 0.0;
        private double alignmentGapAgreedModerated = 0.0;
        private int totalTargets = 0;
        private int targetsWithScore = 0;
        private int targetsWithoutScore = 0;
        private double scoreCoveragePercent = 0.0;
        private int riskRedTargets = 0;
        private int riskAmberTargets = 0;
        private int riskGreenTargets = 0;
        private int targetsWithEvidence = 0;
        private int targetsWithoutEvidence = 0;
        private double evidenceCoveragePercent = 0.0;
        private String insightReportingDateLabel = "Latest available context";
        private int pipOpenCount = 0;
        private int pipInProgressCount = 0;
        private int pipClosedCount = 0;
        private int actionOpenCount = 0;
        private int actionInProgressCount = 0;
        private int actionClosedCount = 0;
    }

    private OverallScore resolveOverallScore(Map<Long, OverallScore> scoreByScorecard, Scorecard scorecard, ReportingDate reportingDate) {
        if (scorecard == null) {
            return null;
        }
        if (scoreByScorecard != null) {
            OverallScore overallScore = scoreByScorecard.get(scorecard.getId());
            if (overallScore != null) {
                return overallScore;
            }
        }
        OverallScore defaultScore = new OverallScore();
        defaultScore.setScorecard(scorecard);
        defaultScore.setReportingDate(reportingDate);
        defaultScore.setEmployeeOverall(0.0);
        defaultScore.setManagerOverall(0.0);
        defaultScore.setAgreedOverall(0.0);
        defaultScore.setModeratedOverall(0.0);
        return defaultScore;
    }

}
