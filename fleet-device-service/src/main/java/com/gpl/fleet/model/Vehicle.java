package com.gpl.fleet.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vehicles")
public class Vehicle extends AuditableEntity {
    private String licensePlate;
    private String type;
    private String organizationId;
    private Double maxVolume;
    private Integer maxBottleCount;
    private String certificateUrl;
    private String certificateNumber;
    private Instant certificateExpiryAt;
    private Double tareWeight;
    private boolean isActive = true;

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public Double getMaxVolume() { return maxVolume; }
    public void setMaxVolume(Double maxVolume) { this.maxVolume = maxVolume; }
    public Integer getMaxBottleCount() { return maxBottleCount; }
    public void setMaxBottleCount(Integer maxBottleCount) { this.maxBottleCount = maxBottleCount; }
    public String getCertificateUrl() { return certificateUrl; }
    public void setCertificateUrl(String certificateUrl) { this.certificateUrl = certificateUrl; }
    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }
    public Instant getCertificateExpiryAt() { return certificateExpiryAt; }
    public void setCertificateExpiryAt(Instant certificateExpiryAt) { this.certificateExpiryAt = certificateExpiryAt; }
    public Double getTareWeight() { return tareWeight; }
    public void setTareWeight(Double tareWeight) { this.tareWeight = tareWeight; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
