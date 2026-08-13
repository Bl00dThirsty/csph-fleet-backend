package com.gpl.tour.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entité représentant un arrêt / point de contrôle d'une tournée de livraison.
 * Un checkpoint est associé soit à un site opérationnel (dépôt, entrepôt),
 * soit à un site client (emplisseur, revendeur, industriel).
 *
 * <p>Contraintes d'exclusivité :</p>
 * <ul>
 *   <li>Un checkpoint ne peut pas avoir à la fois un siteId et un clientSiteId</li>
 *   <li>Un checkpoint doit obligatoirement avoir l'un ou l'autre</li>
 * </ul>
 *
 * <p>Cycle de vie du statut :</p>
 * <ul>
 *   <li>PENDING : En attente de visite</li>
 *   <li>REACHED : Le véhicule est arrivé sur site</li>
 *   <li>COMPLETED : La livraison est terminée sur ce site</li>
 *   <li>SKIPPED : L'arrêt a été sauté (avec motif obligatoire)</li>
 * </ul>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@Entity
@Table(name = "checkpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checkpoint extends AuditableEntity {

    /* Identifiant de la tournée parente */
    @Column(name = "tournee_id", nullable = false)
    private String tourId;

    /* Identifiant du site opérationnel (exclusif avec clientSiteId) */
    @Column(name = "site_id")
    private String siteId;

    /* Identifiant du site client (exclusif avec siteId) */
    @Column(name = "client_site_id")
    private String clientSiteId;

    /* Numéro de passage dans l'ordre de la tournée (commence à 1) */
    @Column(nullable = false)
    private int sequence;

    /* Heure d'arrivée prévue sur ce point */
    @Column(name = "expected_arrival")
    private Instant expectedArrival;

    /* Heure d'arrivée réelle enregistrée par le livreur */
    @Column(name = "actual_arrival")
    private Instant actualArrival;

    /* Motif de saut de cet arrêt (obligatoire si status=SKIPPED) */
    @Column(name = "skip_reason")
    private String skipReason;
}
