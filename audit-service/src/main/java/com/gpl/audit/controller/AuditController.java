package com.gpl.audit.controller;

import com.gpl.audit.dto.AuditQueryParams;
import com.gpl.audit.dto.AuditSummaryResponse;
import com.gpl.audit.dto.EntityModificationResponse;
import com.gpl.audit.dto.StatusHistoryResponse;
import com.gpl.audit.service.AuditQueryService;
import com.gpl.common.security.RequiresPermission;
import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    @RequiresPermission("AUDIT_VIEW_MODIFICATIONS")
    @GetMapping("/modifications")
    public ResponseEntity<ApiResponse<PageResponse<EntityModificationResponse>>> getModifications(@ModelAttribute AuditQueryParams params) {
        PageResponse<EntityModificationResponse> response = auditQueryService.getModifications(params);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @RequiresPermission("AUDIT_VIEW_MODIFICATIONS")
    @GetMapping("/status-history")
    public ResponseEntity<ApiResponse<PageResponse<StatusHistoryResponse>>> getStatusHistory(@ModelAttribute AuditQueryParams params) {
        PageResponse<StatusHistoryResponse> response = auditQueryService.getStatusHistory(params);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @RequiresPermission("AUDIT_VIEW_MODIFICATIONS")
    @GetMapping("/summary/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<AuditSummaryResponse>> getSummary(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        AuditSummaryResponse response = auditQueryService.getSummary(entityType, entityId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
