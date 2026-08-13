package com.gpl.organization.dto;

import lombok.Data;

@Data
public class UpdateSiteRequest {
    private String name;
    private String description;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String region;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private Integer geofenceRadiusMeters;
    private String operatingHoursStart;
    private String operatingHoursEnd;
}
