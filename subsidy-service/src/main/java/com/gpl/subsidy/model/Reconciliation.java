package com.gpl.subsidy.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reconciliations")
public class Reconciliation extends BaseEntity {
    private String declarationId;
    private double trackedVolume;
    private Integer trackedBottlesOut;
    private Integer trackedBottlesIn;
    private double volumeGap;
    private double subsidyImpact;
    private String status;
    private String verifiedByPersonId;
    private Instant verifiedAt;
    private String notes;

    public String getDeclarationId() { return declarationId; }
    public void setDeclarationId(String declarationId) { this.declarationId = declarationId; }
    public double getTrackedVolume() { return trackedVolume; }
    public void setTrackedVolume(double trackedVolume) { this.trackedVolume = trackedVolume; }
    public Integer getTrackedBottlesOut() { return trackedBottlesOut; }
    public void setTrackedBottlesOut(Integer trackedBottlesOut) { this.trackedBottlesOut = trackedBottlesOut; }
    public Integer getTrackedBottlesIn() { return trackedBottlesIn; }
    public void setTrackedBottlesIn(Integer trackedBottlesIn) { this.trackedBottlesIn = trackedBottlesIn; }
    public double getVolumeGap() { return volumeGap; }
    public void setVolumeGap(double volumeGap) { this.volumeGap = volumeGap; }
    public double getSubsidyImpact() { return subsidyImpact; }
    public void setSubsidyImpact(double subsidyImpact) { this.subsidyImpact = subsidyImpact; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVerifiedByPersonId() { return verifiedByPersonId; }
    public void setVerifiedByPersonId(String verifiedByPersonId) { this.verifiedByPersonId = verifiedByPersonId; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
