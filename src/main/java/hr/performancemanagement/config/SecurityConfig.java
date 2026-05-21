package hr.performancemanagement.config;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.service.api.AccountService;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.SystemSettingService;
import hr.performancemanagement.utils.constants.Client;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin";
    private static final String BOOTSTRAP_ADMIN_PASSWORD = "admin123";

    @Autowired
    private AccountService accountService;
    @Autowired
    private CommonService commonService;
    @Autowired
    private SystemSettingService systemSettingService;

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(
                new AuthenticationProvider() {
                    @Override
                    public Authentication authenticate(Authentication authentication) {
                        final String name = authentication.getName();
                        final String password = String.valueOf(authentication.getCredentials());

                        if (isBootstrapAdminLogin(name, password)) {
                            Account bootstrapAdmin = buildBootstrapAdmin();
                            populateSession(bootstrapAdmin, true);

                            Set<String> authorities = new HashSet<>();
                            authorities.add(bootstrapAdmin.getAdmin());
                            Set<SimpleGrantedAuthority> roles = authorities.stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toSet());

                            return new UsernamePasswordAuthenticationToken(name, password, roles);
                        }

                        Account account = accountService.findAccountByEmail(name);

                        if (account == null) {
                            throw new BadCredentialsException("Account does not exist");
                        }

                        if (!name.equals(account.getEmail()) || !commonService.matchesPassword(password, account.getPassword())) {
                            throw new BadCredentialsException("Invalid username or password");
                        }

                        if (commonService.requiresPasswordUpgrade(account.getPassword())) {
                            String encodedPassword = commonService.encodePassword(password);
                            accountService.upgradePassword(account.getId(), encodedPassword);
                            account.setPassword(encodedPassword);
                        }

                        populateSession(account, false);

                        Set<String> authorities = new HashSet<>();
                        authorities.add(account.getAdmin());
                        Set<SimpleGrantedAuthority> roles = authorities.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toSet());

                        return new UsernamePasswordAuthenticationToken(name, password, roles);

                    }

                    @Override
                    public boolean supports(Class<?> aClass) {
                        return aClass.equals(UsernamePasswordAuthenticationToken.class);
                    }
                }
        );
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/login**", "/logout", "/reset-password", "/save-password", "/change-password/**", "/set-reset").permitAll()
                .and().authorizeRequests().anyRequest().authenticated()
                .and().formLogin().loginPage("/login").successHandler(successHandler())
                .permitAll()
                .and().logout()
                .deleteCookies("remove")
                .invalidateHttpSession(true)
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .and().csrf().disable().cors();
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    javax.servlet.http.HttpServletRequest request,
                    javax.servlet.http.HttpServletResponse response,
                    Authentication authentication) throws java.io.IOException, javax.servlet.ServletException {
                HttpSession session = request.getSession(false);
                boolean bootstrapAdmin = session != null && Boolean.TRUE.equals(session.getAttribute("bootstrapAdmin"));
                String target = bootstrapAdmin ? "/system-settings" : "/";
                getRedirectStrategy().sendRedirect(request, response, target);
            }
        };
        handler.setUseReferer(false);
        handler.setDefaultTargetUrl("/");
        return handler;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/img/**", "/css/**", "/js/**", "/fonts/**", "/font-awesome.css/**", "/font-awesome/**");
    }

    private boolean isBootstrapAdminLogin(String name, String password) {
        return BOOTSTRAP_ADMIN_EMAIL.equals(name)
                && BOOTSTRAP_ADMIN_PASSWORD.equals(password)
                && systemSettingService.isBootstrapAdminAvailable();
    }

    private Account buildBootstrapAdmin() {
        Account account = new Account();
        account.setId(0L);
        account.setClientId(Client.CLIENT_ID);
        account.setFullName("Bootstrap Administrator");
        account.setEmail(BOOTSTRAP_ADMIN_EMAIL);
        account.setPosition("Bootstrap Setup");
        account.setAccountType("BOOTSTRAP_ADMIN");
        account.setRole("BOOTSTRAP_ADMIN");
        account.setAdmin(PMConstants.IS_ADMIN);
        account.setSpecial(PMConstants.HAS_SPECIAL_RIGHTS);
        account.setAccounts("NO");
        account.setStatus(PMConstants.STATUS_ACTIVE);
        return account;
    }

    private void populateSession(Account account, boolean bootstrapAdmin) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attributes.getRequest().getSession();
        session.setAttribute("loggedUser", account);
        session.setAttribute("bootstrapAdmin", bootstrapAdmin);
    }
}
