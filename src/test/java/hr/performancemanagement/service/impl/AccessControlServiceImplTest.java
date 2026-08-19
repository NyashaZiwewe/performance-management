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
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @Test
    void updateRoleKeepsExistingPermissionAndAddsOnlyMissingPermissions() {
        Account actor = account(10L, 7L, "NONE");
        actor.setAdmin(PMConstants.IS_ADMIN);
        AccessRole role = role(2L, 7L);
        AccessPermission existingPermission = permission(3L, AccessPermissions.SCORECARD_MODERATE);
        AccessPermission addedPermission = permission(4L, AccessPermissions.PROBATION_CONFIGURE);
        AccessRolePermission existingRolePermission = rolePermission(role, existingPermission);
        AccessControlServiceImpl service = service();

        when(session.getAttribute("loggedUser")).thenReturn(actor);
        when(roleRepository.findAccessRoleByIdAndClientId(2L, 7L)).thenReturn(role);
        when(roleRepository.save(role)).thenReturn(role);
        when(permissionRepository.findAccessPermissionByCode(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            if (AccessPermissions.SCORECARD_MODERATE.equals(code)) {
                return existingPermission;
            }
            if (AccessPermissions.PROBATION_CONFIGURE.equals(code)) {
                return addedPermission;
            }
            return permission(99L, code);
        });
        when(rolePermissionRepository.findAccessRolePermissionsByAccessRoleOrderByPermission_ModuleAscPermission_NameAsc(role))
                .thenReturn(Collections.singletonList(existingRolePermission));

        AccessRole saved = service.saveRole(2L, "HR Role", "Updated role",
                Arrays.asList(AccessPermissions.SCORECARD_MODERATE,
                        AccessPermissions.SCORECARD_MODERATE.toLowerCase(),
                        AccessPermissions.PROBATION_CONFIGURE));

        assertSame(role, saved);
        verify(rolePermissionRepository, never()).deleteAccessRolePermissionsByAccessRole(role);
        verify(rolePermissionRepository, never()).delete(existingRolePermission);
        verify(rolePermissionRepository, times(1)).save(argThat(rolePermission ->
                rolePermission.getAccessRole() == role
                        && rolePermission.getPermission() == addedPermission));
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

    private AccessPermission permission(long id, String code) {
        AccessPermission permission = new AccessPermission();
        permission.setId(id);
        permission.setCode(code);
        permission.setName(code);
        permission.setModule("Test");
        permission.setStatus(PMConstants.STATUS_ACTIVE);
        return permission;
    }

    private AccessRolePermission rolePermission(AccessRole role, AccessPermission permission) {
        AccessRolePermission rolePermission = new AccessRolePermission();
        rolePermission.setAccessRole(role);
        rolePermission.setPermission(permission);
        return rolePermission;
    }

    private Department department(long id, long clientId) {
        Department department = new Department();
        department.setId(id);
        department.setClientId(clientId);
        return department;
    }
}
