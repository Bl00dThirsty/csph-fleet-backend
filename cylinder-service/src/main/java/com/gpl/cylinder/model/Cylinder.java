package com.gpl.cylinder.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "cylinders")
public class Cylinder extends AuditableEntity {
    private String serialNumber;
    private String barcode;
    private String cylinderTypeId;
    private String ownerOrganizationId;
    private String currentHolderOrganizationId;
    private String currentSiteId;
    private String fillStatus;

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getCylinderTypeId() { return cylinderTypeId; }
    public void setCylinderTypeId(String cylinderTypeId) { this.cylinderTypeId = cylinderTypeId; }
    public String getOwnerOrganizationId() { return ownerOrganizationId; }
    public void setOwnerOrganizationId(String ownerOrganizationId) { this.ownerOrganizationId = ownerOrganizationId; }
    public String getCurrentHolderOrganizationId() { return currentHolderOrganizationId; }
    public void setCurrentHolderOrganizationId(String currentHolderOrganizationId) { this.currentHolderOrganizationId = currentHolderOrganizationId; }
    public String getCurrentSiteId() { return currentSiteId; }
    public void setCurrentSiteId(String currentSiteId) { this.currentSiteId = currentSiteId; }
    public String getFillStatus() { return fillStatus; }
    public void setFillStatus(String fillStatus) { this.fillStatus = fillStatus; }
}
