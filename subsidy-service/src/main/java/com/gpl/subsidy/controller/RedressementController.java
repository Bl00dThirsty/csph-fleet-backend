package com.gpl.subsidy.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.subsidy.dto.CreateRedressementRequest;
import com.gpl.subsidy.dto.RedressementResponse;
import com.gpl.subsidy.service.RedressementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/redressements")
@RequiredArgsConstructor
@Slf4j
public class RedressementController {

    private final RedressementService redressementService;

    @RequiresPermission("REDESSEMENT_VIEW")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RedressementResponse>>> listRedressements(
            @RequestParam(required = false) String reconciliationId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        log.info("REST request to list redressements");
        PageResponse<RedressementResponse> response = redressementService.listRedressements(reconciliationId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @RequiresPermission("REDESSEMENT_CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<RedressementResponse>> createRedressement(
            @Valid @RequestBody CreateRedressementRequest request,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to create redressement");
        RedressementResponse result = redressementService.createRedressement(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "Redressement created successfully"));
    }

    @RequiresPermission("REDESSEMENT_VIEW")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RedressementResponse>> getRedressement(@PathVariable String id) {
        log.info("REST request to get redressement: {}", id);
        RedressementResponse result = redressementService.getRedressement(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @RequiresPermission("REDESSEMENT_UPDATE")
    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<RedressementResponse>> markAsPaid(
            @PathVariable String id,
            @RequestParam(required = false) String transactionRef,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to mark redressement as paid: {}", id);
        RedressementResponse result = redressementService.markAsPaid(id, transactionRef, username);
        return ResponseEntity.ok(ApiResponse.ok(result, "Redressement marked as paid successfully"));
    }

    @RequiresPermission("REDESSEMENT_UPDATE")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RedressementResponse>> cancelRedressement(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to cancel redressement: {}", id);
        RedressementResponse result = redressementService.cancelRedressement(id, username);
        return ResponseEntity.ok(ApiResponse.ok(result, "Redressement cancelled successfully"));
    }

    @RequiresPermission("REDESSEMENT_DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRedressement(@PathVariable String id) {
        log.info("REST request to delete redressement: {}", id);
        redressementService.deleteRedressement(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Redressement deleted successfully"));
    }
}
