package com.gpl.tour.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO d'entrée pour l'enregistrement d'un événement de scan RFID.
 *
 * <p>Le point géographique est envoyé sous forme de deux doubles ({@code geoLng},
 * {@code geoLat}) pour rester compatible JSON — la conversion en
 * {@code JTS Point} (PostGIS) est effectuée côté service.</p>
 *
 * <p>Règle métier : exactement un de {@code direction} ou {@code meterReading}
 * doit être renseigné. Un scan avec {@code direction} enregistre une entrée/sortie
 * RFID ; un scan avec {@code meterReading} enregistre une mesure volumétrique VRAC.
 * La validation est faite dans le service (cf. {@code ScanEventServiceImpl}).</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   28.09.2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScanEventDto {

    /** Identifiant du checkpoint sur lequel le scan est effectué. */
    @NotNull
    private UUID checkpointId;

    /** Identifiant du livreur (utilisateur PDA) effectuant le scan. */
    @NotNull
    private UUID livreurUserId;

    /** UID RFID lu (optionnel — null si lecture code-barres ou scan manuel). */
    private UUID rfidTagId;

    /** Direction du scan : {@code "IN"} (entrée) ou {@code "OUT"} (sortie). Mutuellement exclusif avec {@code meterReading}. */
    private String direction;

    /** Longitude GPS du point de scan (WGS 84). */
    @NotNull
    private Double geoLng;

    /** Latitude GPS du point de scan (WGS 84). */
    @NotNull
    private Double geoLat;

    /** Index compteur volumétrique pour les transports VRAC. Mutuellement exclusif avec {@code direction}. Doit être ≥ 0. */
    private Double meterReading;

    /** URL de la photo preuve (optionnel). */
    private String photoUrl;

    /** Identifiant de synchronisation offline émis par le PDA — pour la déduplication à la resynchronisation. */
    private String pdaSyncId;

    /** Horodatage du scan (PDA). Si null, le serveur applique {@code Instant.now()}. */
    private Instant timestamp;
}