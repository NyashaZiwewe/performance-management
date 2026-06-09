package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.*;
import hr.performancemanagement.service.api.AccessControlService;
import hr.performancemanagement.utils.constants.AccessPermissions;
import hr.performancemanagement.utils.constants.PMConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpSession;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceImplTest {

    @Mock private AccessPermissionRepository permissionRepository;
    @Mock private AccessRoleRepository roleRepository;
    @Mock private AccessRolePermissionRepository rolePermissionRepository;
    @Mock private AccountAccessRoleRepository assignmentRepository;
    @Mock private AccessAssignmentAuditRepository auditRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private DivisionRepository divisionRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private HttpSession session;

    @Test
    void legacyModeratorRetainsScorecardRightsBeforeMigration() {
        Account moderator = account(10L, 7L, PMConstants.MODERATOR);
        AccessControlServiceImpl service = service();
        when(assignmentRepository.findAccountAccessRolesByAccountAndClientIdOrderByIdDesc(moderator, 7L))
                .thenReturn(Collections.emptyList());

        assertTrue(service.hasPermission(moderator, AccessPermissions.SCORECARD_MODERATE));
        assertFalse(service.hasPermission(moderator, AccessPermissions.PROBATION_APPROVE_FINAL_ASSESSMENT));
    }

    @Test
    void inactiveAssignmentPreventsLegacyRoleFromRestoringRevokedPermission() {
        Account moderator = account(10L, 7L, PMConstants.MODERATOR);
        AccountAccessRole revoked = assignment(moderator, role(2L, 7L), AccessControlService.SCOPE_CLIENT);
        revoked.setStatus(PMConstants.STATUS_IN_ACTIVE);
        AccessControlServiceImpl service = service();
        when(assignmentRepository.findAccountAccessRolesByAccountAndClientIdOrderByIdDesc(moderator, 7L))
                .thenReturn(Collections.singletonList(revoked));

        assertFalse(service.hasPermission(moderator, AccessPermissions.SCORECARD_MODERATE));
    }

    @Test
    void departmentScopedPermissionOnlyAppliesToEmployeesInThatDepartment() {
        Account approver = account(10L, 7L, "NONE");
        Department finance = department(20L, 7L);
        Department operations = department(21L, 7L);
        Account inScope = account(30L, 7L, "NONE");
        inScope.setDepartment(finance);
        Account outOfScope = account(31L, 7L, "NONE");
        outOfScope.setDepartment(operations);
        AccessRole role = role(2L, 7L);
        AccountAccessRole assignment = assignment(approver, role, AccessControlService.SCOPE_DEPARTMENT);
        assignment.setDepartment(finance);
        AccessControlServiceImpl service = service();
        when(assignmentRepository.findAccountAccessRolesByAccountAndClientIdOrderByIdDesc(approver, 7L))
                .thenReturn(Collections.singletonList(assignment));
        when(rolePermissionRepository.existsAccessRolePermissionByAccessRoleAndPermission_Code(
                role, AccessPermissions.PROBATION_APPROVE_FINAL_ASSESSMENT)).thenReturn(true);

        assertTrue(service.hasPermission(approver, AccessPermissions.PROBATION_APPROVE_FINAL_ASSESSMENT, inScope));
        assertFalse(service.hasPermission(approver, AccessPermissions.PROBATION_APPROVE_FINAL_ASSESSMENT, outOfScope));
    }

    private AccessControlServiceImpl service() {
        return new AccessControlServiceImpl(permissionRepository, roleRepository, rolePermissionRepository,
                assignmentRepository, auditRepository, accountRepository, divisionRepository, departmentRepository, session);
    }

    private Account account(long id, long clientId, String role) {
        Account account = new Account();
        account.setId(id);
        account.setClientId(clientId);
        account.setRole(role);
        account.setAdmin("NO");
        account.setSpecial("NO");
        account.setStatus(PMConstants.STATUS_ACTIVE);
        return account;
    }

    private AccessRole role(long id, long clientId) {
        AccessRole role = new AccessRole();
        role.setId(id);
        role.setClientId(clientId);
        role.setStatus(PMConstants.STATUS_ACTIVE);
        return role;
    }

    private AccountAccessRole assignment(Account account, AccessRole role, String scopeType) {
        AccountAccessRole assignment = new AccountAccessRole();
        assignment.setAccount(account);
        assignment.setAccessRole(role);
        assignment.setClientId(account.getClientId());
        assignment.setScopeType(scopeType);
        assignment.setStatus(PMConstants.STATUS_ACTIVE);
        return assignment;
    }

    private Department department(long id, long clientId) {
        Department department = new Department();
        department.setId(id);
        department.setClientId(clientId);
        return department;
    }
}
