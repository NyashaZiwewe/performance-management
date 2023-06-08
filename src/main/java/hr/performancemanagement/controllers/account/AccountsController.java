package hr.performancemanagement.controllers.account;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Department;
import hr.performancemanagement.entities.Division;
import hr.performancemanagement.repository.AccountRepository;
import hr.performancemanagement.service.AccountService;
import hr.performancemanagement.service.DepartmentService;
import hr.performancemanagement.service.DivisionService;
import hr.performancemanagement.service.PdfService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Client;
import hr.performancemanagement.utils.constants.Pages;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
@Controller
@RequestMapping(value="/accounts")
public class AccountsController {
    @Autowired
    private final AccountRepository accountRepository;
    @Autowired
    private final DepartmentService departmentService;
    @Autowired
    private final DivisionService divisionService;
    @Autowired
    private final AccountService accountService;

    @Autowired
    private final PdfService pdfService;

    public AccountsController(AccountRepository accountRepository, DepartmentService departmentService, DivisionService divisionService, AccountService accountService, PdfService pdfService) {
        this.accountRepository = accountRepository;
        this.departmentService = departmentService;
        this.divisionService = divisionService;
        this.accountService = accountService;
        this.pdfService = pdfService;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {

        List<Department> DEPARTMENT_LIST = departmentService.listAllDepartments();
        List<Division> DIVISION_LIST = divisionService.listAllDivisions();
        List<Account> ACCOUNTS_LIST = accountService.listAllAccounts();

        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Accounts");
        modelAndView.addObject("departmentsList", DEPARTMENT_LIST);
        modelAndView.addObject("divisionsList", DIVISION_LIST);
        modelAndView.addObject("accountsList", ACCOUNTS_LIST);
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

        String password = RandomStringUtils.randomAlphanumeric(8);
        newAccount.setPassword(password);
        newAccount.setClientId(Client.CLIENT_ID);
        accountService.addAccount(newAccount);
        PortletUtils.addInfoMsg("Employee record was successfully created. ", request);
        return "redirect:/accounts/view-account/"+ newAccount.getId();

    }

    @RequestMapping("/view-account/{id}")
    public ModelAndView viewAccount(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_ACCOUNT);
        modelAndView.addObject("pageTitle", "View Account ");
        Account account = accountRepository.findAccountById(id);
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

    @GetMapping("/download-pdf")
    public void downloadPdf(HttpServletResponse response){
        try {
            Path file = Paths.get(pdfService.generatePdf().getAbsolutePath());
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
