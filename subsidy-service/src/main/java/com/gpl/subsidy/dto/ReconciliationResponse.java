package com.gpl.subsidy.dto;

import com.gpl.subsidy.model.Reconciliation;
import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationResponse {
    private String id;
    private String declarationId;
    private double declaredVolume;
    private double trackedVolume;
    private Integer trackedBottlesOut;
    private Integer trackedBottlesIn;
    private double volumeGap;
    private double subsidyImpact;
    private String status;
    private String statusDescription;
    private String verifiedByPersonId;
    private Instant verifiedAt;
    private String notes;
    private Instant createdAt;
    private String createdBy;
    private Instant changedate;
    private String changeby;

    public static ReconciliationResponse fromEntity(Reconciliation entity, Double declaredVolume) {
        if (entity == null) return null;
        return ReconciliationResponse.builder()
                .id(entity.getId())
                .declarationId(entity.getDeclarationId())
                .declaredVolume(declaredVolume != null ? declaredVolume : 0.0)
                .trackedVolume(entity.getTrackedVolume())
                .trackedBottlesOut(entity.getTrackedBottlesOut())
                .trackedBottlesIn(entity.getTrackedBottlesIn())
                .volumeGap(entity.getVolumeGap())
                .subsidyImpact(entity.getSubsidyImpact())
                .status(entity.getStatus())
                .statusDescription(entity.getStatusDescription())
                .verifiedByPersonId(entity.getVerifiedByPersonId())
                .verifiedAt(entity.getVerifiedAt())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .changedate(entity.getChangedate())
                .changeby(entity.getChangeby())
                .build();
    }
}
