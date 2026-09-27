package com.gpl.common.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entité de base pour toutes les entités du système.
 * Inspirée du pattern Maximo : _rowStamp + changeby + changedate.
 *
 * Chaque entité porte nativement :
 * - Son identifiant UUID
 * - Un _rowStamp pour le versioning optimiste (sync offline)
 * - Les métadonnées de création et de dernière modification
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    /**
     * Versioning optimiste pour la synchronisation offline.
     * Incrémenté à chaque modification. L'app mobile compare ce stamp
     * pour détecter les conflits (pattern Maximo _rowstamp).
     */
    @Version
    @Column(name = "_row_stamp", nullable = false)
    private Long rowStamp = 0L;

    // ────────────────────────── Audit : Création ──────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** PersonId du créateur (pattern Maximo). */
    @Column(name = "created_by", nullable = false, updatable = false, length = 36)
    private String createdBy;

    // ────────────────────────── Audit : Dernière modification ──────────────────────────

    /** PersonId du dernier modificateur (pattern Maximo changeby). */
    @Column(name = "changeby", length = 36)
    private String changeby;

    /** Timestamp de la dernière modification (pattern Maximo changedate). */
    @Column(name = "changedate")
    private Instant changedate;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        // `created_by` is NOT NULL in every table, but nothing populated it, so
        // any insert from a service that did not set it explicitly blew up with
        // "null value in column created_by violates not-null constraint" (e.g.
        // POST /api/v1/notification-templates returned 500). Default it here, in
        // the one place every entity passes through: prefer the authenticated
        // caller, fall back to SYSTEM for seeders and internal jobs.
        if (this.createdBy == null) {
            String personId = null;
            try {
                personId = com.gpl.common.security.GplSecurityContext.getCurrentPersonId();
            } catch (Throwable ignored) {
                // no security context available (seeders, background jobs)
            }
            this.createdBy = (personId != null && !personId.isBlank()) ? personId : "SYSTEM";
        }
        if (this.changeby == null) {
            this.changeby = this.createdBy;
        }
        if (this.changedate == null) {
            this.changedate = Instant.now();
        }
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.changedate = Instant.now();
    }
}
