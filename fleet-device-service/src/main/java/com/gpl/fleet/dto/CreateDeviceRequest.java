package com.gpl.fleet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDeviceRequest {

    @NotBlank(message = "Le numéro de série est obligatoire")
    private String serialNumber;

    @NotBlank(message = "Le type d'équipement est obligatoire")
    private String deviceType;

    private String firmwareVersion;
    private Integer batteryLevel;
    private Boolean batteryCritical;
    private String organizationId;
    private String assignedToPersonId;
    private String assignedToVehicleId;
    private String status;
    private String statusDescription;
}
