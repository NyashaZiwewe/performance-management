package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Service
public class CommonService {

    @Autowired
    HttpSession session;
    @Autowired
    ReportingDateService reportingDateService;

    public String getInitials(String fullName) {
        String words[] = fullName.split(" ");
        StringBuilder builder = new StringBuilder();
        for(String word : words) {
            builder.append(word.charAt(0));
        }
        return builder.toString();
    }

    public Account getLoggedUser(){
        return (Account) session.getAttribute("loggedUser");
    }

    public ReportingDate getActiveReportingDate(HttpServletRequest request){
        try {
            return reportingDateService.getActiveReportingDate();
        }catch (Exception ignored){
            PortletUtils.addErrorMsg("There is no active reporting Date", request);
            return null;
        }
    }
}
