package com.gpl.cylinder.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.cylinder.dto.CreateCylinderRequest;
import com.gpl.cylinder.dto.UpdateCylinderRequest;
import com.gpl.cylinder.dto.CylinderResponse;
import com.gpl.cylinder.service.CylinderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cylinders")
@RequiredArgsConstructor
public class CylinderController {

    private final CylinderService cylinderService;

    @RequiresPermission("cylinders.read")
    @GetMapping
    public PageResponse<CylinderResponse> listCylinders(
            @RequestParam(required = false) String ownerOrganizationId,
            @RequestParam(required = false) String currentSiteId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double capacityKg,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return cylinderService.listCylinders(ownerOrganizationId, currentSiteId, brand, capacityKg, status, pageable);
    }

    @RequiresPermission("cylinders.read")
    @GetMapping("/{id}")
    public ApiResponse<CylinderResponse> getCylinder(@PathVariable String id) {
        return ApiResponse.success(cylinderService.getCylinder(id));
    }

    @RequiresPermission("cylinders.create")
    @PostMapping
    public ApiResponse<CylinderResponse> createCylinder(
            @Valid @RequestBody CreateCylinderRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String userId) {
        return ApiResponse.success(cylinderService.createCylinder(request, userId));
    }

    @RequiresPermission("cylinders.update")
    @PutMapping("/{id}")
    public ApiResponse<CylinderResponse> updateCylinder(
            @PathVariable String id,
            @RequestBody UpdateCylinderRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String userId) {
        return ApiResponse.success(cylinderService.updateCylinder(id, request, userId));
    }

    @RequiresPermission("cylinders.delete")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCylinder(@PathVariable String id) {
        cylinderService.deleteCylinder(id);
        return ApiResponse.success(null);
    }
}
