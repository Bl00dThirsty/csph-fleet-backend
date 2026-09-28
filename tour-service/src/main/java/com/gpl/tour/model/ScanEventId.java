package com.gpl.tour.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Composite primary key for {@link ScanEvent}.
 *
 * <p>The {@code scan_events} table is a TimescaleDB hypertable keyed on the
 * {@code timestamp} column. Postgres requires the primary key to include the
 * hypertable dimension column, hence the composite PK {@code (id, timestamp)}.</p>
 *
 * <p>Used as {@code @IdClass} on the entity (Hibernate requires a no-arg
 * constructor and {@code Serializable} on the key class).</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   28.09.2026
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ScanEventId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** UUID generated on the PDA (or backend fallback) to identify the scan event. */
    private UUID id;

    /** Hypertable dimension column — must be part of the composite key. */
    private Instant timestamp;
}