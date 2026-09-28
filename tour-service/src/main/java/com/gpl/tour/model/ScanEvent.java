package com.gpl.tour.model;

import com.gpl.common.enums.ScanDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for a single RFID scan event recorded by a livreur on a checkpoint.
 *
 * <p>Maps to the {@code scan_events} table — a TimescaleDB hypertable keyed on
 * {@code timestamp}. Because of the hypertable constraint, the primary key is
 * composite {@code (id, timestamp)} and is exposed via {@link ScanEventId} as
 * an {@code @IdClass}.</p>
 *
 * <p>GPS is stored as plain {@code geo_lng} / {@code geo_lat} doubles. A PostGIS
 * {@code GEOMETRY(Point,4326)} column was tried before, but the dev/CI Postgres
 * fleet is plain postgres (no PostGIS, no hibernate-spatial) so Hibernate could
 * never create the table and every scan upload 500'd with "relation does not
 * exist". Nothing queries this column spatially — reconciliation reads raw
 * coordinates — so doubles are the honest mapping until PostGIS is provisioned
 * everywhere (then reintroduce a geometry column alongside, not instead).</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   28.09.2026
 */
@Entity
@Table(name = "scan_events")
@IdClass(ScanEventId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanEvent {

    /** UUID generated on the PDA (or backend fallback) — part of composite PK. */
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Hypertable dimension column — part of composite PK. */
    @Id
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /** Checkpoint this scan is attached to. */
    @Column(name = "checkpoint_id", nullable = false)
    private UUID checkpointId;

    /** Livreur (driver) who performed the scan. */
    @Column(name = "livreur_user_id", nullable = false)
    private UUID livreurUserId;

    /** Optional RFID tag UID read by the PDA. Null on barcode fallback or manual scans. */
    @Column(name = "rfid_tag_id")
    private UUID rfidTagId;

    /** Entry / exit direction of the scan: {@code IN} or {@code OUT} (validated in service). */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private ScanDirection direction;

    /** GPS longitude (WGS 84) captured by the PDA at read time. Required. */
    @Column(name = "geo_lng", nullable = false)
    private Double geoLng;

    /** GPS latitude (WGS 84) captured by the PDA at read time. Required. */
    @Column(name = "geo_lat", nullable = false)
    private Double geoLat;

    /** Volumetric meter reading for VRAC (bulk) transports. Null when {@code direction} is set. */
    @Column(name = "meter_reading")
    private Double meterReading;

    /** Optional photo evidence URL (S3 / MinIO). */
    @Column(name = "photo_url")
    private String photoUrl;

    /** Offline-sync correlation id emitted by the PDA (for de-dup on bulk upload). */
    @Column(name = "pda_sync_id", length = 100, unique = true)
    private String pdaSyncId;

    /** Conflict status: {@code PENDING}, {@code RESOLVED}, etc. Populated by reconciliation jobs. */
    @Column(name = "conflict_status", length = 20)
    private String conflictStatus;

    /** Row creation timestamp (set on first persist if null). */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** User (username) who created this row — typically the PDA-side operator. */
    @Column(name = "created_by")
    private UUID createdBy;

    /**
     * JPA lifecycle callback — guarantees {@code createdAt} is populated before insert.
     * Mirrors the Postgres {@code DEFAULT now()} default so JPA-side generated rows
     * are identical to raw-SQL inserts.
     */
    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}