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
public class StatusHistoryResponse {
    private String id;
    private String entityType;
    private String entityId;
    private String entityName;
    private String previousStatus;
    private String previousStatusDescription;
    private String newStatus;
    private String newStatusDescription;
    private String changeby;
    private String changebyDisplayName;
    private Instant changedate;
    private String reason;
    private Boolean isAutomatic;
}
