package com.gpl.tour.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class TransporterContractResponseDto {
    private String id;
    private String marketerOrganizationId;
    private String transporterOrganizationId;
    private String contractReference;
    private boolean isPrimary;
    private Instant startedAt;
    private Instant endedAt;
    private boolean isActive;
    private String createdBy;
    private Instant createdAt;
    private Instant changedate;
}
