package com.gpl.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.Instant;

@Data
public class CreateRelationshipRequest {
    @NotBlank
    private String sourceOrganizationId;
    @NotBlank
    private String targetOrganizationId;
    @NotBlank
    private String type;
    private String contractReference;
    private Instant validFrom;
    private Instant validUntil;
    private boolean isExclusive;
    private String description;
}
