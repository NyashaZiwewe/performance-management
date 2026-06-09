package hr.performancemanagement.utils.constants;

import java.util.Arrays;
import java.util.List;

public final class AccessPermissions {

    public static final String SCORECARD_APPROVE_TARGETS_HR = "SCORECARD_APPROVE_TARGETS_HR";
    public static final String SCORECARD_APPROVE_AGREED_SCORES_HR = "SCORECARD_APPROVE_AGREED_SCORES_HR";
    public static final String SCORECARD_MODERATE = "SCORECARD_MODERATE";
    public static final String SCORECARD_CLOSE = "SCORECARD_CLOSE";
    public static final String PROBATION_CREATE_CONTRACT = "PROBATION_CREATE_CONTRACT";
    public static final String PROBATION_APPROVE_KPI_CONTRACT = "PROBATION_APPROVE_KPI_CONTRACT";
    public static final String PROBATION_APPROVE_FINAL_ASSESSMENT = "PROBATION_APPROVE_FINAL_ASSESSMENT";
    public static final String PROBATION_CONFIGURE = "PROBATION_CONFIGURE";
    public static final String ACCESS_MANAGE = "ACCESS_MANAGE";

    private AccessPermissions() {
    }

    public static List<Definition> catalogue() {
        return Arrays.asList(
                new Definition(SCORECARD_APPROVE_TARGETS_HR, "Approve scorecard targets", "Scorecards",
                        "Approve or reject scorecard targets at the HR approval stage."),
                new Definition(SCORECARD_APPROVE_AGREED_SCORES_HR, "Approve agreed scorecard scores", "Scorecards",
                        "Approve agreed scores before scorecard moderation."),
                new Definition(SCORECARD_MODERATE, "Moderate scorecards", "Scorecards",
                        "Capture and submit moderated scorecard results."),
                new Definition(SCORECARD_CLOSE, "Close scorecards", "Scorecards",
                        "Close a moderated scorecard."),
                new Definition(PROBATION_CREATE_CONTRACT, "Create probation contracts", "Probation",
                        "Create probation assessment contracts."),
                new Definition(PROBATION_APPROVE_KPI_CONTRACT, "Approve probation KPI contracts", "Probation",
                        "Approve or reject probation KPI contracts at the HR stage."),
                new Definition(PROBATION_APPROVE_FINAL_ASSESSMENT, "Approve final probation assessments", "Probation",
                        "Approve or reject final probation assessment results."),
                new Definition(PROBATION_CONFIGURE, "Configure probation assessments", "Probation",
                        "Manage probation dimensions and workflow configuration."),
                new Definition(ACCESS_MANAGE, "Manage access roles", "Administration",
                        "Create access roles and grant or revoke role assignments.")
        );
    }

    public static class Definition {
        private final String code;
        private final String name;
        private final String module;
        private final String description;

        public Definition(String code, String name, String module, String description) {
            this.code = code;
            this.name = name;
            this.module = module;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getModule() {
            return module;
        }

        public String getDescription() {
            return description;
        }
    }
}
