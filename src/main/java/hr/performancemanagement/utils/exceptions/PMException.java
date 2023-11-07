package hr.performancemanagement.utils.exceptions;

import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import lombok.experimental.StandardException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebFault;
import java.util.Enumeration;

@WebFault(name="PMException")
@StandardException
public class PMException extends RuntimeException {

	public PMException() {
	}

	public PMException(String message) {
        super(message);
		ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
		HttpServletRequest request = attr.getRequest();
		removeSessionAttributesAfterError(request);
		PortletUtils.addErrorMsg(message, request);

    }

	private void removeSessionAttributesAfterError(HttpServletRequest request) {
		Enumeration<String> attributeNames = request.getSession().getAttributeNames();
		while (attributeNames.hasMoreElements()){
			String attribute = attributeNames.nextElement();
			if(!attribute.equals("SPRING_SECURITY_CONTEXT")){
				request.getSession().removeAttribute(attribute);
			}
		}
	}
	
	
	
	

}
