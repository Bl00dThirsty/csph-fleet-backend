package com.gpl.fleet.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignDeviceRequest {

    private String assignedToPersonId;
    private String assignedToVehicleId;
}
