package com.gpl.subsidy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReconciliationRequest {
    @NotBlank(message = "declarationId is required")
    private String declarationId;
    private double trackedVolume;
    private Integer trackedBottlesOut;
    private Integer trackedBottlesIn;
    private Double subsidyRate;
    private String verifiedByPersonId;
    private String notes;
}
