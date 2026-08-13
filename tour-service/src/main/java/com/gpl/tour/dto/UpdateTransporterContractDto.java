package com.gpl.tour.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class UpdateTransporterContractDto {
    private String contractReference;
    private Boolean isPrimary;
    private Instant startedAt;
    private Instant endedAt;
    private Boolean isActive;
}
