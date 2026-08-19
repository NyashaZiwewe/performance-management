package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.*;
import hr.performancemanagement.service.api.AccessControlService;
import hr.performancemanagement.utils.constants.AccessPermissions;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.*;

@Service
public class AccessControlServiceImpl implements AccessControlService {

    private final AccessPermissionRepository permissionRepository;
    private final AccessRoleRepository roleRepository;
    private final AccessRolePermissionRepository rolePermissionRepository;
    private final AccountAccessRoleRepository assignmentRepository;
    private final AccessAssignmentAuditRepository auditRepository;
    private final AccountRepository accountRepository;
    private final DivisionRepository divisionRepository;
    private final DepartmentRepository departmentRepository;
    private final HttpSession session;

    public AccessControlServiceImpl(AccessPermissionRepository permissionRepository,
                                    AccessRoleRepository roleRepository,
                                    AccessRolePermissionRepository rolePermissionRepository,
                                    AccountAccessRoleRepository assignmentRepository,
                                    AccessAssignmentAuditRepository auditRepository,
                                    AccountRepository accountRepository,
                                    DivisionRepository divisionRepository,
                                    DepartmentRepository departmentRepository,
                                    HttpSession session) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.auditRepository = auditRepository;
        this.accountRepository = accountRepository;
        this.divisionRepository = divisionRepository;
        this.departmentRepository = departmentRepository;
        this.session = session;
    }

    @Override
    public boolean hasPermission(String permissionCode) {
        return hasPermission(loggedUser(), permissionCode, null);
    }

    @Override
    public boolean hasPermission(Account account, String permissionCode) {
        return hasPermission(account, permissionCode, null);
    }

    @Override
    public boolean hasPermissionForAnyScope(Account account, String permissionCode) {
        if (account == null || !StringUtils.hasText(permissionCode)) {
            return false;
        }
        if (isAdminOrSpecial(account)) {
            return true;
        }
        List<AccountAccessRole> assignments =
                assignmentRepository.findAccountAccessRolesByAccountAndClientIdOrderByIdDesc(account, account.getClientId());
        if (assignments == null || assignments.isEmpty()) {
            return legacyPermission(account, permissionCode);
        }
        LocalDate today = LocalDate.now();
        for (AccountAccessRole assignment : assignments) {
            if (isEffective(assignment, today)
                    && rolePermissionRepository.existsAccessRolePermissionByAccessRoleAndPermission_Code(
                    assignment.getAccessRole(), normalize(permissionCode))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasPermission(Account account, String permissionCode, Account subject) {
        if (account == null || !StringUtils.hasText(permissionCode)) {
            return false;
        }
        if (isAdminOrSpecial(account)) {
            return true;
        }

        List<AccountAccessRole> assignments =
                assignmentRepository.findAccountAccessRolesByAccountAndClientIdOrderByIdDesc(account, account.getClientId());
        if (assignments == null || assignments.isEmpty()) {
            return legacyPermission(account, permissionCode);
        }

        LocalDate today = LocalDate.now();
        for (AccountAccessRole assignment : assignments) {
            if (!isEffective(assignment, today) || !scopeMatches(assignment, subject)) {
                continue;
            }
            if (rolePermissionRepository.existsAccessRolePermissionByAccessRoleAndPermission_Code(
                    assignment.getAccessRole(), permissionCode.trim().toUpperCase(Locale.ENGLISH))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canManageAccess() {
        return hasPermission(AccessPermissions.ACCESS_MANAGE);
    }

    @Override
    public String accessSummary() {
        Account account = loggedUser();
        if (account == null) {
            return "";
        }
        if (PMConstants.IS_ADMIN.equalsIgnoreCase(account.getAdmin())) {
            return "Administrator";
        }
        if (PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(account.getSpecial())) {
            return "Special Rights";
        }
        List<AccountAccessRole> assignments =
                assignmentRepository.findAccountAccessRolesByAccountAndClientIdOrderByIdDesc(account, account.getClientId());
        if (assignments == null || assignments.isEmpty()) {
            if (PMConstants.MODERATOR.equalsIgnoreCase(account.getRole())) {
                return "Legacy HR Moderator";
            }
            if ("HR".equalsIgnoreCase(account.getRole())) {
                return "Legacy HR";
            }
            return StringUtils.hasText(account.getRole()) ? account.getRole() : "No workflow access";
        }
        Set<String> roleNames = new LinkedHashSet<>();
        LocalDate today = LocalDate.now();
        for (AccountAccessRole assignment : assignments) {
            if (isEffective(assignment, today)
                    && assignment.getAccessRole() != null
                    && StringUtils.hasText(assignment.getAccessRole().getName())) {
                roleNames.add(assignment.getAccessRole().getName());
            }
        }
        return roleNames.isEmpty() ? "No workflow access" : String.join(", ", roleNames);
    }

    @Override
    public Set<String> effectivePermissions(Account account) {
        Set<String> permissions = new LinkedHashSet<>();
        if (account == null) {
            return permissions;
        }
        for (AccessPermissions.Definition definition : AccessPermissions.catalogue()) {
            if (hasPermissionForAnyScope(account, definition.getCode())) {
                permissions.add(definition.getCode());
            }
        }
        return permissions;
    }

    @Override
    public List<Account> listAccountsWithPermission(long clientId, String permissionCode, Account subject) {
        List<Account> permitted = new ArrayList<>();
        for (Account account : accountRepository.findAccountsByClient_ClientId(clientId)) {
            if (account != null
                    && PMConstants.STATUS_ACTIVE.equalsIgnoreCase(account.getStatus())
                    && hasPermission(account, permissionCode, subject)) {
                permitted.add(account);
            }
        }
        return permitted;
    }

    @Override
    @Transactional
    public void ensureDefaultConfiguration(long clientId) {
        if (clientId <= 0) {
            return;
        }
        ensurePermissionCatalogue();
        createDefaultRole(clientId, "HR Scorecard Approver",
                "Approves scorecard targets and agreed scores.",
                Arrays.asList(AccessPermissions.SCORECARD_APPROVE_TARGETS_HR,
                        AccessPermissions.SCORECARD_APPROVE_AGREED_SCORES_HR));
        createDefaultRole(clientId, "HR Moderator",
                "Moderates scorecard results.",
                Collections.singletonList(AccessPermissions.SCORECARD_MODERATE));
        createDefaultRole(clientId, "HR Probation Approver",
                "Approves probation KPI contracts and final assessments.",
                Arrays.asList(AccessPermissions.PROBATION_APPROVE_KPI_CONTRACT,
                        AccessPermissions.PROBATION_APPROVE_FINAL_ASSESSMENT));
        createDefaultRole(clientId, "HR Probation Administrator",
                "Creates and configures probation assessments.",
                Arrays.asList(AccessPermissions.PROBATION_CREATE_CONTRACT,
                        AccessPermissions.PROBATION_CONFIGURE));
        createDefaultRole(clientId, "Access Administrator",
                "Creates access roles and grants or revokes assignments.",
                Collections.singletonList(AccessPermissions.ACCESS_MANAGE));
        migrateLegacyWorkflowRoles(clientId);
    }

    @Override
    public List<AccessPermission> listPermissions() {
        return permissionRepository.findAccessPermissionsByStatusOrderByModuleAscNameAsc(PMConstants.STATUS_ACTIVE);
    }

    @Override
    public List<AccessRole> listRoles() {
        Account user = requireLoggedUser();
        return roleRepository.findAccessRolesByClientIdOrderByNameAsc(user.getClientId());
    }

    @Override
    public Map<Long, List<AccessRolePermission>> listRolePermissions() {
        Map<Long, List<AccessRolePermission>> permissionsByRole = new LinkedHashMap<>();
        for (AccessRole role : listRoles()) {
            permissionsByRole.put(role.getId(),
                    rolePermissionRepository.findAccessRolePermissionsByAccessRoleOrderByPermission_ModuleAscPermission_NameAsc(role));
        }
        return permissionsByRole;
    }

    @Override
    @Transactional
    public AccessRole saveRole(long roleId, String name, String description, List<String> permissionCodes) {
        requireAccessManager();
        Account actor = requireLoggedUser();
        ensurePermissionCatalogue();
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Role name is required.");
        }
        Set<String> normalizedPermissionCodes = normalizePermissionCodes(permissionCodes);
        if (normalizedPermissionCodes.isEmpty()) {
            throw new IllegalArgumentException("Select at least one permission.");
        }

        AccessRole role;
        if (roleId > 0) {
            role = roleRepository.findAccessRoleByIdAndClientId(roleId, actor.getClientId());
            if (role == null) {
                throw new IllegalArgumentException("Access role was not found.");
            }
            if (!isAdminOrSpecial(actor)
                    && rolePermissionRepository.existsAccessRolePermissionByAccessRoleAndPermission_Code(
                    role, AccessPermissions.ACCESS_MANAGE)
                    && !normalizedPermissionCodes.contains(AccessPermissions.ACCESS_MANAGE)) {
                throw new IllegalArgumentException("Only an administrator can remove access-management permission from this role.");
            }
        } else {
            AccessRole duplicate = roleRepository.findAccessRoleByClientIdAndNameIgnoreCase(actor.getClientId(), name.trim());
            if (duplicate != null) {
                throw new IllegalArgumentException("An access role with this name already exists.");
            }
            role = new AccessRole();
            role.setClientId(actor.getClientId());
            role.setSystemDefault(false);
        }

        role.setName(name.trim());
        role.setDescription(description == null ? "" : description.trim());
        role.setStatus(PMConstants.STATUS_ACTIVE);
        role = roleRepository.save(role);

        syncRolePermissions(role, normalizedPermissionCodes);
        recordAudit(actor.getClientId(), null, role, roleId > 0 ? "ROLE_UPDATED" : "ROLE_CREATED", role.getDescription(), actor);
        return role;
    }

    @Override
    @Transactional
    public void deactivateRole(long roleId, String reason) {
        requireAccessManager();
        Account actor = requireLoggedUser();
        AccessRole role = roleRepository.findAccessRoleByIdAndClientId(roleId, actor.getClientId());
        if (role == null) {
            throw new IllegalArgumentException("Access role was not found.");
        }
        if (!isAdminOrSpecial(actor)
                && rolePermissionRepository.existsAccessRolePermissionByAccessRoleAndPermission_Code(
                role, AccessPermissions.ACCESS_MANAGE)) {
            throw new IllegalArgumentException("Only an administrator can deactivate an access-management role.");
        }
        role.setStatus(PMConstants.STATUS_IN_ACTIVE);
        roleRepository.save(role);
        recordAudit(actor.getClientId(), null, role, "ROLE_DEACTIVATED", reason, actor);
    }

    @Override
    public List<AccountAccessRole> listAssignments() {
        Account user = requireLoggedUser();
        return assignmentRepository.findAccountAccessRolesByClientIdOrderByIdDesc(user.getClientId());
    }

    @Override
    public List<AccessAssignmentAudit> listRecentAudit() {
        Account user = requireLoggedUser();
        return auditRepository.findTop100ByClientIdOrderByIdDesc(user.getClientId());
    }

    @Override
    @Transactional
    public AccountAccessRole assignRole(long accountId, long roleId, String scopeType, Long divisionId,
                                        Long departmentId, LocalDate effectiveFrom, LocalDate expiresOn, String reason) {
        requireAccessManager();
        Account actor = requireLoggedUser();
        Account account = accountRepository.findAccountById(accountId);
        AccessRole role = roleRepository.findAccessRoleByIdAndClientId(roleId, actor.getClientId());
        if (account == null || account.getClientId() != actor.getClientId()) {
            throw new IllegalArgumentException("Selected account does not belong to your organisation.");
        }
        if (role == null || !PMConstants.STATUS_ACTIVE.equalsIgnoreCase(role.getStatus())) {
            throw new IllegalArgumentException("Select an active access role.");
        }
        if (effectiveFrom != null && expiresOn != null && effectiveFrom.isAfter(expiresOn)) {
            throw new IllegalArgumentException("Effective date cannot be after expiry date.");
        }

        String normalizedScope = normalizeScope(scopeType);
        AccountAccessRole assignment = new AccountAccessRole();
        assignment.setClientId(actor.getClientId());
        assignment.setAccount(account);
        assignment.setAccessRole(role);
        assignment.setScopeType(normalizedScope);
        assignment.setDivision(resolveDivision(normalizedScope, divisionId, actor.getClientId()));
        assignment.setDepartment(resolveDepartment(normalizedScope, departmentId, actor.getClientId()));
        assignment.setEffectiveFrom(effectiveFrom);
        assignment.setExpiresOn(expiresOn);
        assignment.setStatus(PMConstants.STATUS_ACTIVE);
        assignment.setReason(reason == null ? "" : reason.trim());
        assignment.setAssignedBy(actor);
        assignment = assignmentRepository.save(assignment);
        recordAudit(actor.getClientId(), account, role, "ROLE_GRANTED", assignment.getReason(), actor);
        return assignment;
    }

    @Override
    @Transactional
    public void revokeAssignment(long assignmentId, String reason) {
        requireAccessManager();
        Account actor = requireLoggedUser();
        AccountAccessRole assignment = assignmentRepository.findAccountAccessRoleByIdAndClientId(assignmentId, actor.getClientId());
        if (assignment == null) {
            throw new IllegalArgumentException("Access assignment was not found.");
        }
        if (!isAdminOrSpecial(actor)
                && assignment.getAccount() != null
                && assignment.getAccount().getId() == actor.getId()
                && rolePermissionRepository.existsAccessRolePermissionByAccessRoleAndPermission_Code(
                assignment.getAccessRole(), AccessPermissions.ACCESS_MANAGE)) {
            throw new IllegalArgumentException("You cannot revoke your own access-management assignment.");
        }
        assignment.setStatus(PMConstants.STATUS_IN_ACTIVE);
        assignment.setReason(StringUtils.hasText(reason) ? reason.trim() : assignment.getReason());
        assignmentRepository.save(assignment);
        recordAudit(actor.getClientId(), assignment.getAccount(), assignment.getAccessRole(), "ROLE_REVOKED", reason, actor);
    }

    private void ensurePermissionCatalogue() {
        for (AccessPermissions.Definition definition : AccessPermissions.catalogue()) {
            AccessPermission permission = permissionRepository.findAccessPermissionByCode(definition.getCode());
            if (permission == null) {
                permission = new AccessPermission();
                permission.setCode(definition.getCode());
            }
            permission.setName(definition.getName());
            permission.setModule(definition.getModule());
            permission.setDescription(definition.getDescription());
            permission.setStatus(PMConstants.STATUS_ACTIVE);
            permissionRepository.save(permission);
        }
    }

    private void createDefaultRole(long clientId, String name, String description, List<String> permissionCodes) {
        if (roleRepository.findAccessRoleByClientIdAndNameIgnoreCase(clientId, name) != null) {
            return;
        }
        AccessRole role = new AccessRole();
        role.setClientId(clientId);
        role.setName(name);
        role.setDescription(description);
        role.setStatus(PMConstants.STATUS_ACTIVE);
        role.setSystemDefault(true);
        role = roleRepository.save(role);
        for (String permissionCode : permissionCodes) {
            AccessRolePermission rolePermission = new AccessRolePermission();
            rolePermission.setAccessRole(role);
            rolePermission.setPermission(permissionRepository.findAccessPermissionByCode(permissionCode));
            rolePermissionRepository.save(rolePermission);
        }
    }

    private void migrateLegacyWorkflowRoles(long clientId) {
        Account actor = loggedUser();
        for (Account account : accountRepository.findAccountsByClient_ClientId(clientId)) {
            if (account == null || !PMConstants.STATUS_ACTIVE.equalsIgnoreCase(account.getStatus())) {
                continue;
            }
            List<AccountAccessRole> existing =
                    assignmentRepository.findAccountAccessRolesByAccountAndClientIdOrderByIdDesc(account, clientId);
            if (existing != null && !existing.isEmpty()) {
                continue;
            }
            if (PMConstants.MODERATOR.equalsIgnoreCase(account.getRole())) {
                migrateLegacyRole(account, roleRepository.findAccessRoleByClientIdAndNameIgnoreCase(clientId, "HR Scorecard Approver"), actor);
                migrateLegacyRole(account, roleRepository.findAccessRoleByClientIdAndNameIgnoreCase(clientId, "HR Moderator"), actor);
            } else if ("HR".equalsIgnoreCase(account.getRole())) {
                migrateLegacyRole(account, roleRepository.findAccessRoleByClientIdAndNameIgnoreCase(clientId, "HR Probation Approver"), actor);
                migrateLegacyRole(account, roleRepository.findAccessRoleByClientIdAndNameIgnoreCase(clientId, "HR Probation Administrator"), actor);
            }
        }
    }

    private void migrateLegacyRole(Account account, AccessRole role, Account actor) {
        if (account == null || role == null) {
            return;
        }
        AccountAccessRole assignment = new AccountAccessRole();
        assignment.setClientId(account.getClientId());
        assignment.setAccount(account);
        assignment.setAccessRole(role);
        assignment.setScopeType(SCOPE_CLIENT);
        assignment.setEffectiveFrom(LocalDate.now());
        assignment.setStatus(PMConstants.STATUS_ACTIVE);
        assignment.setReason("Migrated from legacy account role.");
        assignment.setAssignedBy(actor);
        assignmentRepository.save(assignment);
        recordAudit(account.getClientId(), account, role, "LEGACY_ROLE_MIGRATED", assignment.getReason(), actor);
    }

    private void syncRolePermissions(AccessRole role, Set<String> permissionCodes) {
        Map<String, AccessPermission> requestedPermissions = new LinkedHashMap<>();
        for (String permissionCode : permissionCodes) {
            AccessPermission permission = permissionRepository.findAccessPermissionByCode(permissionCode);
            if (permission == null) {
                throw new IllegalArgumentException("Unknown permission: " + permissionCode);
            }
            requestedPermissions.put(permissionCode, permission);
        }

        Set<String> existingCodes = new HashSet<>();
        List<AccessRolePermission> existingRolePermissions =
                rolePermissionRepository.findAccessRolePermissionsByAccessRoleOrderByPermission_ModuleAscPermission_NameAsc(role);
        if (existingRolePermissions != null) {
            for (AccessRolePermission rolePermission : existingRolePermissions) {
                String existingCode = rolePermission.getPermission() == null
                        ? ""
                        : normalize(rolePermission.getPermission().getCode());
                if (requestedPermissions.containsKey(existingCode)) {
                    existingCodes.add(existingCode);
                } else {
                    rolePermissionRepository.delete(rolePermission);
                }
            }
        }

        for (Map.Entry<String, AccessPermission> entry : requestedPermissions.entrySet()) {
            if (existingCodes.contains(entry.getKey())) {
                continue;
            }
            AccessRolePermission rolePermission = new AccessRolePermission();
            rolePermission.setAccessRole(role);
            rolePermission.setPermission(entry.getValue());
            rolePermissionRepository.save(rolePermission);
        }
    }

    private boolean isEffective(AccountAccessRole assignment, LocalDate today) {
        return assignment != null
                && PMConstants.STATUS_ACTIVE.equalsIgnoreCase(assignment.getStatus())
                && assignment.getAccessRole() != null
                && PMConstants.STATUS_ACTIVE.equalsIgnoreCase(assignment.getAccessRole().getStatus())
                && (assignment.getEffectiveFrom() == null || !today.isBefore(assignment.getEffectiveFrom()))
                && (assignment.getExpiresOn() == null || !today.isAfter(assignment.getExpiresOn()));
    }

    private boolean scopeMatches(AccountAccessRole assignment, Account subject) {
        if (SCOPE_CLIENT.equalsIgnoreCase(assignment.getScopeType())) {
            return subject == null || subject.getClientId() == assignment.getClientId();
        }
        if (subject == null) {
            return false;
        }
        if (SCOPE_DIVISION.equalsIgnoreCase(assignment.getScopeType())) {
            return assignment.getDivision() != null && subject.getDivision() != null
                    && assignment.getDivision().getId() == subject.getDivision().getId();
        }
        if (SCOPE_DEPARTMENT.equalsIgnoreCase(assignment.getScopeType())) {
            return assignment.getDepartment() != null && subject.getDepartment() != null
                    && assignment.getDepartment().getId() == subject.getDepartment().getId();
        }
        return false;
    }

    private boolean legacyPermission(Account account, String permissionCode) {
        String code = normalize(permissionCode);
        if (AccessPermissions.SCORECARD_APPROVE_TARGETS_HR.equals(code)
                || AccessPermissions.SCORECARD_APPROVE_AGREED_SCORES_HR.equals(code)
                || AccessPermissions.SCORECARD_MODERATE.equals(code)) {
            return PMConstants.MODERATOR.equalsIgnoreCase(account.getRole());
        }
        if (AccessPermissions.PROBATION_CREATE_CONTRACT.equals(code)
                || AccessPermissions.PROBATION_APPROVE_KPI_CONTRACT.equals(code)
                || AccessPermissions.PROBATION_APPROVE_FINAL_ASSESSMENT.equals(code)
                || AccessPermissions.PROBATION_CONFIGURE.equals(code)) {
            return "HR".equalsIgnoreCase(account.getRole());
        }
        return false;
    }

    private boolean isAdminOrSpecial(Account account) {
        return account != null && (PMConstants.IS_ADMIN.equalsIgnoreCase(account.getAdmin())
                || PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(account.getSpecial()));
    }

    private String normalizeScope(String scopeType) {
        String normalized = normalize(scopeType);
        if (SCOPE_DIVISION.equals(normalized) || SCOPE_DEPARTMENT.equals(normalized)) {
            return normalized;
        }
        return SCOPE_CLIENT;
    }

    private Division resolveDivision(String scopeType, Long divisionId, long clientId) {
        if (!SCOPE_DIVISION.equals(scopeType)) {
            return null;
        }
        Division division = divisionId == null ? null : divisionRepository.findById(divisionId).orElse(null);
        if (division == null || division.getClientId() != clientId) {
            throw new IllegalArgumentException("Select a valid division for this access scope.");
        }
        return division;
    }

    private Department resolveDepartment(String scopeType, Long departmentId, long clientId) {
        if (!SCOPE_DEPARTMENT.equals(scopeType)) {
            return null;
        }
        Department department = departmentId == null ? null : departmentRepository.findDepartmentById(departmentId);
        if (department == null || department.getClientId() != clientId) {
            throw new IllegalArgumentException("Select a valid department for this access scope.");
        }
        return department;
    }

    private void requireAccessManager() {
        if (!canManageAccess()) {
            throw new IllegalStateException("You are not allowed to manage access roles.");
        }
    }

    private Account requireLoggedUser() {
        Account account = loggedUser();
        if (account == null) {
            throw new IllegalStateException("A logged-in account is required.");
        }
        return account;
    }

    private Account loggedUser() {
        Object value = session.getAttribute("loggedUser");
        return value instanceof Account ? (Account) value : null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }

    private Set<String> normalizePermissionCodes(List<String> permissionCodes) {
        Set<String> normalizedPermissionCodes = new LinkedHashSet<>();
        if (permissionCodes == null) {
            return normalizedPermissionCodes;
        }
        for (String permissionCode : permissionCodes) {
            String normalizedPermissionCode = normalize(permissionCode);
            if (StringUtils.hasText(normalizedPermissionCode)) {
                normalizedPermissionCodes.add(normalizedPermissionCode);
            }
        }
        return normalizedPermissionCodes;
    }

    private void recordAudit(long clientId, Account account, AccessRole role, String action, String reason, Account actor) {
        AccessAssignmentAudit audit = new AccessAssignmentAudit();
        audit.setClientId(clientId);
        audit.setAccount(account);
        audit.setAccessRole(role);
        audit.setAction(action);
        audit.setReason(reason == null ? "" : reason.trim());
        audit.setActor(actor);
        auditRepository.save(audit);
    }
}
