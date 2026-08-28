package hr.performancemanagement.controllers.reportingperiod;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportingDateActivityPeriodsTemplateTest {

    @Test
    void addActivityCutoffUsesModalInsteadOfInlinePageForm() throws Exception {
        File template = new File("src/main/resources/templates/reporting-period/manageReportingDateActivityPeriods.html");
        Document document = Jsoup.parse(template, "UTF-8");

        assertFalse(document.select("button[data-target=#addActivityPeriodModal]").isEmpty());

        Element addModal = document.getElementById("addActivityPeriodModal");
        assertNotNull(addModal);
        assertTrue(addModal.select("form").attr("th:action").contains("save-activity-period"));
        assertEquals("0", addModal.select("input[name=id]").attr("value"));
        assertFalse(addModal.select("select[name=activityType]").isEmpty());
        assertFalse(addModal.select("input[name=startDate]").isEmpty());
        assertFalse(addModal.select("input[name=endDate]").isEmpty());

        assertTrue(document.select(".ibox-content > .m-t-md").isEmpty());
    }
}
