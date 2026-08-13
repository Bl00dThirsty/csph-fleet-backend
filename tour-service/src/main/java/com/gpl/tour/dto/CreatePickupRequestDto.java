package com.gpl.tour.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePickupRequestDto {
    @NotBlank(message = "Marketer Organization ID is required")
    private String marketerOrganizationId;

    @NotBlank(message = "Source Site (Depot SNH/SCDP) ID is required")
    private String sourceSiteId;

    @NotBlank(message = "Destination Site ID is required")
    private String destinationSiteId;

    @Min(value = 1, message = "Requested quantity must be greater than 0")
    private double requestedQuantity;
}
