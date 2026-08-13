package com.gpl.subsidy.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.subsidy.dto.CreateReconciliationRequest;
import com.gpl.subsidy.dto.ReconciliationResponse;
import com.gpl.subsidy.service.ReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reconciliations")
@RequiredArgsConstructor
@Slf4j
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @RequiresPermission("RECONCILIATION_VIEW")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReconciliationResponse>>> listReconciliations(
            @RequestParam(required = false) String declarationId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        log.info("REST request to list reconciliations");
        PageResponse<ReconciliationResponse> response = reconciliationService.listReconciliations(declarationId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @RequiresPermission("RECONCILIATION_CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<ReconciliationResponse>> createReconciliation(
            @Valid @RequestBody CreateReconciliationRequest request,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to create reconciliation");
        ReconciliationResponse result = reconciliationService.createReconciliation(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "Reconciliation created successfully"));
    }

    @RequiresPermission("RECONCILIATION_VIEW")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReconciliationResponse>> getReconciliation(@PathVariable String id) {
        log.info("REST request to get reconciliation: {}", id);
        ReconciliationResponse result = reconciliationService.getReconciliation(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @RequiresPermission("RECONCILIATION_VIEW")
    @GetMapping("/declaration/{declarationId}")
    public ResponseEntity<ApiResponse<ReconciliationResponse>> getReconciliationByDeclarationId(@PathVariable String declarationId) {
        log.info("REST request to get reconciliation by declaration ID: {}", declarationId);
        ReconciliationResponse result = reconciliationService.getReconciliationByDeclarationId(declarationId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @RequiresPermission("RECONCILIATION_VERIFY")
    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<ReconciliationResponse>> verifyReconciliation(
            @PathVariable String id,
            @RequestParam(required = false) String verifiedBy,
            @RequestParam(required = false) String notes,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to verify reconciliation: {}", id);
        ReconciliationResponse result = reconciliationService.verifyReconciliation(id, verifiedBy, notes, username);
        return ResponseEntity.ok(ApiResponse.ok(result, "Reconciliation verified successfully"));
    }

    @RequiresPermission("RECONCILIATION_DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReconciliation(@PathVariable String id) {
        log.info("REST request to delete reconciliation: {}", id);
        reconciliationService.deleteReconciliation(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Reconciliation deleted successfully"));
    }
}
