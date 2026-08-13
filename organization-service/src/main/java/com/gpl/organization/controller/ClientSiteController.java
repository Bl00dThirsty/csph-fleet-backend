package com.gpl.organization.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.organization.dto.ClientSiteResponse;
import com.gpl.organization.dto.CreateClientSiteRequest;
import com.gpl.organization.dto.UpdateClientSiteRequest;
import com.gpl.organization.service.ClientSiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/client-sites")
@RequiredArgsConstructor
public class ClientSiteController {

    private final ClientSiteService clientSiteService;

    @RequiresPermission("SITE_VIEW")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ClientSiteResponse>>> listClientSites(
            @RequestParam(required = false) String clientOrganizationId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(clientSiteService.listClientSites(clientOrganizationId, pageable)));
    }

    @RequiresPermission("SITE_VIEW")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientSiteResponse>> getClientSite(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(clientSiteService.getClientSite(id)));
    }

    @RequiresPermission("SITE_VIEW")
    @GetMapping("/by-site/{siteId}")
    public ResponseEntity<ApiResponse<ClientSiteResponse>> getClientSiteBySiteId(@PathVariable String siteId) {
        return ResponseEntity.ok(ApiResponse.success(clientSiteService.getClientSiteBySiteId(siteId)));
    }

    @RequiresPermission("CLIENT_SITE_MANAGE")
    @PostMapping
    public ResponseEntity<ApiResponse<ClientSiteResponse>> createClientSite(
            @Valid @RequestBody CreateClientSiteRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(clientSiteService.createClientSite(request, userId)));
    }

    @RequiresPermission("CLIENT_SITE_MANAGE")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientSiteResponse>> updateClientSite(
            @PathVariable String id,
            @Valid @RequestBody UpdateClientSiteRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String userId) {
        return ResponseEntity.ok(ApiResponse.success(clientSiteService.updateClientSite(id, request, userId)));
    }

    @RequiresPermission("CLIENT_SITE_MANAGE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClientSite(@PathVariable String id) {
        clientSiteService.deleteClientSite(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
