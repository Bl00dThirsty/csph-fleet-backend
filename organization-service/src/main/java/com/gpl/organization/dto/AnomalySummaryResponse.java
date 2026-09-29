package com.gpl.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalySummaryResponse {
    private UUID id;
    private String type;
    private String category;
    private String severity;
    private String status;
    private String entityType;
    private String entityId;
    private UUID siteId;
    private UUID clientSiteId;
    private String assignedToGroup;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
