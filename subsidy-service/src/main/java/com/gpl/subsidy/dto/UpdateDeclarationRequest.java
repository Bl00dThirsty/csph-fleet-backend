package com.gpl.subsidy.dto;

import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDeclarationRequest {
    private String marketerOrganizationId;
    private String declaringOrganizationId;
    private String siteId;
    private Instant periodStart;
    private Instant periodEnd;
    private Double declaredVolume;
    private String status;
    private String submittedByPersonId;

    public String getEffectiveOrganizationId() {
        return declaringOrganizationId != null ? declaringOrganizationId : marketerOrganizationId;
    }
}
