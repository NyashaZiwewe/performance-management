package hr.performancemanagement.controllers.probation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProbationEvaluationTemplateTest {

    @Test
    void evaluationUsesBulkDraftSaveAndBlocksSubmissionWhileRowsAreEditable() throws Exception {
        File template = new File("src/main/resources/templates/probation-assessment/evaluateProbation.html");
        Document document = Jsoup.parse(template, "UTF-8");

        assertFalse(document.select("#saveIncumbentEvaluationDraftBtn").isEmpty());
        assertTrue(document.select("#saveIncumbentEvaluationDraftBtn").attr("th:formaction")
                .contains("save-incumbent-evaluation-draft"));
        assertFalse(document.select("#saveIncumbentEvaluationDraftBtn").hasAttr("formnovalidate"));
        assertTrue(document.select(".js-row-save").isEmpty());
        assertFalse(document.select(".js-evaluation-control").isEmpty());
        assertTrue(document.html().contains("$('#submitIncumbentEvaluationBtn').prop('disabled', hasUnsavedRows);"));
    }

    @Test
    void supervisorReviewUsesBulkDraftSaveAndReturnReasonModal() throws Exception {
        File template = new File("src/main/resources/templates/probation-assessment/evaluateProbation.html");
        Document document = Jsoup.parse(template, "UTF-8");

        assertFalse(document.select("#saveSupervisorEvaluationDraftBtn").isEmpty());
        assertTrue(document.select("#saveSupervisorEvaluationDraftBtn").attr("th:formaction")
                .contains("save-supervisor-evaluation-draft"));
        assertFalse(document.select(".js-supervisor-view-cell").isEmpty());
        assertFalse(document.select(".js-supervisor-row-edit").isEmpty());
        assertTrue(document.html().contains("$('#submitSupervisorEvaluationBtn').prop('disabled', hasUnsavedRows);"));

        assertFalse(document.select("button[data-target=#returnEvaluationModal]").isEmpty());
        assertFalse(document.select("#returnEvaluationModal textarea[name=remarks][required]").isEmpty());
        assertEquals(1, document.select("textarea[name=remarks]").size());
        assertTrue(document.select("#returnEvaluationModal form").attr("th:action")
                .contains("reject-supervisor-review"));
    }
}
