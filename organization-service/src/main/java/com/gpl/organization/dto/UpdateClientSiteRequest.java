package com.gpl.organization.dto;

import lombok.Data;

@Data
public class UpdateClientSiteRequest {
    private String siteContactPersonId;
    private String deliveryInstructions;
    private String specificRequirements;
    private Boolean requiresAuthorization;
    private String operatingConstraints;
}
