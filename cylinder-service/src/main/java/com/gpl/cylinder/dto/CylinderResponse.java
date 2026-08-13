package com.gpl.cylinder.dto;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CylinderResponse {
    private String id;
    private Long rowStamp;
    private String serialNumber;
    private String barcode;
    private String cylinderTypeId;
    private String ownerOrganizationId;
    private String currentHolderOrganizationId;
    private String currentSiteId;
    private String fillStatus;
    private String brand;
    private Double capacityKg;
    private String status;
    private String statusDescription;
    private Instant statusDate;
    private Instant createdAt;
    private String createdBy;
    private Instant changedate;
    private String changeby;
}
