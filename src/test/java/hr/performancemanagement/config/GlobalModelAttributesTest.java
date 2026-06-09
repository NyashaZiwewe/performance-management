package hr.performancemanagement.config;

import hr.performancemanagement.service.api.CommonService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalModelAttributesTest {

    @Test
    void specialRightsUserCanManageAccounts() {
        CommonService commonService = mock(CommonService.class);
        GlobalModelAttributes attributes = new GlobalModelAttributes();
        ReflectionTestUtils.setField(attributes, "commonService", commonService);
        when(commonService.isAdmin()).thenReturn(false);
        when(commonService.hasSpecialRights()).thenReturn(true);

        assertTrue(attributes.canManageAccounts());
    }

    @Test
    void regularUserCannotManageAccounts() {
        CommonService commonService = mock(CommonService.class);
        GlobalModelAttributes attributes = new GlobalModelAttributes();
        ReflectionTestUtils.setField(attributes, "commonService", commonService);
        when(commonService.isAdmin()).thenReturn(false);
        when(commonService.hasSpecialRights()).thenReturn(false);

        assertFalse(attributes.canManageAccounts());
    }
}
