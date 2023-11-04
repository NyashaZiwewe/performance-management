package hr.performancemanagement.controllers;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.StrategicObjective;
import hr.performancemanagement.service.ReportingPeriodService;
import hr.performancemanagement.service.ScorecardService;
import hr.performancemanagement.service.StrategicObjectiveService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.wrappers.LoginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static hr.performancemanagement.utils.PortletUtils.PortletUtils.ERROR_MSGS;

@Controller
@RequestMapping("/")
public class HomeController {

    @ModelAttribute("loginWrapper")
    public LoginWrapper getLoginWrapper(){
        return new LoginWrapper();
    }

    @Autowired
    StrategicObjectiveService strategicObjectiveService;
    @Autowired
    private final ReportingPeriodService reportingPeriodService;
    @Autowired
    private final ScorecardService scorecardService;

    public HomeController(ReportingPeriodService reportingPeriodService, ScorecardService scorecardService) {
        this.reportingPeriodService = reportingPeriodService;
        this.scorecardService = scorecardService;
    }

    @RequestMapping
    public ModelAndView goToHome(HttpServletRequest request ) throws ParseException {
        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        List<StrategicObjective> strategicObjectivesList = strategicObjectiveService.listAllStrategicObjectives(reportingPeriod.getId());
        List<Double> averageWeightsList = scorecardService.findAverageAllocatedWeightPerStrategicObjective();
        List<Double> averageScoresList = scorecardService.findAverageWeightedScorePerStrategicObjective();
        int pass = scorecardService.countPassedScorecardsByPeriodId(reportingPeriod);
        int fail = scorecardService.countFailedScorecardsByPeriodId(reportingPeriod);


        List<String> strategicObjectives = new ArrayList<>();
        for(StrategicObjective strategicObjective : strategicObjectivesList){
            strategicObjectives.add(strategicObjective.getName());
        }

        List<Double> averageWeights = new ArrayList<>();
        for(Double weight : averageWeightsList){
            averageWeights.add(weight);
        }

        List<Double> averageScores = new ArrayList<>();
        for(Double score : averageScoresList){
            averageScores.add(score);
        }

        String startDate = reportingPeriod.getStartDate();
        String endDate = reportingPeriod.getEndDate();

        List<String> monthsList = listMonthsWithinAReportingPeriod(startDate, endDate);
        List<String> monthNames = new ArrayList<>();
        for(String month : monthsList){
            monthNames.add(month);
        }

        ModelAndView modelAndView =  new ModelAndView("index");
        modelAndView.addObject("pageDomain", "Home");
        modelAndView.addObject("pageName", "Home");
        modelAndView.addObject("pageTitle", "Home");
        modelAndView.addObject("strategicObjectives", strategicObjectives);
        modelAndView.addObject("weightList", averageWeights);
        modelAndView.addObject("scoresList", averageScores);
        modelAndView.addObject("pass", pass);
        modelAndView.addObject("fail", fail);
        modelAndView.addObject("monthNames", monthNames);

        PortletUtils.addMessagesToPage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/login")
    public ModelAndView goToLoginPage(HttpServletRequest request ){
        ModelAndView modelAndView =  new ModelAndView("login");
        HttpSession session = request.getSession(false);
        if (session != null) {
            AuthenticationException ex = (AuthenticationException) session
                    .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            if (ex != null) {
                PortletUtils.addErrorMsg(ex.getMessage(), request);
            }
        }
        addErrorMessagesToLoginPage(modelAndView, request);
        modelAndView.addObject("localDate", LocalDate.now());
        return modelAndView;
    }

    private void addErrorMessagesToLoginPage(ModelAndView modelAndView, HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<String> errorMsgs = (List<String>) session.getAttribute(ERROR_MSGS);
        if (errorMsgs != null) {
            modelAndView.addObject(ERROR_MSGS, errorMsgs);
        }
        session.removeAttribute(ERROR_MSGS);
    }

    public List<String> listMonthsWithinAReportingPeriod(String sDate, String eDate) throws ParseException {

        Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(sDate);
        Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse(eDate);

        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getMonths();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("CAT"));

        long monthsBetween = ChronoUnit.MONTHS.between(
                YearMonth.from(LocalDate.parse(sDate)),
                YearMonth.from(LocalDate.parse(eDate))
        );
        System.out.println(monthsBetween);
        List<String> monthsList = new ArrayList<>();
        for(int x = 0; x <= monthsBetween; x++){

            cal.setTime(startDate);
            cal.add(Calendar.MONTH, x);
            int month = cal.get(Calendar.MONTH);
            System.out.println("Month is "+ months[month]);
            monthsList.add(months[month]);
        }
      return monthsList;
    }

}
