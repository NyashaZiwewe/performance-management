package hr.performancemanagement.controllers.account;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.service.api.AccountService;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.ConfigurationStatusService;
import hr.performancemanagement.service.api.DepartmentService;
import hr.performancemanagement.service.api.DivisionService;
import hr.performancemanagement.service.api.NotificationService;
import hr.performancemanagement.utils.constants.Pages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountsControllerTest {

    private DepartmentService departmentService;
    private DivisionService divisionService;
    private AccountService accountService;
    private CommonService commonService;
    private ConfigurationStatusService configurationStatusService;
    private AccountsController controller;

    @BeforeEach
    void setUp() {
        departmentService = mock(DepartmentService.class);
        divisionService = mock(DivisionService.class);
        accountService = mock(AccountService.class);
        commonService = mock(CommonService.class);
        configurationStatusService = mock(ConfigurationStatusService.class);
        controller = new AccountsController(
                departmentService,
                divisionService,
                accountService,
                commonService,
                mock(NotificationService.class),
                configurationStatusService
        );
    }

    @Test
    void specialRightsUserCanOpenEditAccount() {
        Account account = new Account();
        account.setId(12L);
        when(commonService.isAdmin()).thenReturn(false);
        when(commonService.hasSpecialRights()).thenReturn(true);
        when(accountService.getAccountById(12L)).thenReturn(account);
        when(accountService.listAllAccounts()).thenReturn(Collections.emptyList());
        when(departmentService.listAllDepartments()).thenReturn(Collections.emptyList());
        when(divisionService.listAllDivisions()).thenReturn(Collections.emptyList());
        when(configurationStatusService.getAccountWarnings()).thenReturn(Collections.emptyList());

        ModelAndView result = controller.editAccount(12L, new MockHttpServletRequest());

        assertEquals(Pages.EDIT_ACCOUNT, result.getViewName());
        assertSame(account, result.getModel().get("account"));
    }

    @Test
    void regularUserCannotOpenEditAccount() {
        when(commonService.isAdmin()).thenReturn(false);
        when(commonService.hasSpecialRights()).thenReturn(false);

        ModelAndView result = controller.editAccount(12L, new MockHttpServletRequest());

        assertEquals("redirect:/accounts", result.getViewName());
        verifyNoInteractions(accountService);
    }
}
