package hr.performancemanagement.config;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.service.api.AccountService;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.SystemSettingService;
import hr.performancemanagement.utils.constants.PMConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private AccountService accountService;
    private CommonService commonService;
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        commonService = mock(CommonService.class);
        securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "accountService", accountService);
        ReflectionTestUtils.setField(securityConfig, "commonService", commonService);
        ReflectionTestUtils.setField(securityConfig, "systemSettingService", mock(SystemSettingService.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {PMConstants.STATUS_IN_ACTIVE, "INACTIVE", "SUSPENDED", "DELETED"})
    void inactiveAccountsCannotAuthenticate(String status) {
        Account account = account(status);
        Authentication authentication = new UsernamePasswordAuthenticationToken(account.getEmail(), "password");
        when(accountService.findAccountByEmail(account.getEmail())).thenReturn(account);
        when(commonService.matchesPassword("password", account.getPassword())).thenReturn(true);

        assertThrows(DisabledException.class,
                () -> authenticationProvider().authenticate(authentication));

        verify(commonService, never()).requiresPasswordUpgrade(account.getPassword());
    }

    @Test
    void accountsWithoutStatusCannotAuthenticate() {
        Account account = account(null);
        Authentication authentication = new UsernamePasswordAuthenticationToken(account.getEmail(), "password");
        when(accountService.findAccountByEmail(account.getEmail())).thenReturn(account);
        when(commonService.matchesPassword("password", account.getPassword())).thenReturn(true);

        assertThrows(DisabledException.class,
                () -> authenticationProvider().authenticate(authentication));

        verify(commonService, never()).requiresPasswordUpgrade(account.getPassword());
    }

    @Test
    void activeAccountCanAuthenticate() {
        Account account = account(PMConstants.STATUS_ACTIVE);
        account.setAdmin(PMConstants.IS_NOT_ADMIN);
        Authentication authentication = new UsernamePasswordAuthenticationToken(account.getEmail(), "password");
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(accountService.findAccountByEmail(account.getEmail())).thenReturn(account);
        when(commonService.matchesPassword("password", account.getPassword())).thenReturn(true);
        when(commonService.requiresPasswordUpgrade(account.getPassword())).thenReturn(false);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            Authentication result = authenticationProvider().authenticate(authentication);

            assertEquals(account.getEmail(), result.getName());
            assertEquals(account, request.getSession().getAttribute("loggedUser"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private AuthenticationProvider authenticationProvider() {
        try {
            AuthenticationManagerBuilder builder = mock(AuthenticationManagerBuilder.class);
            ArgumentCaptor<AuthenticationProvider> providerCaptor = ArgumentCaptor.forClass(AuthenticationProvider.class);
            when(builder.authenticationProvider(any(AuthenticationProvider.class))).thenReturn(builder);
            securityConfig.configure(builder);
            verify(builder).authenticationProvider(providerCaptor.capture());
            return providerCaptor.getValue();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Account account(String status) {
        Account account = new Account();
        account.setEmail("employee@example.com");
        account.setPassword("stored-password");
        account.setStatus(status);
        return account;
    }
}
