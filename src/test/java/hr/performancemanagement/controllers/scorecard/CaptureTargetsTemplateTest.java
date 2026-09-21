package hr.performancemanagement.controllers.scorecard;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CaptureTargetsTemplateTest {

    @Test
    void submitButtonUsesCapturePermissionInsteadOfApprovalStatusOnly() throws Exception {
        File template = new File("src/main/resources/templates/scorecard/captureTargets.html");
        Document document = Jsoup.parse(template, "UTF-8");

        Element submitButton = document.selectFirst("button.submit-scorecard-btn");

        assertFalse(document.select("button.submit-scorecard-btn").isEmpty());
        assertEquals("${canSubmitTargets}", submitButton.attr("th:if"));
    }
}
