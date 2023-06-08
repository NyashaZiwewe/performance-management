package hr.performancemanagement.config;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.service.AccountService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    AccountService accountService;

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(
                new AuthenticationProvider() {
                    @Override
                    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

                        final String name = authentication.getName();
                        final String password = authentication.getCredentials().toString();

                        Account account = accountService.findAccountByEmail(name);

//                        UserDetails systemUser = User.withUsername("sysdev")
//                                .password("#pass123").authorities("system:dev").build();
                        if(account !=null) {

                            if (name.equals(account.getEmail()) && password.equals(account.getPassword())) {

                                ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                                attr.getRequest().getSession().setAttribute("loggedUser", account);
                                attr.getRequest().getSession().setAttribute("userId", account.getId());
                                attr.getRequest().getSession().setAttribute("name", account.getFullName());
                                attr.getRequest().getSession().setAttribute("position", account.getPosition());
                                attr.getRequest().getSession().setAttribute("clientId", account.getClientId());
                                attr.getRequest().getSession().setAttribute("accountType", account.getAccountType());
                                attr.getRequest().getSession().setAttribute("email", account.getEmail());
                                attr.getRequest().getSession().setAttribute("supervisor", account.getSupervisor());
                                attr.getRequest().getSession().setAttribute("department", account.getDepartment());
                                attr.getRequest().getSession().setAttribute("division", account.getDivision());
                                attr.getRequest().getSession().setAttribute("special", account.getSpecial());
                                attr.getRequest().getSession().setAttribute("accounts", account.getAccounts());
                                attr.getRequest().getSession().setAttribute("admin", account.getAdmin());
//                                attr.getRequest().getSession().setAttribute("role", account.getRole());

                                Set<String> authorities = new HashSet<>();
                                authorities.add(account.getAdmin());

                                final Set<SimpleGrantedAuthority> roles = authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
                                return new UsernamePasswordAuthenticationToken(name, password, roles);
                            } else {
//                            HttpServletRequest request = null;
//                            PortletUtils.addErrorMsg("Invalid username or password ", request);
                                throw new BadCredentialsException("Invalid username or password");
                            }
                        }else {
                            throw new BadCredentialsException("Account does not exist");
                        }
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
        http.authorizeRequests().antMatchers("/login","/DecisionService/ws/EcorecRules/1.0/**").permitAll()
                .and().authorizeRequests().anyRequest().authenticated()
                .and().formLogin().loginPage("/login").defaultSuccessUrl("/",true).permitAll()
                .and().logout()
                .deleteCookies("remove")
                .invalidateHttpSession(true)
                .logoutUrl("/logout")
                .logoutSuccessUrl("/logout-success")
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .and().csrf().disable().cors()
        ;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/img/**","/css/**","/js/**");
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
