package com.gpl.common.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Historique des changements de statut d'une entité.
 * Chaque transition de statut (ex: PENDAPPR → ACTIVE) crée un enregistrement.
 */
@Entity
@Table(name = "status_history", indexes = {
        @Index(name = "idx_sh_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_sh_changedate", columnList = "changedate"),
        @Index(name = "idx_sh_new_status", columnList = "new_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistory {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    @Column(name = "entity_name", length = 255)
    private String entityName;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "previous_status_description", length = 100)
    private String previousStatusDescription;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    @Column(name = "new_status_description", nullable = false, length = 100)
    private String newStatusDescription;

    @Column(name = "changeby", nullable = false, length = 36)
    private String changeby;

    @Column(name = "changeby_display_name", length = 200)
    private String changebyDisplayName;

    @Column(name = "changedate", nullable = false)
    private Instant changedate;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "is_automatic", nullable = false)
    private Boolean isAutomatic = false;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.changedate == null) {
            this.changedate = Instant.now();
        }
    }
}
