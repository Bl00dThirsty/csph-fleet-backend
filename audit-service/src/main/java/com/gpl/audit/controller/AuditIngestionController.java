package com.gpl.audit.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.event.AuditEvent;
import com.gpl.audit.service.AuditIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditIngestionController {
    
    private final AuditIngestionService auditIngestionService;
    
    @PostMapping("/ingest")
    public ResponseEntity<ApiResponse<Void>> ingestAudit(@RequestBody AuditEvent request) {
        auditIngestionService.processAuditEvent(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Audit event ingested"));
    }
}
