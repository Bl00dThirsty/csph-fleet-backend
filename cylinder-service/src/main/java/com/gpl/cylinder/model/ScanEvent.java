package com.gpl.cylinder.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "scan_events")
public class ScanEvent extends BaseEntity {
    private String checkpointId;
    private String livreurPersonId;
    private String rfidTagId;
    private String direction;
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
    private Double meterReading;
    private String photoUrl;
    private String pdaSyncId;
    private String conflictStatus;

    public String getCheckpointId() { return checkpointId; }
    public void setCheckpointId(String checkpointId) { this.checkpointId = checkpointId; }
    public String getLivreurPersonId() { return livreurPersonId; }
    public void setLivreurPersonId(String livreurPersonId) { this.livreurPersonId = livreurPersonId; }
    public String getRfidTagId() { return rfidTagId; }
    public void setRfidTagId(String rfidTagId) { this.rfidTagId = rfidTagId; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Double getMeterReading() { return meterReading; }
    public void setMeterReading(Double meterReading) { this.meterReading = meterReading; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getPdaSyncId() { return pdaSyncId; }
    public void setPdaSyncId(String pdaSyncId) { this.pdaSyncId = pdaSyncId; }
    public String getConflictStatus() { return conflictStatus; }
    public void setConflictStatus(String conflictStatus) { this.conflictStatus = conflictStatus; }
}
