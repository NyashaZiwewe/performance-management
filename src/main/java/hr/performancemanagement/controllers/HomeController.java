package hr.performancemanagement.controllers;
import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.StrategicObjective;
import hr.performancemanagement.service.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.wrappers.ChangePasswordWrapper;
import hr.performancemanagement.utils.wrappers.LoginWrapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
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
    @Autowired
    AccountService accountService;
    @Autowired
    CommonService commonService;
    @Autowired
    Mailservice mailservice;

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
        String referrer = request.getHeader("referer");
        addErrorMessagesToLoginPage(modelAndView, request);
        PortletUtils.addMessagesToPage(modelAndView, request);
        modelAndView.addObject("localDate", LocalDate.now());
        return modelAndView;
    }

    @RequestMapping("/change-password/{reset}")
    public ModelAndView changePassword(@PathVariable("reset") String reset, HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView("changePassword");
        ChangePasswordWrapper wrapper = new ChangePasswordWrapper();
        try {
            Account employee = accountService.getAccountToReset(reset);
            wrapper.setEmail(employee.getEmail());
            wrapper.setResetPassword(reset);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Failed with error: "+ e.getMessage(), request);
        }
        modelAndView.addObject("changePasswordWrapper", wrapper);
        PortletUtils.addMessagesToPage(modelAndView, request);
        modelAndView.addObject("localDate", LocalDate.now());
        return modelAndView;
    }

    @RequestMapping("/change-password")
    public ModelAndView changePassword(HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView("changePassword");
        ChangePasswordWrapper wrapper = new ChangePasswordWrapper();
        try {
            Account account = commonService.getLoggedUser();
            String reset = RandomStringUtils.randomAlphanumeric(40);
            account.setResetPassword(reset);
            account = accountService.saveAccount(account);

            wrapper.setResetPassword(reset);
            wrapper.setEmail(account.getEmail());
            modelAndView.addObject("changePasswordWrapper", wrapper);
            PortletUtils.addMessagesToPage(modelAndView, request);
            return modelAndView;
        }catch (Exception e){
            PortletUtils.addErrorMsg("Failed with error: "+ e.getMessage(), request);
            PortletUtils.addMessagesToPage(modelAndView, request);
            modelAndView.addObject("localDate", LocalDate.now());
            return modelAndView;
        }


    }

    @RequestMapping(value = "/save-password", method = RequestMethod.POST)
    public String savePassword(ChangePasswordWrapper wrapper, HttpServletRequest request){

          try {
              Account employee = accountService.getAccountToReset(wrapper.getEmail(), wrapper.getResetPassword());
              String oldPassword = commonService.encryptPassword(wrapper.getOldPassword());
              if(employee != null){
                  if(!employee.getPassword().equalsIgnoreCase(oldPassword)){
                      PortletUtils.addErrorMsg("Incorrect Old password", request);
                      return "redirect:/change-password/"+ wrapper.getResetPassword();
                  }
                  else if(!wrapper.getNewPassword().equalsIgnoreCase(wrapper.getRepeatPassword())){
                      PortletUtils.addErrorMsg("Password mismatch. Check new & repeat Password ", request);
                      return "redirect:/change-password/"+ wrapper.getResetPassword();
                  }else {
                      employee.setPassword(commonService.encryptPassword(wrapper.getNewPassword()));
                      accountService.saveAccount(employee);
                      PortletUtils.addInfoMsg("Password successfully changed. You can login using your new Password ", request);
                      return "redirect:/login";
                  }
              }else {
                  PortletUtils.addErrorMsg("Password reset failed. Account not found ", request);
                  return "redirect:/change-password/"+ wrapper.getResetPassword();
              }
          }catch (Exception e){
              PortletUtils.addErrorMsg("Password reset failed. with message: "+ e.getMessage(), request);
              return "redirect:/change-password/"+ wrapper.getResetPassword();
          }
    }

    @RequestMapping("/reset-password")
    public ModelAndView resetPassword(HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView("resetPassword");
        PortletUtils.addMessagesToPage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/set-reset", method = RequestMethod.POST)
    public String setReset(@RequestParam("username") String username, HttpServletRequest request){
        try {
            Account account = accountService.findAccountByEmail(username);
            String reset = RandomStringUtils.randomAlphanumeric(40);
            if(account != null){
                account.setResetPassword(reset);
                accountService.saveAccount(account);
                String resetLink = commonService.getCurrentUrl(request).concat("/change-password/"+ reset);


                String recipient = username;
                String subject = "Password Reset,";
                String template = "Good day, \n\n"
                        + "Please note that we received a request to reset your account.  "
                        + "If you didn't initiate this, you can ignore this email, otherwise click the link below to set the new password\n\n"
                        + "Link :" + resetLink + "\n\n"
                        + "Best regards,\n"
                        + "The ZimTrade Team";
                try {
                    mailservice.sendEmail(recipient, subject, template);
                    PortletUtils.addInfoMsg("Password reset successfully initiated. Login to your email account "+ recipient +" and click the reset link to change your password", request);
                    return "redirect:/login";
                }catch (Exception e){
                    PortletUtils.addErrorMsg("Password reset successfully initiated. Email to "+ recipient + " failed to send. It's likely due to a network issue. Get in touch with the admin", request);
                    return "redirect:/login/";
                }
            }else {
                PortletUtils.addErrorMsg("Password reset failed. Account not found ", request);
                return "redirect:/reset-password";
            }
        }catch (Exception e){
            PortletUtils.addErrorMsg("Password reset failed. with message: "+ e.getMessage(), request);
            return "redirect:/reset-password";
        }
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
