package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ProbationDimensionTemplate;
import hr.performancemanagement.repository.ProbationAssessmentDimensionRepository;
import hr.performancemanagement.repository.ProbationDimensionTemplateRepository;
import hr.performancemanagement.repository.ProbationWorkflowStepRepository;
import hr.performancemanagement.service.api.AccountService;
import hr.performancemanagement.service.api.AccessControlService;
import hr.performancemanagement.utils.constants.AccessPermissions;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.utils.constants.PMConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProbationConfigServiceImplTest {

    @Mock
    private ProbationDimensionTemplateRepository dimensionTemplateRepository;
    @Mock
    private ProbationAssessmentDimensionRepository assessmentDimensionRepository;
    @Mock
    private ProbationWorkflowStepRepository workflowStepRepository;
    @Mock
    private CommonService commonService;
    @Mock
    private AccountService accountService;
    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private ProbationConfigServiceImpl service;

    @Test
    void ordinaryUserCannotChangeProbationConfiguration() {
        assertNull(service.saveDimensionTemplate(new ProbationDimensionTemplate()));
        verify(dimensionTemplateRepository, never()).save(any(ProbationDimensionTemplate.class));
    }

    @Test
    void newConfigurationIsAlwaysOwnedByLoggedUsersTenant() {
        Account hr = account(10L, 7L, "HR");
        ProbationDimensionTemplate template = new ProbationDimensionTemplate();
        template.setClientId(99L);
        template.setTitle("Communication");
        when(commonService.getLoggedUser()).thenReturn(hr);
        when(accessControlService.hasPermission(AccessPermissions.PROBATION_CONFIGURE)).thenReturn(true);
        when(dimensionTemplateRepository.save(template)).thenReturn(template);

        ProbationDimensionTemplate saved = service.saveDimensionTemplate(template);

        assertEquals(7L, saved.getClientId());
        assertEquals(PMConstants.STATUS_ACTIVE, saved.getStatus());
    }

    private Account account(long id, long clientId, String role) {
        Account account = new Account();
        account.setId(id);
        account.setClientId(clientId);
        account.setRole(role);
        account.setAdmin("NO");
        account.setSpecial("NO");
        return account;
    }
}
