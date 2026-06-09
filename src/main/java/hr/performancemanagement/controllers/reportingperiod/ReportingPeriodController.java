package hr.performancemanagement.controllers.reportingperiod;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingDateActivityPeriod;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.StrategicObjective;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.NotificationService;
import hr.performancemanagement.service.api.ReportingDateService;
import hr.performancemanagement.service.api.ReportingDateActivityPeriodService;
import hr.performancemanagement.service.api.ReportingPeriodService;
import hr.performancemanagement.service.api.StrategicObjectiveService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Pages;
import hr.performancemanagement.utils.dto.ActivityPeriodProgress;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final ReportingDateActivityPeriodService reportingDateActivityPeriodService;

    public ReportingPeriodController(ReportingPeriodService reportingPeriodService,
                                     StrategicObjectiveService strategicObjectiveService,
                                     ReportingDateService reportingDateService,
                                     NotificationService notificationService,
                                     CommonService commonService,
                                     ReportingDateActivityPeriodService reportingDateActivityPeriodService) {
        this.reportingPeriodService = reportingPeriodService;
        this.strategicObjectiveService = strategicObjectiveService;
        this.reportingDateService = reportingDateService;
        this.notificationService = notificationService;
        this.commonService = commonService;
        this.reportingDateActivityPeriodService = reportingDateActivityPeriodService;
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

        try {
            newReportingPeriod.setClientId(commonService.getConfiguredClientId());
            newReportingPeriod.setStatus("ACTIVE");
            reportingPeriodService.saveReportingPeriod(newReportingPeriod);
            PortletUtils.addInfoMsg("A new reporting period was successfully created and activated", request);
        } catch (IllegalArgumentException exception) {
            PortletUtils.addErrorMsg(exception.getMessage(), request);
            return "redirect:/reporting-periods/add-reporting-period";
        }
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

        try {
            reportingPeriod.setClientId(commonService.getConfiguredClientId());
            reportingPeriodService.saveReportingPeriod(reportingPeriod);
            if (isInactiveStatus(reportingPeriod.getStatus())) {
                PortletUtils.addInfoMsg("Reporting period updated. Open reporting dates were closed and active scorecards in this period were deactivated.", request);
            } else {
                PortletUtils.addInfoMsg("Reporting period successfully updated.", request);
            }
        } catch (IllegalArgumentException exception) {
            PortletUtils.addErrorMsg(exception.getMessage(), request);
            if (reportingPeriod != null && reportingPeriod.getId() > 0) {
                return "redirect:/reporting-periods/edit-reporting-period/" + reportingPeriod.getId();
            }
        }
        return "redirect:/reporting-periods";
    }

    @RequestMapping(value = "/delete-reporting-period/{id}", method = RequestMethod.POST)
    public String deleteReportingPeriod(@PathVariable("id") long id, HttpServletRequest request) {
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(id);
        if (reportingPeriod == null) {
            PortletUtils.addErrorMsg("Reporting period could not be found.", request);
            return "redirect:/reporting-periods";
        }
        try {
            reportingPeriodService.deleteReportingPeriod(reportingPeriod);
            PortletUtils.addInfoMsg("Reporting period successfully deleted.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Reporting period could not be deleted. " + PortletUtils.sanitiseUserErrorMessage(exception.getMessage()), request, exception);
        }
        return "redirect:/reporting-periods";
    }

    @RequestMapping("/strategic-goals/{id}")
    public String viewStrategicObjectives(@PathVariable("id") long id, HttpServletRequest request) {
        PortletUtils.addInfoMsg("Manage strategic goals under predefined metrics for the selected reporting period.", request);
        return "redirect:/gears?reportingPeriodId=" + id;
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
        modelAndView.addObject("activityPeriod", new ReportingDateActivityPeriod());
        Map<Long, List<ReportingDateActivityPeriod>> activityPeriodsByReportingDateId = new HashMap<Long, List<ReportingDateActivityPeriod>>();
        Map<Long, ActivityPeriodProgress> activityProgressByReportingDateId = new HashMap<Long, ActivityPeriodProgress>();
        Map<Long, String> currentActivityByReportingDateId = new HashMap<Long, String>();
        for (ReportingDate reportingDate : reportingDateList) {
            activityPeriodsByReportingDateId.put(reportingDate.getId(), reportingDateActivityPeriodService.listActivityPeriods(reportingDate));
            activityProgressByReportingDateId.put(reportingDate.getId(), reportingDateActivityPeriodService.getProgress(reportingDate));
            currentActivityByReportingDateId.put(reportingDate.getId(), reportingDateActivityPeriodService.getCurrentActivityLabel(reportingDate));
        }
        modelAndView.addObject("activityPeriodsByReportingDateId", activityPeriodsByReportingDateId);
        modelAndView.addObject("activityProgressByReportingDateId", activityProgressByReportingDateId);
        modelAndView.addObject("currentActivityByReportingDateId", currentActivityByReportingDateId);
        modelAndView.addObject("canManageActivityPeriods", commonService.isAdmin());

        preparePage(modelAndView, request);
        return modelAndView;
    }
    @RequestMapping(value = "/add-strategic-objective", method = RequestMethod.POST)
    public String addStrategicObjective(HttpServletRequest request, StrategicObjective newStrategicObjective) {

        strategicObjectiveService.addStrategicObjective(newStrategicObjective);
        PortletUtils.addInfoMsg("Strategic goal successfully added.", request);
        return "redirect:/gears?reportingPeriodId=" + newStrategicObjective.getReportingPeriod().getId();
    }

    @RequestMapping(value = "/save-strategic-objective", method = RequestMethod.POST)
    public String saveStrategicObjective( HttpServletRequest request, StrategicObjective strategicObjective) {

        strategicObjectiveService.saveStrategicObjective(strategicObjective);
        PortletUtils.addInfoMsg("Strategic goal successfully updated.", request);
        return "redirect:/gears?reportingPeriodId=" + strategicObjective.getReportingPeriod().getId();
    }

    @RequestMapping(value = "/add-reporting-date", method = RequestMethod.POST)
    public String addReportingDate(HttpServletRequest request, ReportingDate newReportingDate) {

        Long reportingPeriodId = resolveReportingPeriodId(newReportingDate);
        try {
            reportingDateService.saveReportingDate(newReportingDate);
            sendReportingDateActivationNotice(request, newReportingDate, "created");
            PortletUtils.addInfoMsg("Reporting Date successfully added.", request);
            reportingPeriodId = resolveReportingPeriodId(newReportingDate);
        } catch (IllegalArgumentException exception) {
            PortletUtils.addErrorMsg(exception.getMessage(), request);
        }
        return reportingDatesRedirect(reportingPeriodId);
    }

    @RequestMapping(value = "/save-reporting-date", method = RequestMethod.POST)
    public String saveReportingDate( HttpServletRequest request, ReportingDate newReportingDate) {

        Long reportingPeriodId = resolveReportingPeriodId(newReportingDate);
        try {
            reportingDateService.saveReportingDate(newReportingDate);
            sendReportingDateActivationNotice(request, newReportingDate, "updated");
            PortletUtils.addInfoMsg("Reporting Date successfully updated.", request);
            reportingPeriodId = resolveReportingPeriodId(newReportingDate);
        } catch (IllegalArgumentException exception) {
            PortletUtils.addErrorMsg(exception.getMessage(), request);
        }
        return reportingDatesRedirect(reportingPeriodId);
    }

    @RequestMapping(value = "/delete-reporting-date/{id}", method = RequestMethod.POST)
    public String deleteReportingDate(@PathVariable("id") long id,
                                      @RequestParam(value = "reportingPeriodId", required = false) Long reportingPeriodId,
                                      HttpServletRequest request) {
        ReportingDate reportingDate = reportingDateService.getReportingDateById(id);
        if (reportingDate == null) {
            PortletUtils.addErrorMsg("Reporting date could not be found.", request);
            return reportingDatesRedirect(reportingPeriodId);
        }
        Long redirectReportingPeriodId = reportingPeriodId;
        if ((redirectReportingPeriodId == null || redirectReportingPeriodId <= 0) && reportingDate.getReportingPeriod() != null) {
            redirectReportingPeriodId = reportingDate.getReportingPeriod().getId();
        }
        try {
            reportingDateService.deleteReportingDate(reportingDate);
            PortletUtils.addInfoMsg("Reporting date successfully deleted.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Reporting date could not be deleted. " + PortletUtils.sanitiseUserErrorMessage(exception.getMessage()), request, exception);
        }
        return reportingDatesRedirect(redirectReportingPeriodId);
    }

    @RequestMapping(value = "/save-activity-period", method = RequestMethod.POST)
    public String saveActivityPeriod(HttpServletRequest request, ReportingDateActivityPeriod activityPeriod) {
        Long reportingPeriodId = resolveReportingPeriodId(activityPeriod);
        if (!commonService.isAdmin()) {
            PortletUtils.addErrorMsg("Only administrators can create or extend activity periods.", request);
            return reportingDatesRedirect(reportingPeriodId);
        }
        try {
            reportingDateActivityPeriodService.saveActivityPeriod(activityPeriod);
            reportingPeriodId = resolveReportingPeriodId(activityPeriod);
            sendActivityPeriodChangeNotice(activityPeriod, "saved");
            PortletUtils.addInfoMsg("Activity period successfully saved. Its last date is the cutoff date.", request);
        } catch (IllegalArgumentException exception) {
            PortletUtils.addErrorMsg(exception.getMessage(), request);
        }
        return reportingDatesRedirect(reportingPeriodId);
    }

    @RequestMapping(value = "/delete-activity-period/{id}", method = RequestMethod.POST)
    public String deleteActivityPeriod(@PathVariable("id") long id,
                                       @RequestParam(value = "reportingPeriodId", required = false) Long reportingPeriodId,
                                       HttpServletRequest request) {
        if (!commonService.isAdmin()) {
            PortletUtils.addErrorMsg("Only administrators can delete activity periods.", request);
            return reportingDatesRedirect(reportingPeriodId);
        }
        ReportingDateActivityPeriod activityPeriod = reportingDateActivityPeriodService.getActivityPeriodById(id);
        if (activityPeriod == null) {
            PortletUtils.addErrorMsg("Activity period could not be found.", request);
            return reportingDatesRedirect(reportingPeriodId);
        }
        if (reportingPeriodId == null || reportingPeriodId <= 0) {
            reportingPeriodId = resolveReportingPeriodId(activityPeriod);
        }
        try {
            sendActivityPeriodChangeNotice(activityPeriod, "deleted");
            reportingDateActivityPeriodService.deleteActivityPeriod(activityPeriod);
            PortletUtils.addInfoMsg("Activity period successfully deleted.", request);
        } catch (IllegalArgumentException exception) {
            PortletUtils.addErrorMsg(exception.getMessage(), request);
        }
        return reportingDatesRedirect(reportingPeriodId);
    }

    private Long resolveReportingPeriodId(ReportingDate reportingDate) {
        if (reportingDate == null || reportingDate.getReportingPeriod() == null || reportingDate.getReportingPeriod().getId() <= 0) {
            return null;
        }
        return reportingDate.getReportingPeriod().getId();
    }

    private Long resolveReportingPeriodId(ReportingDateActivityPeriod activityPeriod) {
        if (activityPeriod == null || activityPeriod.getReportingDate() == null) {
            return null;
        }
        ReportingDate reportingDate = activityPeriod.getReportingDate();
        if (reportingDate.getReportingPeriod() != null && reportingDate.getReportingPeriod().getId() > 0) {
            return reportingDate.getReportingPeriod().getId();
        }
        if (reportingDate.getId() <= 0) {
            return null;
        }
        ReportingDate resolved = reportingDateService.getReportingDateById(reportingDate.getId());
        return resolveReportingPeriodId(resolved);
    }

    private String reportingDatesRedirect(Long reportingPeriodId) {
        if (reportingPeriodId == null || reportingPeriodId <= 0) {
            return "redirect:/reporting-periods";
        }
        return "redirect:/reporting-periods/reporting-dates/" + reportingPeriodId;
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

    private void sendActivityPeriodChangeNotice(ReportingDateActivityPeriod activityPeriod, String action) {
        if (activityPeriod == null || !hasText(commonService.getHREmail())) {
            return;
        }
        ReportingDate reportingDate = activityPeriod.getReportingDate();
        String subject = "Reporting date activity period " + action;
        String message = "An administrator " + action + " a reporting date activity period.\n"
                + "Reporting date: " + (reportingDate == null ? "N/A" : reportingDate.getEndDate()) + "\n"
                + "Activity: " + activityLabel(activityPeriod.getActivityType()) + "\n"
                + "Activity period: " + activityPeriod.getStartDate() + " to " + activityPeriod.getEndDate() + "\n"
                + "Cutoff date: " + activityPeriod.getEndDate() + "\n"
                + "Impact: actions outside the active configured period are restricted.\n"
                + "Link: /reporting-periods/reporting-dates/" + resolveReportingPeriodId(activityPeriod);
        notificationService.sendUserMessageAsync(commonService.getHREmail().trim(), "HR", subject, message);
    }

    private String activityLabel(String activityType) {
        if ("TARGET_CAPTURE".equalsIgnoreCase(activityType)) {
            return "Target Capture and Approval";
        }
        if ("SCORE_CAPTURE".equalsIgnoreCase(activityType)) {
            return "Score Capture and Agreement";
        }
        if ("MODERATION".equalsIgnoreCase(activityType)) {
            return "Moderation";
        }
        return activityType == null ? "N/A" : activityType;
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

    private boolean isInactiveStatus(String status) {
        return status != null
                && ("IN_ACTIVE".equalsIgnoreCase(status.trim()) || "INACTIVE".equalsIgnoreCase(status.trim()));
    }

}
