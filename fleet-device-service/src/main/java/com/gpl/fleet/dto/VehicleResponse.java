package com.gpl.fleet.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    private String id;
    private String licensePlate;
    private String type;
    private String organizationId;
    private Double maxVolume;
    private Integer maxBottleCount;
    private String certificateUrl;
    private String certificateNumber;
    private Instant certificateExpiryAt;
    private Double tareWeight;
    private boolean isActive;

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
