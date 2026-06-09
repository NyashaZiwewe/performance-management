package hr.performancemanagement.controllers.probation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProbationKpiTemplateTest {

    @Test
    void hiddenTemplateControlsDoNotBlockKpiFormValidation() throws Exception {
        File template = new File("src/main/resources/templates/scorecard/captureKpisProbation.html");
        Document document = Jsoup.parse(template, "UTF-8");
        Element templateRow = document.getElementById("kpiTemplateRow");

        assertFalse(templateRow.select("input, button").isEmpty());
        assertTrue(templateRow.select("input, button").stream().allMatch(control -> control.hasAttr("disabled")));
        assertTrue(document.html().contains("controls[i].disabled = false;"));
    }

    @Test
    void savedKpisDisplayAsTextUntilEditIsClicked() throws Exception {
        File template = new File("src/main/resources/templates/scorecard/captureKpisProbation.html");
        Document document = Jsoup.parse(template, "UTF-8");
        Element savedRow = document.select("tr").stream()
                .filter(row -> row.hasAttr("th:each"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Saved KPI row not found"));

        assertFalse(savedRow.select(".js-kpi-view").isEmpty());
        assertTrue(savedRow.select(".js-kpi-edit-cell").stream()
                .allMatch(control -> control.hasAttr("disabled") && control.attr("style").contains("display:none")));
        assertTrue(savedRow.select(".js-kpi-edit-control").stream().allMatch(control -> control.hasAttr("disabled")));
        assertFalse(savedRow.select(".js-edit-kpi").isEmpty());
        assertTrue(document.html().contains("$(document).on('click', '.js-edit-kpi'"));
        assertTrue(document.html().contains("row.find('.js-kpi-edit-control').prop('disabled', false);"));
    }
}
