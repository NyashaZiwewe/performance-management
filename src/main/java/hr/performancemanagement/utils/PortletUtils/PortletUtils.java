package hr.performancemanagement.utils.PortletUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

public class PortletUtils {
    public static final String ERROR_MSGS = "errorMsgs";
    public static final String INFO_MSGS = "infoMsgs";



    public static String getUsername(HttpServletRequest request) {
        return request.getUserPrincipal().getName().toLowerCase();
    }

    public static void addErrorMsg(String msg, HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<String> errorMsgs = (List<String>) session.getAttribute(ERROR_MSGS);
        if (errorMsgs == null) {
            errorMsgs = new ArrayList<>();
            session.setAttribute(ERROR_MSGS, errorMsgs);
        }
        errorMsgs.add(msg);
    }

    public static void addInfoMsg(String msg, HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<String> infoMsgs = (List<String>) session.getAttribute(INFO_MSGS);
        if (infoMsgs == null) {
            infoMsgs = new ArrayList<>();
            session.setAttribute(INFO_MSGS, infoMsgs);
        }
        infoMsgs.add(msg);
    }


    public static String getMessages(HttpServletRequest request) {
        StringBuilder buffer = new StringBuilder();
        HttpSession session = request.getSession();
        List<String> infoMsgs = (List<String>) session.getAttribute(INFO_MSGS);
        List<String> errorMsgs = (List<String>) session.getAttribute(ERROR_MSGS);

        if (infoMsgs != null) {

            for (String msg : infoMsgs) {
                buffer.append(msg + "\n");
            }
        }

        if (errorMsgs != null) {

            for (String msg : errorMsgs) {
                buffer.append(msg + "\n");
            }

        }
        session.removeAttribute(ERROR_MSGS);
        session.removeAttribute(INFO_MSGS);
        return buffer.toString();
    }


    public static void addMessagesToPage(ModelAndView modelAndView, HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<String> infoMsgs = (List<String>) session.getAttribute(INFO_MSGS);
        List<String> errorMsgs = (List<String>) session.getAttribute(ERROR_MSGS);
//        Set<String> notifications = CacheService.getNotificationMsgs(getUsername(request));
//        if (notifications != null) {
//            if (infoMsgs == null) {
//                infoMsgs = new ArrayList<>();
//            }
//            infoMsgs.addAll(notifications);
//        }
        if (infoMsgs != null) {
            modelAndView.addObject(INFO_MSGS, infoMsgs);
        }

        if (errorMsgs != null) {
            modelAndView.addObject(ERROR_MSGS, errorMsgs);
        }
        session.removeAttribute(ERROR_MSGS);
        session.removeAttribute(INFO_MSGS);
    }


}
