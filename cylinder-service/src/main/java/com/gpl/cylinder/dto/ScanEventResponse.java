package com.gpl.cylinder.dto;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanEventResponse {
    private String id;
    private Long rowStamp;
    private String checkpointId;
    private String driverPersonId;
    private String rfidTagId;
    private String direction;
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
    private Double meterReading;
    private String photoUrl;
    private String pdaSyncId;
    private String conflictStatus;
    private Instant createdAt;
    private String createdBy;
    private Instant changedate;
    private String changeby;
}
