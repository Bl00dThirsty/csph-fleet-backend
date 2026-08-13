package com.gpl.fleet.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVehicleRequest {

    private String licensePlate;
    private String type;
    private String organizationId;
    private Double maxVolume;
    private Integer maxBottleCount;
    private String certificateUrl;
    private String certificateNumber;
    private Instant certificateExpiryAt;
    private Double tareWeight;
    private Boolean isActive;
    private String status;
    private String statusDescription;
}
