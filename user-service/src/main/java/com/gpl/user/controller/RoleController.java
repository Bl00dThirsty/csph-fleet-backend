package com.gpl.user.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.user.dto.CreateRoleRequest;
import com.gpl.user.dto.PermissionResponse;
import com.gpl.user.dto.RoleResponse;
import com.gpl.user.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @RequiresPermission("ROLE_VIEW")
    @GetMapping("/")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.success(roleService.listRoles());
    }

    @RequiresPermission("ROLE_VIEW")
    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> getRole(@PathVariable String id) {
        return ApiResponse.success(roleService.getRole(id));
    }

    @RequiresPermission("ROLE_CREATE")
    @PostMapping("/")
    public ApiResponse<RoleResponse> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @RequestHeader("X-User-PersonId") String createdBy) {
        return ApiResponse.success(roleService.createRole(request, createdBy));
    }

    @RequiresPermission("ROLE_VIEW")
    @GetMapping("/{id}/permissions")
    public ApiResponse<List<PermissionResponse>> getPermissions(@PathVariable String id) {
        return ApiResponse.success(roleService.getPermissionsForRole(id));
    }

    @RequiresPermission("ROLE_UPDATE")
    @PostMapping("/{id}/permissions")
    public ApiResponse<Void> grantPermission(
            @PathVariable String id,
            @RequestParam String permissionId,
            @RequestHeader("X-User-PersonId") String grantedBy) {
        roleService.grantPermission(id, permissionId, grantedBy);
        return ApiResponse.success(null);
    }

    @RequiresPermission("ROLE_UPDATE")
    @DeleteMapping("/{id}/permissions/{permissionId}")
    public ApiResponse<Void> revokePermission(
            @PathVariable String id,
            @PathVariable String permissionId) {
        roleService.revokePermission(id, permissionId);
        return ApiResponse.success(null);
    }
}
