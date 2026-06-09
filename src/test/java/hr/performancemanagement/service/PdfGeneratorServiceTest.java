package hr.performancemanagement.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfGeneratorServiceTest {

    @Test
    void redesignedPerformanceReportRendersAsPdf() throws Exception {
        PdfGeneratorService service = new PdfGeneratorService();
        ReflectionTestUtils.setField(service, "templateEngine", templateEngine());

        Context context = reportContext();
        byte[] pdf = service.generatePdfFromTemplate("reports/report", context, false);

        assertTrue(pdf.length > 5_000);
        assertArrayEquals("%PDF".getBytes(), Arrays.copyOf(pdf, 4));
    }

    private TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private Context reportContext() {
        Context context = new Context();
        Map<String, Object> department = mapOf("name", "Corporate Services");
        Map<String, Object> owner = mapOf(
                "fullName", "Test Employee",
                "position", "Performance Analyst",
                "department", department
        );
        Map<String, Object> reportingPeriod = mapOf(
                "startDate", "2026-01-01",
                "endDate", "2026-12-31"
        );

        context.setVariable("reportLogoPath", "img/zimlogo.png");
        context.setVariable("owner", owner);
        context.setVariable("reportingPeriod", reportingPeriod);
        context.setVariable("latestWeightedScore", 76.5);
        context.setVariable("previousWeightedScore", 71.0);
        context.setVariable("scoreMovement", 5.5);
        context.setVariable("scoreMovementLabel", "Improving");
        context.setVariable("performanceBand", "Strong Performance");
        context.setVariable("decisionRiskLevel", "Low");
        context.setVariable("decisionRecommendation", "Continue current performance plan");
        context.setVariable("targetsWithScore", 8);
        context.setVariable("totalTargets", 10);
        context.setVariable("scoreCoveragePercent", 80.0);
        context.setVariable("targetsWithEvidence", 7);
        context.setVariable("evidenceCoveragePercent", 70.0);
        context.setVariable("outstandingInterventionCount", 1);
        context.setVariable("probationStatus", "Not applicable");
        context.setVariable("probationRecommendation", "No action required");
        context.setVariable("scoreTrendLabels", Arrays.asList("Q1", "Q2", "Q3"));
        context.setVariable("scoreTrendScores", Arrays.asList(68.0, 72.0, 76.5));
        context.setVariable("topRiskItems", Collections.singletonList("One target requires evidence"));
        context.setVariable("topStrengthItems", Collections.singletonList("Consistent upward score movement"));
        context.setVariable("lowPerformingTargets", Collections.emptyList());
        context.setVariable("missingEvidenceTargets", Collections.emptyList());
        context.setVariable("highVarianceTargets", Collections.emptyList());
        context.setVariable("probationPeriod", "N/A");
        context.setVariable("probationCurrentStep", "N/A");
        context.setVariable("probationEndDate", "N/A");
        context.setVariable("probationKpiCount", 0);
        context.setVariable("probationFlaggedKpiCount", 0);
        context.setVariable("probationAverageProgress", 0.0);
        context.setVariable("probationDecisionRequired", false);
        context.setVariable("finalReportingDate", null);
        context.setVariable("finalOverallScore", null);
        context.setVariable("insightReportingDateLabel", "2026-12-31");
        context.setVariable("averageEmployeeScore", 3.7);
        context.setVariable("averageManagerScore", 3.8);
        context.setVariable("averageAgreedScore", 3.8);
        context.setVariable("averageModeratedScore", 3.83);
        context.setVariable("alignmentGapEmployeeManager", 0.1);
        context.setVariable("alignmentGapManagerAgreed", 0.0);
        context.setVariable("alignmentGapAgreedModerated", 0.03);
        context.setVariable("riskRedTargets", 1);
        context.setVariable("riskAmberTargets", 2);
        context.setVariable("riskGreenTargets", 7);
        context.setVariable("pipOpenCount", 0);
        context.setVariable("pipInProgressCount", 1);
        context.setVariable("pipClosedCount", 0);
        context.setVariable("actionOpenCount", 0);
        context.setVariable("actionInProgressCount", 0);
        context.setVariable("actionClosedCount", 1);
        context.setVariable("pips", Collections.emptyList());
        context.setVariable("actionPlans", Collections.emptyList());
        context.setVariable("overallComments", Collections.emptyList());
        return context;
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> values = new HashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put((String) entries[index], entries[index + 1]);
        }
        return values;
    }
}
