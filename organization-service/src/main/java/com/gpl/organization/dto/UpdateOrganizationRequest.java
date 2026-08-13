package com.gpl.organization.dto;

import lombok.Data;

@Data
public class UpdateOrganizationRequest {
    private String name;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private String logoUrl;
    private String taxId;
    private String registrationNumber;
    private String currency;
    private String language;
    private String timezone;
    private Boolean isHeadquarters;
}
