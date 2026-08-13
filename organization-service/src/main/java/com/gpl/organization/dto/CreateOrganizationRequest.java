package com.gpl.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrganizationRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String type;
    @NotBlank
    private String tier;
    private String classStructureId;
    private String parentOrganizationId;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private boolean isHeadquarters;
}
