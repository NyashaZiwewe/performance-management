package hr.performancemanagement.entities;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceImprovementPlanTest {

    @Test
    void notApplicableDefaultsToFalse() {
        PerformanceImprovementPlan plan = new PerformanceImprovementPlan();

        assertFalse(plan.isNotApplicable());
        assertEquals(Boolean.FALSE, plan.getNotApplicable());
    }

    @Test
    void nullNotApplicableIsTreatedAsFalse() {
        PerformanceImprovementPlan plan = new PerformanceImprovementPlan();

        ReflectionTestUtils.setField(plan, "notApplicable", null);

        assertFalse(plan.isNotApplicable());
        assertEquals(Boolean.FALSE, plan.getNotApplicable());
    }

    @Test
    void trueNotApplicableIsPreserved() {
        PerformanceImprovementPlan plan = new PerformanceImprovementPlan();

        plan.setNotApplicable(Boolean.TRUE);

        assertTrue(plan.isNotApplicable());
        assertEquals(Boolean.TRUE, plan.getNotApplicable());
    }
}
