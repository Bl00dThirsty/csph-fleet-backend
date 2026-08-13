package com.gpl.organization.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class RelationshipResponse {
    private String id;
    private String sourceOrganizationId;
    private String targetOrganizationId;
    private String sourceOrganizationName;
    private String targetOrganizationName;
    private String type;
    private String typeDescription;
    private String contractReference;
    private Instant validFrom;
    private Instant validUntil;
    private boolean isActive;
    private boolean isExclusive;
    private String description;
    private String status;
}
