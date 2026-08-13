package com.gpl.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityModificationResponse {
    private String modificationId;
    private String entityType;
    private String entityId;
    private String entityName;
    private String action;
    private String actionDescription;
    private String module;
    private String description;
    private String changeby;
    private String changebyDisplayName;
    private String actorOrgId;
    private String actorSiteId;
    private Instant changedate;
    private String deviceInfo;
    private String appVersion;
    private List<FieldChangeResponse> changes;
}
