package com.gpl.tour.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de réponse pour un événement de scan RFID.
 *
 * <p>Expose le point géographique sous forme de deux doubles ({@code geoLng},
 * {@code geoLat}) — le {@code JTS Point} interne n'est jamais sérialisé.</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   28.09.2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanEventResponseDto {

    /** Identifiant UUID du scan (PK). */
    private UUID id;

    /** Identifiant du checkpoint associé. */
    private UUID checkpointId;

    /** Identifiant du livreur. */
    private UUID livreurUserId;

    /** UID RFID lu (optionnel). */
    private UUID rfidTagId;

    /** Direction du scan : {@code IN} ou {@code OUT}. */
    private String direction;

    /** Longitude GPS du point de scan. */
    private Double geoLng;

    /** Latitude GPS du point de scan. */
    private Double geoLat;

    /** Index compteur volumétrique pour VRAC. */
    private Double meterReading;

    /** URL photo preuve. */
    private String photoUrl;

    /** Identifiant de synchronisation offline PDA. */
    private String pdaSyncId;

    /** Horodatage effectif du scan. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    /** Horodatage de création de la ligne en base. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    /** Statut de conflit éventuel (peut être null). */
    private String conflictStatus;
}