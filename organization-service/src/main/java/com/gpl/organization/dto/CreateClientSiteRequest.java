package com.gpl.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateClientSiteRequest {
    @NotBlank(message = "Site ID is required")
    private String siteId;

    @NotBlank(message = "Client Organization ID is required")
    private String clientOrganizationId;

    private String siteContactPersonId;
    private String deliveryInstructions;
    private String specificRequirements;
    private boolean requiresAuthorization;
    private String operatingConstraints;
}
