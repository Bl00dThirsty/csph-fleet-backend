package com.gpl.tour.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO de réponse complet pour une tournée de livraison.
 * Expose l'intégralité des métadonnées métier et d'audit :
 * <ul>
 *   <li>Identité et cycle de vie de la tournée</li>
 *   <li>Affectation logistique (véhicule, chauffeur, transporteur)</li>
 *   <li>Quantités (demandée, chargée, livrée)</li>
 *   <li>Horodatages opérationnels (départ, clôture)</li>
 *   <li>Audit de traçabilité (createdBy, createdAt, changeby, changedate, rowStamp)</li>
 *   <li>Statut courant avec date de changement (pattern Maximo)</li>
 *   <li>Liste des arrêts ordonnés (checkpoints)</li>
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
public class TourResponseDto {

    // ─────────────────────────────────────────────
    // Identité
    // ─────────────────────────────────────────────

    /** Identifiant UUID de la tournée (PK). */
    private String id;

    /** Code fonctionnel lisible (ex: T-VRAC-2026-001). */
    private String tourCode;

    // ─────────────────────────────────────────────
    // Logistique & Affectation
    // ─────────────────────────────────────────────

    /** Identifiant de l'organisation marketeur (société pétrolière). */
    private String marketerOrganizationId;

    /** Mode d'exécution : INTERNAL (propre flotte) ou EXTERNAL (sous-traitance). */
    private String executionMode;

    /** Identifiant de l'organisation transporteur (si mode EXTERNAL). */
    private String transporterOrganizationId;

    /** Identifiant UUID du véhicule assigné à cette tournée. */
    private String vehicleId;

    /** Identifiant UUID du chauffeur assigné (Driver). */
    private String driverId;

    /** Identifiant UUID de l'utilisateur livreur sur PDA. */
    private String livreurPersonId;

    /** Identifiant de l'utilisateur transporteur ayant confirmé l'affectation. */
    private String assignedByTransporterPersonId;

    /** Horodatage de la confirmation d'affectation par le transporteur. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant transporterAssignedAt;

    // ─────────────────────────────────────────────
    // Type & Quantités
    // ─────────────────────────────────────────────

    /** Type de tournée : VRAC (gaz liquide) ou BOUTEILLES50KG. */
    private String type;

    /** Quantité demandée en kg lors de la création de la tournée. */
    private double requestedQuantity;

    /** Quantité effectivement chargée en kg au dépôt (renseignée au départ). */
    private Double loadedQuantity;

    /** Quantité effectivement livrée en kg (cumulée sur tous les checkpoints). */
    private Double deliveredQuantity;

    // ─────────────────────────────────────────────
    // Cycle de vie opérationnel
    // ─────────────────────────────────────────────

    /** Horodatage de départ réel de la tournée (mise en route du véhicule). */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startedAt;

    /** Horodatage de clôture de la tournée (retour dépôt ou annulation). */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant closedAt;

    // ─────────────────────────────────────────────
    // Statut (pattern Maximo status + status_description + statusdate)
    // ─────────────────────────────────────────────

    /** Code du statut courant (DRAFT, PLANNED, INPROGRESS, CLOSED, CANCELLED...). */
    private String status;

    /** Libellé lisible du statut courant. */
    private String statusDescription;

    /** Horodatage du dernier changement de statut. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant statusDate;

    // ─────────────────────────────────────────────
    // Audit de traçabilité (pattern Maximo BaseEntity)
    // ─────────────────────────────────────────────

    /** Horodatage de création de l'enregistrement en base. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    /**
     * Identifiant (username ou personId) de l'utilisateur ayant créé la tournée.
     * Valeur SYSTEM_INIT si créée par l'initialiseur de données.
     */
    private String createdBy;

    /** Horodatage de la dernière modification (pattern Maximo changedate). */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant changedate;

    /**
     * Identifiant (username ou personId) de l'utilisateur ayant effectué
     * la dernière modification (pattern Maximo changeby).
     */
    private String changeby;

    /**
     * Numéro de version pour le verrouillage optimiste et la synchronisation
     * hors-ligne (pattern Maximo _rowstamp). Incrémenté à chaque UPDATE.
     */
    private Long rowStamp;

    // ─────────────────────────────────────────────
    // Arrêts de la tournée
    // ─────────────────────────────────────────────

    /** Liste ordonnée des arrêts (checkpoints) de la tournée, triés par séquence. */
    private List<CheckpointResponseDto> checkpoints;
}
