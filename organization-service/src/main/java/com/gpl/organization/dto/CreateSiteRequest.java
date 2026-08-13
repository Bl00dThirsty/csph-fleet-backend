package com.gpl.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSiteRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String siteId;
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String organizationId;
    @NotBlank
    private String type;
    private String addressLine1;
    private String city;
    private String region;
    private Double latitude;
    private Double longitude;
    private Integer geofenceRadiusMeters;
    private Double storageCapacityTons;
    private Integer maxVehicleBays;
}
