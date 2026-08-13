package com.gpl.tour.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.Instant;

@Data
public class CreateTransporterContractDto {
    @NotBlank(message = "Marketer Organization ID is required")
    private String marketerOrganizationId;

    @NotBlank(message = "Transporter Organization ID is required")
    private String transporterOrganizationId;

    @NotBlank(message = "Contract Reference is required")
    private String contractReference;

    private boolean isPrimary;
    private Instant startedAt;
    private Instant endedAt;
}
