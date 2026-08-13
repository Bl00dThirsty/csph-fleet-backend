package com.gpl.subsidy.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "reconciliations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reconciliation extends AuditableEntity {

    @Column(name = "declaration_id", nullable = false)
    private String declarationId;

    @Column(name = "tracked_volume")
    private double trackedVolume;

    @Column(name = "tracked_bottles_out")
    private Integer trackedBottlesOut;

    @Column(name = "tracked_bottles_in")
    private Integer trackedBottlesIn;

    @Column(name = "volume_gap")
    private double volumeGap;

    @Column(name = "subsidy_impact")
    private double subsidyImpact;

    @Column(name = "verified_by")
    private String verifiedByPersonId;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "notes", length = 1000)
    private String notes;
}
