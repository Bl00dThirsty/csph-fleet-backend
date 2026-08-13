package com.gpl.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "audit-service")
public interface AuditClient {
    @PostMapping("/api/v1/audit/ingest")
    void ingestAudit(@RequestBody Map<String, Object> event);
}
