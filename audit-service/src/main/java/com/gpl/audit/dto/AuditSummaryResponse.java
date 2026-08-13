package com.gpl.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSummaryResponse {
    private String id;
    private String entityType;
    private String entityId;
    private String entityName;
    private int totalModifications;
    private Instant lastModifiedAt;
    private String lastModifiedBy;
    private String lastModifiedByDisplayName;
    private String lastAction;
    private String lastActionDescription;
    private Instant firstCreatedAt;
    private String firstCreatedBy;
    private String firstCreatedByDisplayName;
}
