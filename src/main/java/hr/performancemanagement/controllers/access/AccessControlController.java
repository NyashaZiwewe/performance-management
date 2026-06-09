package hr.performancemanagement.controllers.access;

import hr.performancemanagement.entities.AccessRole;
import hr.performancemanagement.entities.Account;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/access-control")
public class AccessControlController {

    private final AccessControlService accessControlService;
    private final CommonService commonService;
    private final AccountService accountService;
    private final DepartmentService departmentService;
    private final DivisionService divisionService;

    public AccessControlController(AccessControlService accessControlService,
                                   CommonService commonService,
                                   AccountService accountService,
                                   DepartmentService departmentService,
                                   DivisionService divisionService) {
        this.accessControlService = accessControlService;
        this.commonService = commonService;
        this.accountService = accountService;
        this.departmentService = departmentService;
        this.divisionService = divisionService;
    }

    @RequestMapping
    public ModelAndView viewAccessControl(@RequestParam(value = "roleId", required = false) Long roleId,
                                          HttpServletRequest request) {
        if (!hasAccess(request)) {
            return new ModelAndView("redirect:/");
        }

        Account loggedUser = commonService.getLoggedUser();
        accessControlService.ensureDefaultConfiguration(loggedUser.getClientId());
        List<AccessRole> roles = accessControlService.listRoles();
        AccessRole roleForm = new AccessRole();
        roleForm.setStatus("ACTIVE");
        if (roleId != null && roleId > 0) {
            for (AccessRole role : roles) {
                if (role.getId() == roleId) {
                    roleForm = role;
                    break;
                }
            }
        }

        Set<String> selectedPermissionCodes = Collections.emptySet();
        if (roleForm.getId() > 0) {
            selectedPermissionCodes = new java.util.LinkedHashSet<>();
            for (hr.performancemanagement.entities.AccessRolePermission rolePermission
                    : accessControlService.listRolePermissions().getOrDefault(roleForm.getId(), Collections.emptyList())) {
                selectedPermissionCodes.add(rolePermission.getPermission().getCode());
            }
        }

        ModelAndView modelAndView = new ModelAndView(Pages.ACCESS_CONTROL);
        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Access Roles");
        modelAndView.addObject("pageTitle", "Access Roles");
        modelAndView.addObject("permissions", accessControlService.listPermissions());
        modelAndView.addObject("roles", roles);
        modelAndView.addObject("rolePermissions", accessControlService.listRolePermissions());
        modelAndView.addObject("roleForm", roleForm);
        modelAndView.addObject("selectedPermissionCodes", selectedPermissionCodes);
        modelAndView.addObject("assignments", accessControlService.listAssignments());
        modelAndView.addObject("accessAudit", accessControlService.listRecentAudit());
        modelAndView.addObject("accounts", accountService.listAllAccountsByClientId(loggedUser.getClientId()));
        modelAndView.addObject("departments", departmentService.listAllDepartments(loggedUser.getClientId()));
        modelAndView.addObject("divisions", divisionService.listAllDivisions(loggedUser.getClientId()));
        modelAndView.addObject("scopeClient", AccessControlService.SCOPE_CLIENT);
        modelAndView.addObject("scopeDivision", AccessControlService.SCOPE_DIVISION);
        modelAndView.addObject("scopeDepartment", AccessControlService.SCOPE_DEPARTMENT);
        PortletUtils.addMessagesToPage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-role", method = RequestMethod.POST)
    public String saveRole(@RequestParam(value = "id", defaultValue = "0") long roleId,
                           @RequestParam("name") String name,
                           @RequestParam(value = "description", required = false) String description,
                           @RequestParam(value = "permissionCodes", required = false) List<String> permissionCodes,
                           HttpServletRequest request) {
        if (!hasAccess(request)) {
            return "redirect:/";
        }
        try {
            accessControlService.saveRole(roleId, name, description,
                    permissionCodes == null ? Collections.emptyList() : permissionCodes);
            PortletUtils.addInfoMsg("Access role saved successfully.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Could not save access role: " + exception.getMessage(), request);
        }
        return "redirect:/access-control";
    }

    @RequestMapping(value = "/deactivate-role", method = RequestMethod.POST)
    public String deactivateRole(long roleId,
                                 @RequestParam(value = "reason", required = false) String reason,
                                 HttpServletRequest request) {
        if (!hasAccess(request)) {
            return "redirect:/";
        }
        try {
            accessControlService.deactivateRole(roleId, reason);
            PortletUtils.addInfoMsg("Access role deactivated. Its assignments no longer grant permissions.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Could not deactivate access role: " + exception.getMessage(), request);
        }
        return "redirect:/access-control";
    }

    @RequestMapping(value = "/assign-role", method = RequestMethod.POST)
    public String assignRole(long accountId,
                             long roleId,
                             String scopeType,
                             @RequestParam(value = "divisionId", required = false) Long divisionId,
                             @RequestParam(value = "departmentId", required = false) Long departmentId,
                             @RequestParam(value = "effectiveFrom", required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
                             @RequestParam(value = "expiresOn", required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresOn,
                             @RequestParam(value = "reason", required = false) String reason,
                             HttpServletRequest request) {
        if (!hasAccess(request)) {
            return "redirect:/";
        }
        try {
            accessControlService.assignRole(accountId, roleId, scopeType, divisionId, departmentId,
                    effectiveFrom, expiresOn, reason);
            PortletUtils.addInfoMsg("Access role assigned successfully. The new rights take effect immediately.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Could not assign access role: " + exception.getMessage(), request);
        }
        return "redirect:/access-control";
    }

    @RequestMapping(value = "/revoke-assignment", method = RequestMethod.POST)
    public String revokeAssignment(long assignmentId,
                                   @RequestParam(value = "reason", required = false) String reason,
                                   HttpServletRequest request) {
        if (!hasAccess(request)) {
            return "redirect:/";
        }
        try {
            accessControlService.revokeAssignment(assignmentId, reason);
            PortletUtils.addInfoMsg("Access assignment revoked. The rights no longer apply.", request);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Could not revoke access assignment: " + exception.getMessage(), request);
        }
        return "redirect:/access-control";
    }

    private boolean hasAccess(HttpServletRequest request) {
        if (accessControlService.canManageAccess()) {
            return true;
        }
        PortletUtils.addErrorMsg("You are not allowed to manage access roles.", request);
        return false;
    }
}
