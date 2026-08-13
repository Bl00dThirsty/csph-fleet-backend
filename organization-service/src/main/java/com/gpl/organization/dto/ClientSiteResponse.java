package com.gpl.organization.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ClientSiteResponse {
    private String id;
    private String siteId;
    private String siteCode;
    private String siteName;
    private String clientOrganizationId;
    private String clientOrganizationName;
    private String siteContactPersonId;
    private String deliveryInstructions;
    private String specificRequirements;
    private boolean requiresAuthorization;
    private String operatingConstraints;
    private Instant createdAt;
    private Instant updatedAt;
}
