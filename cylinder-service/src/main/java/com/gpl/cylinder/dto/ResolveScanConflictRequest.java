package com.gpl.cylinder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveScanConflictRequest {

    @NotBlank(message = "L'action de résolution (resolutionAction) est obligatoire")
    private String resolutionAction;

    private String notes;
}
