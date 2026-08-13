package com.gpl.fleet.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {

    private String id;
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

    // AuditableEntity metadata fields
    private String status;
    private String statusDescription;
    private Instant statusDate;
    private Instant createdAt;
    private String createdBy;
    private String changeby;
    private Instant changedate;
    private Long rowStamp;
}
