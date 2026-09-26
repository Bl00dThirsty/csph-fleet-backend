package com.gpl.tour.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a Tour.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTourDto {

    @NotBlank
    private String tourCode;

    @NotBlank
    private String marketerOrganizationId;

    @NotBlank
    private String executionMode;

    @NotBlank
    private String type;

    @NotNull
    @Positive
    private Double requestedQuantity;

    private String transporterOrganizationId;
    private String vehicleId;
    private String driverId;
    private String driverPersonId;
}
