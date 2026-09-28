package com.gpl.common.model;

import com.gpl.common.lifecycle.Lifecycle;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
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
 *
 * <p><strong>Écriture du statut : {@link #updateStatus} uniquement.</strong>
 * Le setter Lombok de {@code status} est désactivé. La lecture reste publique
 * partout, mais écrire un statut exige le couple (code, libellé) — c'est ce qui
 * rend impossible d'insérer un code sans son libellé, ou de laisser le libellé
 * et la date derrière lui. Un appelant qui veut contourner le cycle de vie n'a
 * plus de porte : {@code PUT /api/v1/checkpoints/{id}} avec un champ
 * {@code status} n'est plus compilable.</p>
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {

    /**
     * Code du statut courant (ACTIVE, INACTIVE, SUSPENDED, PENDAPPR...).
     *
     * <p>La largeur vient de {@link Lifecycle#MAX_STATUS_LENGTH} et non d'un
     * littéral : ce module existe précisément parce qu'une largeur fixée à 20
     * caractères tronquait — ou faisait échouer — {@code PENDINGTRANSPORTERACK},
     * qui en fait 21. La largeur vit avec le vocabulaire, l'entité s'y réfère,
     * et les deux ne peuvent plus diverger.</p>
     */
    @Setter(AccessLevel.NONE)
    @Column(name = "status", nullable = false, length = Lifecycle.MAX_STATUS_LENGTH)
    private String status = "ACTIVE";

    /** Libellé lisible du statut (pattern Maximo status_description). */
    @Setter(AccessLevel.NONE)
    @Column(name = "status_description", nullable = false, length = 100)
    private String statusDescription = "Actif";

    /** Timestamp du dernier changement de statut (pattern Maximo statusdate). */
    @Setter(AccessLevel.NONE)
    @Column(name = "status_date")
    private Instant statusDate;

    /**
     * Met à jour le statut et enregistre la date de changement.
     *
     * <p>Unique porte d'écriture du statut, pour toutes les entités. Le chemin
     * mécanique pour les dix modules hors périmètre du cycle de vie de Flux 2 ;
     * pour {@code Tour} et {@code Checkpoint}, c'est
     * {@code Lifecycle.requireTransition} qui décide si l'écriture est permise.</p>
     */
    public void updateStatus(String newStatus, String newStatusDescription) {
        this.status = newStatus;
        this.statusDescription = newStatusDescription;
        this.statusDate = Instant.now();
    }
}
