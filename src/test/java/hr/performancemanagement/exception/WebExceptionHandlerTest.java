package hr.performancemanagement.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;

import static hr.performancemanagement.utils.PortletUtils.PortletUtils.ERROR_MSGS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WebExceptionHandlerTest {

    @Test
    void missingRequestParameterReturnsToReferrerWithFriendlyMessage() {
        WebExceptionHandler handler = new WebExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/performance-review/view-individual-trends"
        );
        request.addHeader(
                "Referer",
                "http://localhost/performance-review/view-individual-trends-select-year"
        );

        String redirect = handler.handleRequestBinding(
                new MissingServletRequestParameterException("employeeId", "Long"),
                request
        );

        assertEquals("redirect:/performance-review/view-individual-trends-select-year", redirect);
        List<String> messages = (List<String>) request.getSession().getAttribute(ERROR_MSGS);
        assertEquals("Provide a value for employee before continuing.", messages.get(0));
    }
}
