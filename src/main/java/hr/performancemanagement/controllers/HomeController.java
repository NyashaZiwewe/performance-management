package hr.performancemanagement.controllers;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.wrappers.LoginWrapper;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

import static hr.performancemanagement.utils.PortletUtils.PortletUtils.ERROR_MSGS;

@Controller
@RequestMapping("/")
public class HomeController {

    @ModelAttribute("loginWrapper")
    public LoginWrapper getLoginWrapper(){
        return new LoginWrapper();
    }

    @RequestMapping
    public ModelAndView goToHome(HttpServletRequest request ){
        ModelAndView modelAndView =  new ModelAndView("index");
        modelAndView.addObject("pageDomain", "Home");
        modelAndView.addObject("pageName", "Home");
        modelAndView.addObject("pageTitle", "Home");
        PortletUtils.addMessagesToPage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/login")
    public ModelAndView goToLoginPage(HttpServletRequest request ){
        ModelAndView modelAndView =  new ModelAndView("login");
        HttpSession session = request.getSession(false);
        if (session != null) {
            AuthenticationException ex = (AuthenticationException) session
                    .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            if (ex != null) {
                PortletUtils.addErrorMsg(ex.getMessage(), request);
            }
        }
        addErrorMessagesToLoginPage(modelAndView, request);
        return modelAndView;
    }

    private void addErrorMessagesToLoginPage(ModelAndView modelAndView, HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<String> errorMsgs = (List<String>) session.getAttribute(ERROR_MSGS);
        if (errorMsgs != null) {
            modelAndView.addObject(ERROR_MSGS, errorMsgs);
        }
        session.removeAttribute(ERROR_MSGS);
    }

}
