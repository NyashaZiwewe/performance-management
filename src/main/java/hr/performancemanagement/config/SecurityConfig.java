package hr.performancemanagement.config;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.service.AccountService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Pages;
import hr.performancemanagement.utils.exceptions.PMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.sound.sampled.Port;
import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    AccountService accountService;
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(
                new AuthenticationProvider() {
                    @Override
                    public Authentication authenticate(Authentication authentication) {

                        MessageDigest md = null;

                        try {
                            md = MessageDigest.getInstance("MD5");
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);
                        }

                        final String name = authentication.getName();
                        final String pass = authentication.getCredentials().toString();

                        md.update(pass.getBytes());
                        byte[] digest = md.digest();
                        String password = DatatypeConverter.printHexBinary(digest).toLowerCase();
                        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                        Account account = accountService.findAccountByEmail(name);

                        if(account !=null) {

                            if (name.equals(account.getEmail()) && password.equals(account.getPassword())) {

                                attr.getRequest().getSession().setAttribute("loggedUser", account);

                                Set<String> authorities = new HashSet<>();
                                authorities.add(account.getAdmin());

                                final Set<SimpleGrantedAuthority> roles = authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
                                return new UsernamePasswordAuthenticationToken(name, password, roles);
                            } else {

                                throw new PMException("Invalid username or Password");
                            }
                        }else {

                                throw new PMException("Account does not exist");

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
        http.authorizeRequests().antMatchers("/login**","/logout","/reset-password","/save-password","/change-password/**","/set-reset").permitAll()
                .and().authorizeRequests().anyRequest().authenticated()
                .and().formLogin().loginPage("/login")
                .defaultSuccessUrl("/",false)
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
        SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler();
        handler.setUseReferer(true);
        return handler;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/img/**","/css/**","/js/**","/fonts/**","/font-awesome.css/**","/font-awesome/**");
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
