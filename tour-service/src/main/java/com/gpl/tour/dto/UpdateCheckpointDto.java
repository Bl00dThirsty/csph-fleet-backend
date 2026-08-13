package com.gpl.tour.dto;

import lombok.*;

import java.time.Instant;

/**
 * DTO pour la mise à jour partielle d'un arrêt de tournée (checkpoint).
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCheckpointDto {

    private String siteId;

    private String clientSiteId;

    private Integer sequence;

    private Instant expectedArrival;

    private Instant actualArrival;

    private String status;

    private String skipReason;
}
