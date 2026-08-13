package com.gpl.subsidy.dto;

import com.gpl.subsidy.model.Declaration;
import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclarationResponse {
    private String id;
    private String marketerOrganizationId;
    private String declaringOrganizationId;
    private String siteId;
    private Instant periodStart;
    private Instant periodEnd;
    private double declaredVolume;
    private String status;
    private String statusDescription;
    private Instant statusDate;
    private String submittedByPersonId;
    private Instant createdAt;
    private String createdBy;
    private Instant changedate;
    private String changeby;

    public static DeclarationResponse fromEntity(Declaration entity) {
        if (entity == null) return null;
        String orgId = entity.getEffectiveOrganizationId();
        return DeclarationResponse.builder()
                .id(entity.getId())
                .marketerOrganizationId(orgId)
                .declaringOrganizationId(orgId)
                .siteId(entity.getSiteId())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .declaredVolume(entity.getDeclaredVolume())
                .status(entity.getStatus())
                .statusDescription(entity.getStatusDescription())
                .statusDate(entity.getStatusDate())
                .submittedByPersonId(entity.getSubmittedByPersonId())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .changedate(entity.getChangedate())
                .changeby(entity.getChangeby())
                .build();
    }
}
