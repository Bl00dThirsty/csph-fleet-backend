package com.gpl.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a Tour.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTourDto {

    private String tourCode;
    private String marketerOrganizationId;
    private String executionMode;
    private String type;
    private Double requestedQuantity;
    private String transporterOrganizationId;
    private String vehicleId;
    private String driverId;
    private String livreurPersonId;
}
