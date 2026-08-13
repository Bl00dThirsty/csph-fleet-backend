package com.gpl.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Extension de BaseEntity pour les entités ayant un statut.
 * Ajoute le pattern dual Maximo : status (code) + statusDescription (libellé).
 *
 * Le champ statusDate enregistre le dernier changement de statut,
 * tandis que l'historique complet des statuts est conservé
 * dans la table status_history via le service d'audit.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {

    /** Code du statut courant (ACTIVE, INACTIVE, SUSPENDED, PENDAPPR...). */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    /** Libellé lisible du statut (pattern Maximo status_description). */
    @Column(name = "status_description", nullable = false, length = 100)
    private String statusDescription = "Actif";

    /** Timestamp du dernier changement de statut (pattern Maximo statusdate). */
    @Column(name = "status_date")
    private Instant statusDate;

    /**
     * Met à jour le statut et enregistre la date de changement.
     */
    public void updateStatus(String newStatus, String newStatusDescription) {
        this.status = newStatus;
        this.statusDescription = newStatusDescription;
        this.statusDate = Instant.now();
    }
}
