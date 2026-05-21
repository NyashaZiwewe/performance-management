package hr.performancemanagement.controllers.department;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Department;
import hr.performancemanagement.service.api.AccountService;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.DepartmentService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Client;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final AccountService accountService;
    private final CommonService commonService;

    public DepartmentController(DepartmentService departmentService,
                                AccountService accountService,
                                CommonService commonService) {
        this.departmentService = departmentService;
        this.accountService = accountService;
        this.commonService = commonService;
    }

    @ModelAttribute("departmentForm")
    public Department departmentForm() {
        return new Department();
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {
        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Departments");
        modelAndView.addObject("canManageDepartments", canManage());
        PortletUtils.addMessagesToPage(modelAndView, request);
    }

    @RequestMapping
    public ModelAndView viewAllDepartments(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_DEPARTMENTS);
        modelAndView.addObject("pageTitle", "View All Departments");

        Account loggedUser = commonService.getLoggedUser();
        long clientId = loggedUser != null ? loggedUser.getClientId() : Client.CLIENT_ID;
        List<Department> departments = canManage()
                ? departmentService.listAllDepartments(clientId)
                : departmentService.listAllDepartments();
        List<Account> managers = accountService.listAllAccountsByClientId(clientId);

        Map<Integer, String> managerNames = new HashMap<Integer, String>();
        for (Account account : managers) {
            if (account != null && account.getId() > 0) {
                managerNames.put((int) account.getId(), account.getFullName());
            }
        }

        modelAndView.addObject("departments", departments);
        modelAndView.addObject("managers", managers);
        modelAndView.addObject("managerNames", managerNames);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-department", method = RequestMethod.POST)
    public String saveDepartment(HttpServletRequest request,
                                 @RequestParam("name") String name,
                                 @RequestParam(value = "managerId", required = false) Integer managerId) {
        if (!canManage()) {
            PortletUtils.addErrorMsg("You are not allowed to create departments.", request);
            return "redirect:/departments";
        }

        if (name == null || name.trim().isEmpty()) {
            PortletUtils.addErrorMsg("Department name is required.", request);
            return "redirect:/departments";
        }

        Account loggedUser = commonService.getLoggedUser();
        long clientId = loggedUser != null ? loggedUser.getClientId() : Client.CLIENT_ID;

        Department department = new Department();
        department.setClientId(clientId);
        department.setName(name.trim());
        department.setManager(managerId == null ? 0 : managerId);

        try {
            departmentService.saveDepartment(department);
            PortletUtils.addInfoMsg("Department was successfully created.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Department could not be saved: " + exception.getMessage(), request);
        }
        return "redirect:/departments";
    }

    @RequestMapping(value = "/update-department", method = RequestMethod.POST)
    public String updateDepartment(HttpServletRequest request,
                                   @RequestParam("id") long id,
                                   @RequestParam("name") String name,
                                   @RequestParam(value = "managerId", required = false) Integer managerId) {
        if (!canManage()) {
            PortletUtils.addErrorMsg("You are not allowed to update departments.", request);
            return "redirect:/departments";
        }

        if (name == null || name.trim().isEmpty()) {
            PortletUtils.addErrorMsg("Department name is required.", request);
            return "redirect:/departments";
        }

        try {
            Department department = departmentService.getDepartmentById(id);
            department.setName(name.trim());
            department.setManager(managerId == null ? 0 : managerId);
            if (department.getClientId() <= 0) {
                Account loggedUser = commonService.getLoggedUser();
                department.setClientId(loggedUser != null ? loggedUser.getClientId() : Client.CLIENT_ID);
            }
            departmentService.saveDepartment(department);
            PortletUtils.addInfoMsg("Department was successfully updated.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Department could not be updated: " + exception.getMessage(), request);
        }
        return "redirect:/departments";
    }

    @RequestMapping(value = "/delete-department", method = RequestMethod.POST)
    public String deleteDepartment(HttpServletRequest request, @RequestParam("id") long id) {
        if (!canManage()) {
            PortletUtils.addErrorMsg("You are not allowed to delete departments.", request);
            return "redirect:/departments";
        }

        try {
            Department department = departmentService.getDepartmentById(id);
            departmentService.deleteDepartment(department);
            PortletUtils.addInfoMsg("Department was successfully deleted.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Department could not be deleted: " + exception.getMessage(), request);
        }
        return "redirect:/departments";
    }

    private boolean canManage() {
        return commonService.isAdmin() || commonService.hasSpecialRights();
    }
}
