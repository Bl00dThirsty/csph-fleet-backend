package com.gpl.tour.dto;

import lombok.*;

import java.time.Instant;

/**
 * DTO pour la mise à jour partielle d'un arrêt de tournée (checkpoint).
 *
 * <p><strong>Deliberately carries no {@code status} and no {@code actualArrival}.</strong>
 * Both were bypasses of the Flux 2 lifecycle: a {@code PUT} carrying {@code status}
 * could set any lifecycle state directly, and one carrying {@code actualArrival}
 * could backdate the arrival evidence that only the reach transition may capture.
 * A client wanting to move the stop must call
 * {@code POST /api/v1/checkpoints/{id}/reach}, {@code .../complete} or
 * {@code .../skip} — each of which is guarded by the status domain.</p>
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

    private String skipReason;
}
