package com.gpl.user.service;

import com.gpl.user.model.UserRoleAssignment;
import com.gpl.user.repository.PermissionRepository;
import com.gpl.user.repository.RolePermissionRepository;
import com.gpl.user.repository.UserRoleAssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PermissionService {
    private final UserRoleAssignmentRepository uraRepo;
    private final RolePermissionRepository rolePermissionRepo;

    public PermissionService(UserRoleAssignmentRepository uraRepo, RolePermissionRepository rolePermissionRepo) {
        this.uraRepo = uraRepo;
        this.rolePermissionRepo = rolePermissionRepo;
    }

    public Set<String> resolvePermissions(String personId) {
        List<UserRoleAssignment> activeRoles = uraRepo.findActiveByPersonId(personId);
        Set<String> perms = new HashSet<>();
        for (UserRoleAssignment ura : activeRoles) {
            perms.addAll(rolePermissionRepo.findPermissionCodesByRoleId(ura.getRoleId()));
        }
        return perms;
    }

    public boolean hasPermission(String personId, String permissionCode) {
        Set<String> perms = resolvePermissions(personId);
        return perms.contains(permissionCode);
    }
}
