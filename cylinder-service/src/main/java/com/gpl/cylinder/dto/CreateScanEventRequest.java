package com.gpl.cylinder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateScanEventRequest {
    private String checkpointId;
    private String livreurPersonId;
    @NotBlank(message = "L'ID du tag RFID est obligatoire")
    private String rfidTagId;
    private String direction;
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
    private Double meterReading;
    private String photoUrl;
    private String pdaSyncId;
    private String conflictStatus;
}
