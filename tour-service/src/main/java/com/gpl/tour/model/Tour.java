package com.gpl.tour.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entité représentant une tournée de livraison GPL (Gaz de Pétrole Liquéfié).
 * Une tournée peut être de type VRAC (camion-citerne) ou BOUTEILLES50KG (camion-plateau).
 * Elle peut comporter plusieurs arrêts ({@link Checkpoint}) pour des clients différents.
 *
 * <p>Types de tournée :</p>
 * <ul>
 *   <li>VRAC : Livraison en vrac, gaz liquide en tonnes (camion-citerne)</li>
 *   <li>BOUTEILLES50KG : Livraison de bouteilles de 50kg (camion plateau)</li>
 * </ul>
 *
 * <p>Modes d'exécution :</p>
 * <ul>
 *   <li>INTERNAL : Tournée gérée en propre par le marketeur (propre flotte)</li>
 *   <li>EXTERNAL : Tournée sous-traitée à un transporteur externe</li>
 * </ul>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@Entity
@Table(name = "delivery_tours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tour extends AuditableEntity {

    /* Code fonctionnel unique de la tournée (ex: T-VRAC-2026-001) */
    @Column(name = "tour_code")
    private String tourCode;

    /* Identifiant de l'organisation marketeur (ex: MKT-GPL) */
    @Column(name = "marketeur_org_id", nullable = false)
    private String marketerOrganizationId;

    /* Mode d'exécution : INTERNAL ou EXTERNAL */
    @Column(name = "execution_mode", nullable = false)
    private String executionMode;

    /* Identifiant de l'organisation transporteur (si EXTERNAL) */
    @Column(name = "transporter_org_id")
    private String transporterOrganizationId;

    /* Identifiant du véhicule assigné */
    @Column(name = "vehicle_id")
    private String vehicleId;

    /* Identifiant du chauffeur */
    @Column(name = "driver_id")
    private String driverId;

    /* Identifiant de l'utilisateur livreur (PDA) */
    @Column(name = "livreur_user_id")
    private String livreurPersonId;

    /* Identifiant de l'utilisateur transporteur ayant confirmé l'affectation */
    @Column(name = "assigned_by_transporter_user_id")
    private String assignedByTransporterPersonId;

    /* Date/heure de confirmation du transporteur */
    @Column(name = "transporter_assigned_at")
    private Instant transporterAssignedAt;

    /* Type de tournée : VRAC ou BOUTEILLES50KG */
    @Column(name = "type", nullable = false)
    private String type;

    /* Quantité demandée en kg */
    @Column(name = "requested_quantity", nullable = false)
    private double requestedQuantity;

    /* Quantité effectivement chargée en kg */
    @Column(name = "loaded_quantity")
    private Double loadedQuantity;

    /* Quantité effectivement livrée en kg */
    @Column(name = "delivered_quantity")
    private Double deliveredQuantity;

    /* Date/heure de départ de la tournée */
    @Column(name = "started_at")
    private Instant startedAt;

    /* Date/heure de clôture de la tournée */
    @Column(name = "closed_at")
    private Instant closedAt;
}
