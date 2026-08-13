package com.gpl.organization.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.organization.dto.*;
import com.gpl.organization.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import com.gpl.common.security.RequiresPermission;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @RequiresPermission("SITE_VIEW")
    @GetMapping
    public PageResponse<SiteSummaryResponse> listSites(
            @RequestParam(required = false) String organizationId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean isOperational,
            Pageable pageable) {
        return siteService.listSites(organizationId, type, city, isOperational, pageable);
    }

    @RequiresPermission("SITE_VIEW")
    @GetMapping("/{id}")
    public ApiResponse<SiteResponse> getSite(@PathVariable String id) {
        return ApiResponse.success(siteService.getSite(id));
    }

    @RequiresPermission("SITE_CREATE")
    @PostMapping
    public ApiResponse<SiteResponse> createSite(
            @Valid @RequestBody CreateSiteRequest request,
            @RequestHeader("X-User-PersonId") String userId) {
        return ApiResponse.success(siteService.createSite(request, userId));
    }

    @RequiresPermission("SITE_UPDATE")
    @PutMapping("/{id}")
    public ApiResponse<SiteResponse> updateSite(
            @PathVariable String id,
            @Valid @RequestBody UpdateSiteRequest request,
            @RequestHeader("X-User-PersonId") String userId) {
        return ApiResponse.success(siteService.updateSite(id, request, userId));
    }

    @RequiresPermission("SITE_UPDATE")
    @PatchMapping("/{id}/status")
    public ApiResponse<?> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("X-User-PersonId") String userId) {
        return ApiResponse.success(siteService.updateSiteStatus(id, request, userId));
    }

    @RequiresPermission("SITE_VIEW")
    @GetMapping("/nearby")
    public ApiResponse<List<SiteSummaryResponse>> getNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam double radius) {
        return ApiResponse.success(siteService.findNearbySites(lat, lon, radius));
    }
}
