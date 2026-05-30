package hr.performancemanagement.controllers.reportingperiod;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.StrategicObjective;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.NotificationService;
import hr.performancemanagement.service.api.ReportingDateService;
import hr.performancemanagement.service.api.ReportingPeriodService;
import hr.performancemanagement.service.api.StrategicObjectiveService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Client;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/reporting-periods")
public class ReportingPeriodController {

    private final ReportingPeriodService reportingPeriodService;
    private final StrategicObjectiveService strategicObjectiveService;
    private final ReportingDateService reportingDateService;
    private final NotificationService notificationService;
    private final CommonService commonService;

    public ReportingPeriodController(ReportingPeriodService reportingPeriodService,
                                     StrategicObjectiveService strategicObjectiveService,
                                     ReportingDateService reportingDateService,
                                     NotificationService notificationService,
                                     CommonService commonService) {
        this.reportingPeriodService = reportingPeriodService;
        this.strategicObjectiveService = strategicObjectiveService;
        this.reportingDateService = reportingDateService;
        this.notificationService = notificationService;
        this.commonService = commonService;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {

        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Reporting Period");
        PortletUtils.addMessagesToPage(modelAndView, request);
    }


    @RequestMapping
    public ModelAndView viewReportingPeriods(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_REPORTING_PERIODS);
        modelAndView.addObject("pageTitle", "View ReportingPeriods");
        List<ReportingPeriod> reportingPeriodsList = reportingPeriodService.listAllReportingPeriods();
        Map<Long, Long> strategicObjectiveCounts = new HashMap<Long, Long>();
        Map<Long, Long> reportingDateCounts = new HashMap<Long, Long>();
        for (ReportingPeriod period : reportingPeriodsList) {
            strategicObjectiveCounts.put(period.getId(), strategicObjectiveService.countStrategicObjectives(period.getId()));
            reportingDateCounts.put(period.getId(), reportingDateService.countReportingDates(period.getId()));
        }
        modelAndView.addObject("reportingPeriodsList", reportingPeriodsList);
        modelAndView.addObject("strategicObjectiveCounts", strategicObjectiveCounts);
        modelAndView.addObject("reportingDateCounts", reportingDateCounts);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/add-reporting-period")
    public ModelAndView addReportingPeriod(HttpServletRequest request) {

        ModelAndView modelAndView = new ModelAndView(Pages.ADD_REPORTING_PERIOD);
        modelAndView.addObject("pageTitle", "New Reporting Period");
        modelAndView.addObject("reportingPeriod", new ReportingPeriod());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/add-reporting-period", method = RequestMethod.POST)
    public String addReportingPeriod(HttpServletRequest request, ReportingPeriod newReportingPeriod) {

        newReportingPeriod.setClientId(Client.CLIENT_ID);
        newReportingPeriod.setStatus("ACTIVE");
        reportingPeriodService.saveReportingPeriod(newReportingPeriod);
        PortletUtils.addInfoMsg("A new reporting period was successfully created and activated", request);
        return "redirect:/reporting-periods";
    }


    @RequestMapping("/edit-reporting-period/{id}")
    public ModelAndView editReportingPeriod(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.EDIT_REPORTING_PERIOD);
        modelAndView.addObject("pageTitle", "Update Reporting Period");
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(id);
        modelAndView.addObject("reportingPeriod", reportingPeriod);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-reporting-period", method = RequestMethod.POST)
    public String saveReportingPeriod(HttpServletRequest request, ReportingPeriod reportingPeriod) {

        reportingPeriod.setClientId(Client.CLIENT_ID);
        reportingPeriodService.saveReportingPeriod(reportingPeriod);
        PortletUtils.addInfoMsg("Reporting period successfully updated.", request);
        return "redirect:/reporting-periods";
    }

    @RequestMapping("/strategic-goals/{id}")
    public ModelAndView viewStrategicObjectives(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_STRATEGIC_OBJECTIVES);
        modelAndView.addObject("pageTitle", "View Strategic Objectives");
        List<StrategicObjective> strategicObjectivesList = strategicObjectiveService.listAllStrategicObjectives(id);
        modelAndView.addObject("goalsList", strategicObjectivesList);
        modelAndView.addObject("reportingPeriod", reportingPeriodService.getReportingPeriodById(id));
        modelAndView.addObject("reportingPeriodsList", reportingPeriodService.listAllReportingPeriods());
        modelAndView.addObject("strategicObjective", new StrategicObjective());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/reporting-dates/{id}")
    public ModelAndView viewReportingDates(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_REPORTING_DATES);
        modelAndView.addObject("pageTitle", "View Reporting Dates");
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(id);
        List<ReportingDate> reportingDateList = reportingDateService.listAllReportingDates(reportingPeriod);
        modelAndView.addObject("reportingDateList", reportingDateList);
        modelAndView.addObject("reportingPeriod", reportingPeriod);
        modelAndView.addObject("reportingPeriodsList", reportingPeriodService.listAllReportingPeriods());
        modelAndView.addObject("reportingDate", new ReportingDate());

        preparePage(modelAndView, request);
        return modelAndView;
    }
    @RequestMapping(value = "/add-strategic-objective", method = RequestMethod.POST)
    public String addStrategicObjective(HttpServletRequest request, StrategicObjective newStrategicObjective) {

        strategicObjectiveService.addStrategicObjective(newStrategicObjective);
        PortletUtils.addInfoMsg("Strategic goal successfully added.", request);
        return "redirect:/reporting-periods/strategic-goals/" + newStrategicObjective.getReportingPeriod().getId();
    }

    @RequestMapping(value = "/save-strategic-objective", method = RequestMethod.POST)
    public String saveStrategicObjective( HttpServletRequest request, StrategicObjective strategicObjective) {

        strategicObjectiveService.saveStrategicObjective(strategicObjective);
        PortletUtils.addInfoMsg("Strategic goal successfully updated.", request);
        return "redirect:/reporting-periods/strategic-goals/" + strategicObjective.getReportingPeriod().getId();
    }

    @RequestMapping(value = "/add-reporting-date", method = RequestMethod.POST)
    public String addReportingDate(HttpServletRequest request, ReportingDate newReportingDate) {

        reportingDateService.saveReportingDate(newReportingDate);
        sendReportingDateActivationNotice(request, newReportingDate, "created");
        PortletUtils.addInfoMsg("Reporting Date successfully added.", request);
        return "redirect:/reporting-periods/reporting-dates/" + newReportingDate.getReportingPeriod().getId();
    }

    @RequestMapping(value = "/save-reporting-date", method = RequestMethod.POST)
    public String saveReportingDate( HttpServletRequest request, ReportingDate newReportingDate) {

        reportingDateService.saveReportingDate(newReportingDate);
        sendReportingDateActivationNotice(request, newReportingDate, "updated");
        PortletUtils.addInfoMsg("Reporting Date successfully updated.", request);
        return "redirect:/reporting-periods/reporting-dates/" + newReportingDate.getReportingPeriod().getId();
    }

    private void sendReportingDateActivationNotice(HttpServletRequest request, ReportingDate reportingDate, String action) {
        if (reportingDate == null || reportingDate.getReportingPeriod() == null || !isOpenStatus(reportingDate.getStatus())) {
            return;
        }
        String periodRange = (reportingDate.getReportingPeriod().getStartDate() == null ? "?" : reportingDate.getReportingPeriod().getStartDate())
                + " to "
                + (reportingDate.getReportingPeriod().getEndDate() == null ? "?" : reportingDate.getReportingPeriod().getEndDate());
        String host = "";
        try {
            host = commonService.getCurrentUrl(request);
        } catch (Exception ignored) {
            // Optional host URL for action links.
        }
        String link = (host == null || host.trim().isEmpty())
                ? "/reporting-periods/reporting-dates/" + reportingDate.getReportingPeriod().getId()
                : host + "/reporting-periods/reporting-dates/" + reportingDate.getReportingPeriod().getId();
        String subject = "Reporting Date Opened";
        String message = "A reporting date was " + action + " and set to OPEN.\n"
                + "Reporting date: " + (reportingDate.getEndDate() == null ? "N/A" : reportingDate.getEndDate()) + "\n"
                + "Period: " + periodRange + "\n"
                + "Impact: score capture is now available for this window.\n"
                + "Link: " + link;

        String adminEmail = commonService.getAdminEmail();
        String hrEmail = commonService.getHREmail();
        String loggedUserEmail = loggedUserEmail();
        if (hasText(adminEmail) && !sameEmail(adminEmail, loggedUserEmail)) {
            notificationService.sendUserMessageAsync(adminEmail.trim(), "Administrator", subject, message);
        }
        if (hasText(hrEmail)
                && !sameEmail(hrEmail, adminEmail)
                && !sameEmail(hrEmail, loggedUserEmail)) {
            notificationService.sendUserMessageAsync(hrEmail.trim(), "HR", subject, message);
        }
    }

    private String loggedUserEmail() {
        Account loggedUser = commonService.getLoggedUser();
        return loggedUser == null ? null : loggedUser.getEmail();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean sameEmail(String left, String right) {
        return hasText(left) && hasText(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean isOpenStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return "OPEN".equals(normalized) || "ACTIVE".equals(normalized);
    }

}
