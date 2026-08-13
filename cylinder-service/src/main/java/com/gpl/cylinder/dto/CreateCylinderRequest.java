package com.gpl.cylinder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCylinderRequest {
    @NotBlank(message = "Le numéro de série est obligatoire")
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
}
