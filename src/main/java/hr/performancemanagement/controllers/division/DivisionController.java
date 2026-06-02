package hr.performancemanagement.controllers.division;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Division;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.DivisionService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/divisions")
public class DivisionController {

    private final DivisionService divisionService;
    private final CommonService commonService;

    public DivisionController(DivisionService divisionService, CommonService commonService) {
        this.divisionService = divisionService;
        this.commonService = commonService;
    }

    @ModelAttribute("divisionForm")
    public Division divisionForm() {
        return new Division();
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {
        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Divisions");
        modelAndView.addObject("canManageDivisions", canManage());
        PortletUtils.addMessagesToPage(modelAndView, request);
    }

    @RequestMapping
    public ModelAndView viewAllDivisions(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_DIVISIONS);
        modelAndView.addObject("pageTitle", "View All Divisions");

        long clientId = commonService.getConfiguredClientId();
        List<Division> divisions = divisionService.listAllDivisions(clientId);

        modelAndView.addObject("divisions", divisions);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-division", method = RequestMethod.POST)
    public String saveDivision(HttpServletRequest request, Division newDivision) {
        if (!canManage()) {
            PortletUtils.addErrorMsg("You are not allowed to create divisions.", request);
            return "redirect:/divisions";
        }

        if (newDivision.getName() == null || newDivision.getName().trim().isEmpty()) {
            PortletUtils.addErrorMsg("Division name is required.", request);
            return "redirect:/divisions";
        }

        long clientId = commonService.getConfiguredClientId();
        newDivision.setClientId(clientId);
        normalizeDivisionFields(newDivision);

        try {
            divisionService.saveDivision(newDivision);
            PortletUtils.addInfoMsg("Division was successfully created.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Division could not be saved: " + exception.getMessage(), request);
        }
        return "redirect:/divisions";
    }

    @RequestMapping(value = "/update-division", method = RequestMethod.POST)
    public String updateDivision(HttpServletRequest request, Division updatedDivision) {
        if (!canManage()) {
            PortletUtils.addErrorMsg("You are not allowed to update divisions.", request);
            return "redirect:/divisions";
        }

        if (updatedDivision.getName() == null || updatedDivision.getName().trim().isEmpty()) {
            PortletUtils.addErrorMsg("Division name is required.", request);
            return "redirect:/divisions";
        }

        try {
            Division existingDivision = divisionService.getDivisionById(updatedDivision.getId());
            existingDivision.setName(updatedDivision.getName());
            existingDivision.setCode(updatedDivision.getCode());
            existingDivision.setAddress(updatedDivision.getAddress());
            existingDivision.setEmail(updatedDivision.getEmail());
            existingDivision.setPhone(updatedDivision.getPhone());
            existingDivision.setWebsite(updatedDivision.getWebsite());
            existingDivision.setColorCode(updatedDivision.getColorCode());
            if (existingDivision.getClientId() <= 0) {
                existingDivision.setClientId(commonService.getConfiguredClientId());
            }
            normalizeDivisionFields(existingDivision);
            divisionService.saveDivision(existingDivision);
            PortletUtils.addInfoMsg("Division was successfully updated.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Division could not be updated: " + exception.getMessage(), request);
        }
        return "redirect:/divisions";
    }

    @RequestMapping(value = "/delete-division", method = RequestMethod.POST)
    public String deleteDivision(HttpServletRequest request, @RequestParam("id") long id) {
        if (!canManage()) {
            PortletUtils.addErrorMsg("You are not allowed to delete divisions.", request);
            return "redirect:/divisions";
        }

        try {
            Division division = divisionService.getDivisionById(id);
            divisionService.deleteDivision(division);
            PortletUtils.addInfoMsg("Division was successfully deleted.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Division could not be deleted: " + exception.getMessage(), request);
        }
        return "redirect:/divisions";
    }

    private void normalizeDivisionFields(Division division) {
        division.setName(normalizeText(division.getName()));
        division.setCode(normalizeText(division.getCode()));
        division.setAddress(normalizeText(division.getAddress()));
        division.setEmail(normalizeText(division.getEmail()));
        division.setPhone(normalizeText(division.getPhone()));
        division.setWebsite(normalizeText(division.getWebsite()));
        division.setColorCode(normalizeText(division.getColorCode()));
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean canManage() {
        return commonService.isAdmin() || commonService.hasSpecialRights();
    }
}
