package com.gpl.tour.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class PickupRequestResponseDto {
    private String id;
    private String marketerOrganizationId;
    private String sourceSiteId;
    private String destinationSiteId;
    private double requestedQuantity;
    private Double approvedQuantity;
    private String status;
    private String statusDescription;
    private Instant statusDate;
    private String createdBy;
    private Instant createdAt;
    private Instant changedate;
}
