package com.gpl.cylinder.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "scan_events")
@Getter
@Setter
@NoArgsConstructor
public class ScanEvent extends BaseEntity {
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
}
