package com.gpl.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Sous-objet d'un changement de champ individuel.
 *
 * Chaque FieldChange capture la modification d'UN champ spécifique :
 * - fieldName  : nom technique du champ (ex: "contactEmail")
 * - fieldLabel : libellé lisible (ex: "Email de contact")
 * - oldValue   : ancienne valeur (ex: "old@snh.cm")
 * - newValue   : nouvelle valeur (ex: "contact@snh.cm")
 * - valueType  : type de la valeur (STRING, NUMBER, BOOLEAN, DATE, ENUM, JSON)
 *
 * Exemple JSON :
 * <pre>
 * {
 *   "fieldName": "status",
 *   "fieldLabel": "Statut",
 *   "oldValue": "PENDAPPR",
 *   "oldValueDescription": "En attente d'approbation",
 *   "newValue": "ACTIVE",
 *   "newValueDescription": "Actif",
 *   "valueType": "ENUM"
 * }
 * </pre>
 */
@Entity
@Table(name = "field_changes", indexes = {
        @Index(name = "idx_fc_modification", columnList = "modification_id"),
        @Index(name = "idx_fc_field_name", columnList = "field_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldChange {

    @Id
    @Column(length = 36)
    private String id;

    /** Référence vers la modification parente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modification_id", nullable = false)
    @JsonIgnore
    private EntityModification modification;

    /** Nom technique du champ modifié. */
    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    /** Libellé lisible du champ (pour l'affichage UI). */
    @Column(name = "field_label", length = 200)
    private String fieldLabel;

    /** Ancienne valeur (sérialisée en String). */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** Description lisible de l'ancienne valeur (pour les enums). */
    @Column(name = "old_value_description", length = 255)
    private String oldValueDescription;

    /** Nouvelle valeur (sérialisée en String). */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** Description lisible de la nouvelle valeur (pour les enums). */
    @Column(name = "new_value_description", length = 255)
    private String newValueDescription;

    /**
     * Type de la valeur pour le parsing côté client.
     * Valeurs possibles : STRING, NUMBER, BOOLEAN, DATE, ENUM, JSON
     */
    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType;

    /** Ordre d'affichage dans la liste des changements. */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
}
