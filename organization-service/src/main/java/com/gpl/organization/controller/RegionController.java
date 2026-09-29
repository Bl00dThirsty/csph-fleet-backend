package com.gpl.organization.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.organization.dto.RegionResponse;
import com.gpl.organization.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.gpl.common.security.RequiresPermission;

import java.util.List;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @RequiresPermission("SITE_VIEW")
    @GetMapping
    public ApiResponse<List<RegionResponse>> listRegions() {
        return ApiResponse.success(regionService.listRegions());
    }

    @RequiresPermission("SITE_VIEW")
    @GetMapping("/{id}")
    public ApiResponse<RegionResponse> getRegion(@PathVariable String id) {
        return ApiResponse.success(regionService.getRegion(id));
    }

    @RequiresPermission("SITE_VIEW")
    @GetMapping("/by-code/{code}")
    public ApiResponse<RegionResponse> getRegionByCode(@PathVariable String code) {
        return ApiResponse.success(regionService.getRegionByCode(code));
    }
}
