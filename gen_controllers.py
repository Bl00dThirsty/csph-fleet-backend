import os

base_dir = r"c:\Users\User\Downloads\gpl-rfid-livraisons\backend\organization-service\src\main\java\com\gpl\organization"

def write_file(sub_dir, name, content):
    d = os.path.join(base_dir, sub_dir)
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, name), "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

write_file("controller", "OrganizationController.java", """
package com.gpl.organization.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.organization.dto.*;
import com.gpl.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    
    private final OrganizationService organizationService;

    @GetMapping
    public PageResponse<OrganizationSummaryResponse> listOrganizations(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return organizationService.searchOrganizations(search, pageable);
        }
        return organizationService.listOrganizations(type, tier, isActive, pageable);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrganizationResponse> getOrganization(@PathVariable String id) {
        return ApiResponse.success(organizationService.getOrganization(id));
    }

    @PostMapping
    public ApiResponse<OrganizationResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @RequestHeader("X-User-PersonId") String userId) {
        return ApiResponse.success(organizationService.createOrganization(request, userId));
    }

    @PutMapping("/{id}")
    public ApiResponse<OrganizationResponse> updateOrganization(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            @RequestHeader("X-User-PersonId") String userId) {
        return ApiResponse.success(organizationService.updateOrganization(id, request, userId));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("X-User-PersonId") String userId) {
        return ApiResponse.success(organizationService.updateStatus(id, request, userId));
    }

    @GetMapping("/{id}/children")
    public ApiResponse<List<OrganizationSummaryResponse>> getChildren(@PathVariable String id) {
        return ApiResponse.success(organizationService.getChildren(id));
    }

    @GetMapping("/{id}/hierarchy")
    public ApiResponse<Object> getHierarchy(@PathVariable String id) {
        return ApiResponse.success(organizationService.getHierarchy(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteOrganization(@PathVariable String id) {
        organizationService.deleteOrganization(id);
        return ApiResponse.success(null);
    }
}
""")

write_file("controller", "SiteController.java", """
package com.gpl.organization.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.organization.dto.*;
import com.gpl.organization.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping
    public PageResponse<SiteSummaryResponse> listSites(
            @RequestParam(required = false) String organizationId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean isOperational,
            Pageable pageable) {
        return siteService.listSites(organizationId, type, city, isOperational, pageable);
    }

    @GetMapping("/{id}")
    public ApiResponse<SiteResponse> getSite(@PathVariable String id) {
        return ApiResponse.success(siteService.getSite(id));
    }

    @PostMapping
    public ApiResponse<SiteResponse> createSite(
            @Valid @RequestBody CreateSiteRequest request,
            @RequestHeader("X-User-PersonId") String userId) {
        return ApiResponse.success(siteService.createSite(request, userId));
    }

    @PutMapping("/{id}")
    public ApiResponse<SiteResponse> updateSite(
            @PathVariable String id,
            @Valid @RequestBody UpdateSiteRequest request,
            @RequestHeader("X-User-PersonId") String userId) {
        return ApiResponse.success(siteService.updateSite(id, request, userId));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("X-User-PersonId") String userId) {
        return ApiResponse.success(siteService.updateSiteStatus(id, request, userId));
    }

    @GetMapping("/nearby")
    public ApiResponse<List<SiteSummaryResponse>> getNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam double radius) {
        return ApiResponse.success(siteService.findNearbySites(lat, lon, radius));
    }
}
""")

write_file("controller", "ClassStructureController.java", """
package com.gpl.organization.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.organization.dto.ClassStructureResponse;
import com.gpl.organization.service.ClassStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classifications")
@RequiredArgsConstructor
public class ClassStructureController {
    private final ClassStructureService classStructureService;

    @GetMapping
    public ApiResponse<List<ClassStructureResponse>> getAll() {
        return ApiResponse.success(classStructureService.getAll());
    }
}
""")

write_file("controller", "OrganizationRelationshipController.java", """
package com.gpl.organization.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.organization.dto.CreateRelationshipRequest;
import com.gpl.organization.dto.RelationshipResponse;
import com.gpl.organization.service.OrganizationRelationshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organization-relationships")
@RequiredArgsConstructor
public class OrganizationRelationshipController {
    private final OrganizationRelationshipService relationshipService;

    @PostMapping
    public ApiResponse<RelationshipResponse> create(
            @Valid @RequestBody CreateRelationshipRequest request,
            @RequestHeader("X-User-PersonId") String userId) {
        return ApiResponse.success(relationshipService.createRelationship(request, userId));
    }
}
""")

print("Controllers generated!")
