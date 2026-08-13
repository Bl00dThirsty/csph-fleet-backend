package com.gpl.organization.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.organization.dto.CreateRelationshipRequest;
import com.gpl.organization.dto.RelationshipResponse;
import com.gpl.organization.service.OrganizationRelationshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.gpl.common.security.RequiresPermission;

@RestController
@RequestMapping("/api/v1/organization-relationships")
@RequiredArgsConstructor
public class OrganizationRelationshipController {
    private final OrganizationRelationshipService relationshipService;

    @RequiresPermission("ORG_UPDATE")
    @PostMapping
    public ApiResponse<RelationshipResponse> create(
            @Valid @RequestBody CreateRelationshipRequest request,
            @RequestHeader("X-User-PersonId") String userId) {
        return ApiResponse.success(relationshipService.createRelationship(request, userId));
    }
}
