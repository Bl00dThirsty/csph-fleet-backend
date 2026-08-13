package com.gpl.tour.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "transporter_contracts")
public class TransporterContract extends AuditableEntity {
    private String marketerOrganizationId;
    private String transporterOrganizationId;
    private boolean isPrimary;
    private String contractReference;
    private Instant startedAt;
    private Instant endedAt;
    private boolean isActive = true;

    public String getMarketerOrganizationId() { return marketerOrganizationId; }
    public void setMarketerOrganizationId(String marketerOrganizationId) { this.marketerOrganizationId = marketerOrganizationId; }
    public String getTransporterOrganizationId() { return transporterOrganizationId; }
    public void setTransporterOrganizationId(String transporterOrganizationId) { this.transporterOrganizationId = transporterOrganizationId; }
    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
    public String getContractReference() { return contractReference; }
    public void setContractReference(String contractReference) { this.contractReference = contractReference; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
