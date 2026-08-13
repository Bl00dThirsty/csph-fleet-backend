package com.gpl.subsidy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDeclarationRequest {
    private String marketerOrganizationId;
    private String declaringOrganizationId;
    private String siteId;
    @NotNull(message = "periodStart is required")
    private Instant periodStart;
    @NotNull(message = "periodEnd is required")
    private Instant periodEnd;
    private double declaredVolume;
    private String submittedByPersonId;

    public String getEffectiveOrganizationId() {
        return declaringOrganizationId != null ? declaringOrganizationId : marketerOrganizationId;
    }
}
