package com.gpl.cylinder.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "rfid_tags")
public class RfidTag extends AuditableEntity {
    private String tagUid;
    private String bottleSerial;
    private String currentSiteId;
    private String currentClientSiteId;

    public String getTagUid() { return tagUid; }
    public void setTagUid(String tagUid) { this.tagUid = tagUid; }
    public String getBottleSerial() { return bottleSerial; }
    public void setBottleSerial(String bottleSerial) { this.bottleSerial = bottleSerial; }
    public String getCurrentSiteId() { return currentSiteId; }
    public void setCurrentSiteId(String currentSiteId) { this.currentSiteId = currentSiteId; }
    public String getCurrentClientSiteId() { return currentClientSiteId; }
    public void setCurrentClientSiteId(String currentClientSiteId) { this.currentClientSiteId = currentClientSiteId; }
}
