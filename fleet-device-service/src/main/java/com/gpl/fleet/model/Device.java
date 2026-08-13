package com.gpl.fleet.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device extends AuditableEntity {
    private String serialNumber;
    private String deviceType;
    private String firmwareVersion;
    private Integer batteryLevel;
    private boolean batteryCritical;
    private Instant lastSync;
    private Double lastLatitude;
    private Double lastLongitude;
    private String assignedToPersonId;
    private String assignedToVehicleId;
    private String organizationId;

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }
    public boolean isBatteryCritical() { return batteryCritical; }
    public void setBatteryCritical(boolean batteryCritical) { this.batteryCritical = batteryCritical; }
    public Instant getLastSync() { return lastSync; }
    public void setLastSync(Instant lastSync) { this.lastSync = lastSync; }
    public Double getLastLatitude() { return lastLatitude; }
    public void setLastLatitude(Double lastLatitude) { this.lastLatitude = lastLatitude; }
    public Double getLastLongitude() { return lastLongitude; }
    public void setLastLongitude(Double lastLongitude) { this.lastLongitude = lastLongitude; }
    public String getAssignedToPersonId() { return assignedToPersonId; }
    public void setAssignedToPersonId(String assignedToPersonId) { this.assignedToPersonId = assignedToPersonId; }
    public String getAssignedToVehicleId() { return assignedToVehicleId; }
    public void setAssignedToVehicleId(String assignedToVehicleId) { this.assignedToVehicleId = assignedToVehicleId; }
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
}
