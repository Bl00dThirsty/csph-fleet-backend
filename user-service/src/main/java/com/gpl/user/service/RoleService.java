package com.gpl.user.service;

import com.gpl.user.dto.CreateRoleRequest;
import com.gpl.user.dto.PermissionResponse;
import com.gpl.user.dto.RoleResponse;
import com.gpl.user.model.Permission;
import com.gpl.user.model.Role;
import com.gpl.user.model.RolePermission;
import com.gpl.user.repository.PermissionRepository;
import com.gpl.user.repository.RolePermissionRepository;
import com.gpl.user.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, RolePermissionRepository rolePermissionRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request, String createdBy) {
        Role role = new Role();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setScopeOrgType(request.getScopeOrgType());
        role.setMinTier(request.getMinTier());
        role.setSortOrder(request.getSortOrder());
        role.setCreatedBy(createdBy);
        role.setCreatedAt(Instant.now());
        role = roleRepository.save(role);
        return buildRoleResponse(role);
    }

    public RoleResponse getRole(String id) {
        Role role = roleRepository.findById(id).orElseThrow();
        return buildRoleResponse(role);
    }
    
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream().map(this::buildRoleResponse).collect(Collectors.toList());
    }

    public List<PermissionResponse> getPermissionsForRole(String roleId) {
        List<RolePermission> rps = rolePermissionRepository.findByRoleId(roleId);
        return rps.stream()
                .filter(RolePermission::isGranted)
                .map(rp -> permissionRepository.findById(rp.getPermissionId()).orElse(null))
                .filter(p -> p != null)
                .map(p -> {
                    PermissionResponse pr = new PermissionResponse();
                    pr.setId(p.getId());
                    pr.setCode(p.getCode());
                    pr.setName(p.getName());
                    pr.setDescription(p.getDescription());
                    pr.setModule(p.getModule());
                    pr.setModuleDescription(p.getModuleDescription());
                    pr.setActive(p.isActive());
                    pr.setSortOrder(p.getSortOrder());
                    return pr;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void grantPermission(String roleId, String permissionId, String grantedBy) {
        Optional<RolePermission> opt = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId);
        RolePermission rp;
        if (opt.isPresent()) {
            rp = opt.get();
            rp.setGranted(true);
        } else {
            rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rp.setGranted(true);
        }
        rp.setGrantedBy(grantedBy);
        rp.setGrantedAt(Instant.now());
        rolePermissionRepository.save(rp);
    }

    @Transactional
    public void revokePermission(String roleId, String permissionId) {
        Optional<RolePermission> opt = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId);
        opt.ifPresent(rp -> {
            rp.setGranted(false);
            rolePermissionRepository.save(rp);
        });
    }

    private RoleResponse buildRoleResponse(Role role) {
        RoleResponse res = new RoleResponse();
        res.setId(role.getId());
        res.setCode(role.getCode());
        res.setName(role.getName());
        res.setDescription(role.getDescription());
        res.setScopeOrgType(role.getScopeOrgType());
        res.setMinTier(role.getMinTier());
        res.setStatus(role.getStatus());
        res.setStatusDescription(role.getStatusDescription());
        res.setSystemRole(role.isSystemRole());
        res.setActive(role.isActive());
        res.setSortOrder(role.getSortOrder());
        res.setPermissions(getPermissionsForRole(role.getId()));
        return res;
    }
}
