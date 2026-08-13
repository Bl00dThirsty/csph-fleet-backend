package com.gpl.cylinder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferCylinderRequest {

    @NotBlank(message = "Le site de destination (targetSiteId) est obligatoire")
    private String targetSiteId;

    private String targetHolderOrganizationId;

    private String fillStatus;

    private String notes;
}
