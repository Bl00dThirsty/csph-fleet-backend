package com.gpl.tour.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "pickup_requests")
public class PickupRequest extends AuditableEntity {
    private String marketerOrganizationId;
    private String sourceSiteId;
    private String destinationSiteId;
    private double requestedQuantity;
    private Double approvedQuantity;

    public String getMarketerOrganizationId() { return marketerOrganizationId; }
    public void setMarketerOrganizationId(String marketerOrganizationId) { this.marketerOrganizationId = marketerOrganizationId; }
    public String getSourceSiteId() { return sourceSiteId; }
    public void setSourceSiteId(String sourceSiteId) { this.sourceSiteId = sourceSiteId; }
    public String getDestinationSiteId() { return destinationSiteId; }
    public void setDestinationSiteId(String destinationSiteId) { this.destinationSiteId = destinationSiteId; }
    public double getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(double requestedQuantity) { this.requestedQuantity = requestedQuantity; }
    public Double getApprovedQuantity() { return approvedQuantity; }
    public void setApprovedQuantity(Double approvedQuantity) { this.approvedQuantity = approvedQuantity; }
}
