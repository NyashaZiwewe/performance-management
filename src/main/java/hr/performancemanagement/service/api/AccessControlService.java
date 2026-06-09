package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AccessControlService {

    String SCOPE_CLIENT = "CLIENT";
    String SCOPE_DIVISION = "DIVISION";
    String SCOPE_DEPARTMENT = "DEPARTMENT";

    boolean hasPermission(String permissionCode);
    boolean hasPermission(Account account, String permissionCode);
    boolean hasPermission(Account account, String permissionCode, Account subject);
    boolean hasPermissionForAnyScope(Account account, String permissionCode);
    boolean canManageAccess();
    String accessSummary();
    Set<String> effectivePermissions(Account account);
    List<Account> listAccountsWithPermission(long clientId, String permissionCode, Account subject);

    void ensureDefaultConfiguration(long clientId);
    List<AccessPermission> listPermissions();
    List<AccessRole> listRoles();
    Map<Long, List<AccessRolePermission>> listRolePermissions();
    AccessRole saveRole(long roleId, String name, String description, List<String> permissionCodes);
    void deactivateRole(long roleId, String reason);

    List<AccountAccessRole> listAssignments();
    List<AccessAssignmentAudit> listRecentAudit();
    AccountAccessRole assignRole(long accountId, long roleId, String scopeType, Long divisionId,
                                 Long departmentId, LocalDate effectiveFrom, LocalDate expiresOn, String reason);
    void revokeAssignment(long assignmentId, String reason);
}
