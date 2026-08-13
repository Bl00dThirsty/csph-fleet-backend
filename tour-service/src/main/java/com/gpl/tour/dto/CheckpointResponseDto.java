package com.gpl.tour.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.Instant;

/**
 * DTO de réponse complet pour un arrêt de tournée (checkpoint).
 * Expose l'intégralité des métadonnées métier et d'audit :
 * <ul>
 *   <li>Identité et lien avec la tournée parente</li>
 *   <li>Destination (site opérationnel ou site client, exclusifs)</li>
 *   <li>Séquence de passage et horodatages prévus/réels</li>
 *   <li>Statut d'avancement (PENDING → REACHED → COMPLETED / SKIPPED)</li>
 *   <li>Audit de traçabilité (createdBy, createdAt, changeby, changedate, rowStamp)</li>
 * </ul>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckpointResponseDto {

    // ─────────────────────────────────────────────
    // Identité
    // ─────────────────────────────────────────────

    /** Identifiant UUID du checkpoint (PK). */
    private String id;

    /** Identifiant UUID de la tournée à laquelle cet arrêt appartient. */
    private String tourId;

    // ─────────────────────────────────────────────
    // Destination (exclusifs)
    // ─────────────────────────────────────────────

    /**
     * Identifiant du site opérationnel de destination (dépôt, entrepôt).
     * Null si clientSiteId est renseigné.
     */
    private String siteId;

    /**
     * Identifiant du site client de destination (emplisseur, revendeur, industriel).
     * Null si siteId est renseigné.
     */
    private String clientSiteId;

    // ─────────────────────────────────────────────
    // Séquence & Horaires
    // ─────────────────────────────────────────────

    /** Numéro d'ordre de cet arrêt dans le circuit de la tournée (commence à 1). */
    private int sequence;

    /** Heure de passage prévue planifiée par le Marketeur. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant expectedArrival;

    /** Heure d'arrivée réelle enregistrée par le livreur depuis le PDA. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant actualArrival;

    // ─────────────────────────────────────────────
    // Statut (pattern Maximo)
    // ─────────────────────────────────────────────

    /**
     * Code du statut courant de l'arrêt :
     * PENDING → REACHED → COMPLETED ou SKIPPED.
     */
    private String status;

    /** Libellé lisible du statut courant. */
    private String statusDescription;

    /** Horodatage du dernier changement de statut. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant statusDate;

    /** Motif saisi par le livreur si cet arrêt a été sauté (status = SKIPPED). */
    private String skipReason;

    // ─────────────────────────────────────────────
    // Audit de traçabilité (pattern Maximo BaseEntity)
    // ─────────────────────────────────────────────

    /** Horodatage de création de l'enregistrement en base. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    /**
     * Identifiant (username ou personId) de l'utilisateur ayant créé cet arrêt.
     * Valeur SYSTEM_INIT si créé par l'initialiseur de données.
     */
    private String createdBy;

    /** Horodatage de la dernière modification (pattern Maximo changedate). */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant changedate;

    /**
     * Identifiant de l'utilisateur ayant effectué la dernière modification
     * (pattern Maximo changeby). Ex: mise à jour du statut par le livreur PDA.
     */
    private String changeby;

    /**
     * Numéro de version pour le verrouillage optimiste et la synchronisation
     * hors-ligne PDA (pattern Maximo _rowstamp). Incrémenté à chaque UPDATE.
     */
    private Long rowStamp;
}
