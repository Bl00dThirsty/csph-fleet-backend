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
public class AuditQueryParams {
    private String entityType;
    private String entityId;
    private String actorPersonId;
    private String action;
    private String module;
    private Instant dateFrom;
    private Instant dateTo;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
