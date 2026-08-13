package com.gpl.common.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Sous-objet de modification d'une entité.
 *
 * Chaque fois qu'un utilisateur modifie une entité (Organization, Site, Person, etc.),
 * un enregistrement EntityModification est créé, contenant la liste détaillée
 * des champs modifiés via {@link FieldChange}.
 *
 * Ce modèle est inspiré du pattern Maximo collection_ref :
 * - GET /api/v1/organizations/{id} → retourne l'organisation + ses dernières modifications
 * - GET /api/v1/organizations/{id}/modifications → retourne TOUTES les modifications
 * - GET /api/v1/modifications/{modificationId} → retourne UNE modification avec ses changes
 *
 * Exemple de réponse JSON embarquée (comme les phone[] dans mxperson) :
 *
 * <pre>
 * {
 *   "id": "org-123",
 *   "name": "SNH",
 *   "status": "ACTIVE",
 *   "modifications_collectionref": "/api/v1/organizations/org-123/modifications",
 *   "modifications": [
 *     {
 *       "modificationId": "mod-001",
 *       "action": "UPDATE",
 *       "actionDescription": "Modification",
 *       "changeby": "5788-M",
 *       "changebyDisplayName": "MBARGA Jean-Pierre",
 *       "changedate": "2024-06-10T14:30:00Z",
 *       "description": "Mise à jour des coordonnées de contact",
 *       "changes": [
 *         {
 *           "fieldName": "contactEmail",
 *           "fieldLabel": "Email de contact",
 *           "oldValue": "old@snh.cm",
 *           "newValue": "contact@snh.cm",
 *           "valueType": "STRING"
 *         },
 *         {
 *           "fieldName": "contactPhone",
 *           "fieldLabel": "Téléphone de contact",
 *           "oldValue": "237699000000",
 *           "newValue": "237699111111",
 *           "valueType": "STRING"
 *         }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 */
@Entity
@Table(name = "entity_modifications", indexes = {
        @Index(name = "idx_mod_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_mod_changeby", columnList = "changeby"),
        @Index(name = "idx_mod_changedate", columnList = "changedate"),
        @Index(name = "idx_mod_action", columnList = "action"),
        @Index(name = "idx_mod_org", columnList = "actor_org_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityModification {

    @Id
    @Column(length = 36)
    private String id;

    // ────────────────────────── Entité cible ──────────────────────────

    /** Type de l'entité modifiée (Organization, Site, Person, Role...). */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /** ID de l'entité modifiée. */
    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    /** Nom lisible de l'entité (pour affichage sans join). */
    @Column(name = "entity_name", length = 255)
    private String entityName;

    // ────────────────────────── Action ──────────────────────────

    /** Code de l'action (CRT, UPD, DEL, STC...). */
    @Column(name = "action", nullable = false, length = 10)
    private String action;

    /** Description lisible de l'action (pattern Maximo _description). */
    @Column(name = "action_description", nullable = false, length = 100)
    private String actionDescription;

    /** Module applicatif concerné (organizations, persons, tours...). */
    @Column(name = "module", length = 50)
    private String module;

    /** Description libre de la modification. */
    @Column(name = "description", length = 500)
    private String description;

    // ────────────────────────── Qui a modifié ──────────────────────────

    /** PersonId de l'auteur de la modification. */
    @Column(name = "changeby", nullable = false, length = 36)
    private String changeby;

    /** Nom affiché de l'auteur (dénormalisé pour perf). */
    @Column(name = "changeby_display_name", length = 200)
    private String changebyDisplayName;

    /** Organisation de l'auteur. */
    @Column(name = "actor_org_id", length = 36)
    private String actorOrgId;

    /** Site de l'auteur au moment de la modification. */
    @Column(name = "actor_site_id", length = 36)
    private String actorSiteId;

    // ────────────────────────── Quand ──────────────────────────

    /** Timestamp de la modification. */
    @Column(name = "changedate", nullable = false)
    private Instant changedate;

    // ────────────────────────── Contexte technique ──────────────────────────

    /** Info appareil (PDA Chainway C72, Android 13...). */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /** Version de l'application. */
    @Column(name = "app_version", length = 20)
    private String appVersion;

    /** Adresse IP. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Latitude GPS au moment de la modification. */
    @Column(name = "latitude")
    private Double latitude;

    /** Longitude GPS au moment de la modification. */
    @Column(name = "longitude")
    private Double longitude;

    // ────────────────────────── Sous-objets : détails des changements ──────────────────────────

    /**
     * Liste détaillée des champs modifiés.
     * Chaque FieldChange capture : nom du champ, ancien et nouveau valeur.
     * Cascade ALL : les FieldChange sont créés/supprimés avec leur EntityModification parent.
     */
    @OneToMany(mappedBy = "modification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<FieldChange> changes = new ArrayList<>();

    // ────────────────────────── Helpers ──────────────────────────

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.changedate == null) {
            this.changedate = Instant.now();
        }
    }

    /**
     * Ajoute un changement de champ à cette modification.
     */
    public void addChange(String fieldName, String fieldLabel, String oldValue, String newValue, String valueType) {
        FieldChange change = new FieldChange();
        change.setId(java.util.UUID.randomUUID().toString());
        change.setModification(this);
        change.setFieldName(fieldName);
        change.setFieldLabel(fieldLabel);
        change.setOldValue(oldValue);
        change.setNewValue(newValue);
        change.setValueType(valueType);
        change.setSortOrder(this.changes.size());
        this.changes.add(change);
    }
}
