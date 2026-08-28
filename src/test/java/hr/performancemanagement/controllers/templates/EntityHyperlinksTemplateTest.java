package hr.performancemanagement.controllers.templates;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityHyperlinksTemplateTest {

    @Test
    void scorecardListLinksOwnersPeriodsAndModels() throws Exception {
        Document document = parse("src/main/resources/templates/scorecard/viewScorecards.html");

        assertEntityLink(document, "/accounts/view-account/{id}", "scorecard.owner.fullName");
        assertEntityLink(document, "/reporting-periods/reporting-dates/{id}", "scorecard.reportingPeriod.startDate");
        assertEntityLink(document, "/scorecard-models", "scorecard.scorecardModel.name");
    }

    @Test
    void accountListLinksEmployeesSupervisorsDepartmentsAndDivisions() throws Exception {
        Document document = parse("src/main/resources/templates/account/viewAccounts.html");

        assertEntityLink(document, "/accounts/view-account/{id}", "account.fullName");
        assertEntityLink(document, "/accounts/view-account/{id}", "account.supervisor.fullName");
        assertEntityLink(document, "/departments", "account.department.name");
        assertEntityLink(document, "/divisions", "account.division.name");
    }

    @Test
    void reportingAndPlanningListsLinkTheirRelatedEntities() throws Exception {
        assertEntityLink(parse("src/main/resources/templates/reporting-period/viewReportingDates.html"),
                "/reporting-periods/reporting-dates/{id}/activity-periods", "reportingDate.endDate");
        assertEntityLink(parse("src/main/resources/templates/reporting-period/viewReportingPeriods.html"),
                "/reporting-periods/reporting-dates/{id}", "period.startDate");
        assertEntityLink(parse("src/main/resources/templates/action-plan/viewActionPlans.html"),
                "/action-plans/view-plan/{id}", "plan.name");
        assertEntityLink(parse("src/main/resources/templates/action-plan/viewActionPlans2.html"),
                "/action-plans/view-plan/{id}", "plan.name");
        assertEntityLink(parse("src/main/resources/templates/performance-improvement-plan/fragments/pipBoardFragments.html"),
                "/performance-improvement-plans/view-plan/{id}", "plan.agreedAction");
    }

    @Test
    void entityLinksUseTextLikeStyling() throws Exception {
        String customCss = new String(Files.readAllBytes(Paths.get("src/main/resources/static/css/custom.css")),
                StandardCharsets.UTF_8);

        assertTrue(customCss.contains("a.entity-link {"));
        assertTrue(customCss.contains("a.entity-link:hover,"));
        assertTrue(customCss.contains("a.entity-link:focus {"));
        assertFalse(customCss.contains("a[href*=\"/accounts/view-account/\"]"));
        assertFalse(customCss.contains("a[href*=\"/reporting-periods/reporting-dates/\"]"));
        assertTrue(customCss.contains("color: inherit !important;"));
        assertTrue(customCss.contains("text-decoration: none;"));
    }

    private Document parse(String path) throws Exception {
        return Jsoup.parse(new File(path), "UTF-8");
    }

    private void assertEntityLink(Document document, String hrefFragment, String textFragment) {
        boolean found = false;
        for (Element link : document.select("a")) {
            if (link.attr("th:href").contains(hrefFragment)
                    && link.attr("th:text").contains(textFragment)
                    && link.hasClass("entity-link")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected link with th:href containing " + hrefFragment
                + ", th:text containing " + textFragment
                + ", and class entity-link");
    }
}
