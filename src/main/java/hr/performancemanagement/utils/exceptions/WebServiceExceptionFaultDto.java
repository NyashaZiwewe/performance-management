package hr.performancemanagement.utils.exceptions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PMException", propOrder = {
    "message"
})
public class WebServiceExceptionFaultDto {

	protected String message;

    public WebServiceExceptionFaultDto(String message) {
		super();
		this.message = message;
	}

	public WebServiceExceptionFaultDto() {
		super();
	}

	public String getMessage() {
        return message;
    }

    public void setMessage(String value) {
        this.message = value;
    }
}
