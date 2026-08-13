package com.gpl.fleet.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDeviceRequest {

    private String serialNumber;
    private String deviceType;
    private String firmwareVersion;
    private Integer batteryLevel;
    private Boolean batteryCritical;
    private Instant lastSync;
    private Double lastLatitude;
    private Double lastLongitude;
    private String organizationId;
    private String assignedToPersonId;
    private String assignedToVehicleId;
    private String status;
    private String statusDescription;
}
