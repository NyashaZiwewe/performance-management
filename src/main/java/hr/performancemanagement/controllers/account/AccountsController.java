package hr.performancemanagement.controllers.account;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Department;
import hr.performancemanagement.entities.Division;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Client;
import hr.performancemanagement.utils.constants.Pages;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
@Controller
@RequestMapping(value="/accounts")
public class AccountsController {
    private final DepartmentService departmentService;
    private final DivisionService divisionService;
    private final AccountService accountService;
    private final CommonService cs;
    private final NotificationService notificationService;
    private final ConfigurationStatusService configurationStatusService;

    public AccountsController(
            DepartmentService departmentService,
            DivisionService divisionService,
            AccountService accountService,
            CommonService cs,
            NotificationService notificationService,
            ConfigurationStatusService configurationStatusService
    ) {
        this.departmentService = departmentService;
        this.divisionService = divisionService;
        this.accountService = accountService;
        this.cs = cs;
        this.notificationService = notificationService;
        this.configurationStatusService = configurationStatusService;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {

        List<Department> DEPARTMENT_LIST = departmentService.listAllDepartments();
        List<Division> DIVISION_LIST = divisionService.listAllDivisions();
        List<Account> ACCOUNTS_LIST = accountService.listAllAccounts();
        List<String> accountWarnings = configurationStatusService.getAccountWarnings();
        List<Department> cleanDepartments = new ArrayList<Department>();
        for (Department department : DEPARTMENT_LIST) {
            if (department != null && department.getId() > 0) {
                cleanDepartments.add(department);
            }
        }
        List<Division> cleanDivisions = new ArrayList<Division>();
        for (Division division : DIVISION_LIST) {
            if (division != null && division.getId() > 0) {
                cleanDivisions.add(division);
            }
        }
        List<Account> cleanAccounts = new ArrayList<Account>();
        for (Account account : ACCOUNTS_LIST) {
            if (account != null && account.getId() > 0) {
                cleanAccounts.add(account);
            }
        }
        boolean hasDepartments = !cleanDepartments.isEmpty();
        boolean hasDivisions = !cleanDivisions.isEmpty();
        boolean hasSupervisors = !cleanAccounts.isEmpty();

        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Accounts");
        modelAndView.addObject("departmentsList", cleanDepartments);
        modelAndView.addObject("divisionsList", cleanDivisions);
        modelAndView.addObject("accountsList", cleanAccounts);
        modelAndView.addObject("hasDepartments", hasDepartments);
        modelAndView.addObject("hasDivisions", hasDivisions);
        modelAndView.addObject("hasSupervisors", hasSupervisors);
        modelAndView.addObject("accountWarnings", accountWarnings);
        modelAndView.addObject("canSaveAccount", true);
        PortletUtils.addMessagesToPage(modelAndView, request);
    }

    @RequestMapping
    public ModelAndView viewAccounts(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_ACCOUNTS);
        modelAndView.addObject("pageTitle", "View Accounts");
        List<Account> accounts = accountService.listAllAccounts();
        modelAndView.addObject("accounts", accounts);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/add-account")
    public ModelAndView addAccount(HttpServletRequest request) {

        ModelAndView modelAndView = new ModelAndView(Pages.ADD_ACCOUNT);
        modelAndView.addObject("pageTitle", "New Account");
        modelAndView.addObject("account", new Account());
        modelAndView.addObject("accountTypes", new Account());

        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-account", method = RequestMethod.POST)
    public String saveAccount(HttpServletRequest request, Account newAccount) {
        List<String> errors = new ArrayList<String>();
        if (newAccount.getFullName() == null || newAccount.getFullName().trim().isEmpty()) {
            errors.add("Employee full name is required.");
        }
        if (newAccount.getEmail() == null || newAccount.getEmail().trim().isEmpty()) {
            errors.add("Employee email is required.");
        }
        if (newAccount.getPosition() == null || newAccount.getPosition().trim().isEmpty()) {
            errors.add("Employee position is required.");
        }
        if (newAccount.getAccountType() == null || newAccount.getAccountType().trim().isEmpty()) {
            errors.add("Account type is required.");
        }
        if (newAccount.getStatus() == null || newAccount.getStatus().trim().isEmpty()) {
            errors.add("Account status is required.");
        }
        if (newAccount.getAdmin() == null || newAccount.getAdmin().trim().isEmpty()) {
            errors.add("Admin role is required.");
        }
        if (newAccount.getAccounts() == null || newAccount.getAccounts().trim().isEmpty()) {
            errors.add("Accounts role is required.");
        }
        if (newAccount.getSpecial() == null || newAccount.getSpecial().trim().isEmpty()) {
            errors.add("Special rights selection is required.");
        }
        if (!errors.isEmpty()) {
            PortletUtils.addErrorMsg(String.join(" ", errors), request);
            return "redirect:/accounts/add-account";
        }

        if (accountService.findAccountByEmail(newAccount.getEmail()) != null) {
            PortletUtils.addErrorMsg("An employee with email " + newAccount.getEmail() + " already exists.", request);
            return "redirect:/accounts/add-account";
        }

        Account loggedUser = cs.getLoggedUser();
        long clientId = loggedUser != null ? loggedUser.getClientId() : Client.CLIENT_ID;

        List<Department> departments = departmentService.listAllDepartments();
        List<Division> divisions = divisionService.listAllDivisions();
        List<Account> accounts = accountService.listAllAccountsByClientId(clientId);
        boolean hasDepartments = departments.stream().anyMatch(department -> department != null && department.getId() > 0);
        boolean hasDivisions = divisions.stream().anyMatch(division -> division != null && division.getId() > 0);
        if (!hasDepartments) {
            newAccount.setDepartment(null);
        }
        if (!hasDivisions) {
            newAccount.setDivision(null);
        }
        if (accounts.isEmpty()) {
            newAccount.setSupervisor(null);
        }

        String setupToken = RandomStringUtils.randomAlphanumeric(40);
        newAccount.setPassword(null);
        newAccount.setResetPassword(cs.hashResetToken(setupToken));
        newAccount.setClientId(clientId);
        accountService.addAccount(newAccount);
        sendSetupLink(newAccount, setupToken, request);
        PortletUtils.addInfoMsg("Employee record was successfully created. A password setup link was sent if email delivery is available.", request);
        return "redirect:/accounts/view-account/"+ newAccount.getId();

    }

    @RequestMapping("/view-account/{id}")
    public ModelAndView viewAccount(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_ACCOUNT);
        modelAndView.addObject("pageTitle", "View Account ");
        Account account = accountService.getAccountById(id);
        modelAndView.addObject("account", account);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/edit-account/{id}")
    public ModelAndView editAccount(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.EDIT_ACCOUNT);
        modelAndView.addObject("pageTitle", "Update Account");
        Account account = accountService.getAccountById(id);
        modelAndView.addObject("account", account);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/update-account", method = RequestMethod.POST)
    public String updateAccount(HttpServletRequest request, Account account) {

        try {
            accountService.saveAccount(account);
            PortletUtils.addInfoMsg("Employee record successfully updated.", request);
            return "redirect:/accounts/view-account/"+ account.getId();
        }catch (Exception e){
            PortletUtils.addErrorMsg("Employee record wasn't updated.", request);
            return "redirect:/accounts/view-account/"+ account.getId();
        }

    }


    @RequestMapping(value = "/delete-account", method = RequestMethod.POST)
    public String deleteAccount(HttpServletRequest request, Account account) {

        try{
            accountService.deleteAccount(account);
            PortletUtils.addInfoMsg("Account was successfully deleted", request);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Delete failed with message: You cannot delete this user because has scorecards and or subordinates ", request);
        }
        return "redirect:/accounts/";
    }

    private void sendSetupLink(Account account, String setupToken, HttpServletRequest request) {
        try {
            URL setupLink = new URL(cs.getCurrentUrl(request).concat("/change-password/" + setupToken));
            notificationService.sendAccountSetup(account, setupLink.toString());
        } catch (Exception ignored) {
            // Account creation must still succeed when email delivery is unavailable.
        }
    }



}
