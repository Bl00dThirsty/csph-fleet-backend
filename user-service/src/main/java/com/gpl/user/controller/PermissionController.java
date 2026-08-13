package com.gpl.user.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.user.model.Permission;
import com.gpl.user.repository.PermissionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {
    private final PermissionRepository repository;

    public PermissionController(PermissionRepository repository) {
        this.repository = repository;
    }

    @RequiresPermission("PERMISSION_VIEW")
    @GetMapping("/")
    public ApiResponse<List<Permission>> listPermissions() {
        return ApiResponse.success(repository.findAll());
    }

    @RequiresPermission("PERMISSION_VIEW")
    @GetMapping("/{id}")
    public ApiResponse<Permission> getPermission(@PathVariable String id) {
        return ApiResponse.success(repository.findById(id).orElseThrow());
    }

    @RequiresPermission("PERMISSION_VIEW")
    @GetMapping("/modules")
    public ApiResponse<List<String>> listModules() {
        List<String> modules = repository.findAll().stream()
                .map(Permission::getModule)
                .distinct()
                .collect(Collectors.toList());
        return ApiResponse.success(modules);
    }
}
