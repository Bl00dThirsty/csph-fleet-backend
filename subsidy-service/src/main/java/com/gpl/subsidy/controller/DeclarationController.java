package com.gpl.subsidy.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.subsidy.dto.CreateDeclarationRequest;
import com.gpl.subsidy.dto.DeclarationResponse;
import com.gpl.subsidy.dto.UpdateDeclarationRequest;
import com.gpl.subsidy.service.DeclarationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/declarations")
@RequiredArgsConstructor
@Slf4j
public class DeclarationController {

    private final DeclarationService declarationService;

    @RequiresPermission("DECLARATION_VIEW")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DeclarationResponse>>> listDeclarations(
            @RequestParam(required = false) String declaringOrganizationId,
            @RequestParam(required = false) String siteId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String period,
            Pageable pageable) {
        log.info("REST request to list declarations");
        PageResponse<DeclarationResponse> response = declarationService.listDeclarations(declaringOrganizationId, siteId, status, period, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @RequiresPermission("DECLARATION_CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<DeclarationResponse>> createDeclaration(
            @Valid @RequestBody CreateDeclarationRequest request,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to create declaration");
        DeclarationResponse result = declarationService.createDeclaration(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "Declaration created successfully"));
    }

    @RequiresPermission("DECLARATION_VIEW")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeclarationResponse>> getDeclaration(@PathVariable String id) {
        log.info("REST request to get declaration: {}", id);
        DeclarationResponse result = declarationService.getDeclaration(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @RequiresPermission("DECLARATION_UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeclarationResponse>> updateDeclaration(
            @PathVariable String id,
            @Valid @RequestBody UpdateDeclarationRequest request,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to update declaration: {}", id);
        DeclarationResponse result = declarationService.updateDeclaration(id, request, username);
        return ResponseEntity.ok(ApiResponse.ok(result, "Declaration updated successfully"));
    }

    @RequiresPermission("DECLARATION_SUBMIT")
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<DeclarationResponse>> submitDeclaration(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to submit declaration: {}", id);
        DeclarationResponse result = declarationService.submitDeclaration(id, username);
        return ResponseEntity.ok(ApiResponse.ok(result, "Declaration submitted successfully"));
    }

    @RequiresPermission("DECLARATION_DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDeclaration(@PathVariable String id) {
        log.info("REST request to delete declaration: {}", id);
        declarationService.deleteDeclaration(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Declaration deleted successfully"));
    }
}
