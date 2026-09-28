package com.gpl.tour.repository;

import com.gpl.tour.model.ScanEvent;
import com.gpl.tour.model.ScanEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour l'accès aux données des événements de scan RFID
 * (table {@code scan_events}, hypertable TimescaleDB).
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   28.09.2026
 */
@Repository
public interface ScanEventRepository extends JpaRepository<ScanEvent, ScanEventId> {

    /**
     * Liste tous les scans d'un checkpoint, ordonnés par timestamp croissant
     * (ordre chronologique d'arrivée du livreur sur le site).
     *
     * @param checkpointId identifiant UUID du checkpoint
     * @return liste ordonnée des scans associés
     */
    List<ScanEvent> findByCheckpointIdOrderByTimestampAsc(UUID checkpointId);

    /**
     * Idempotency lookup for PDA bulk re-uploads. The PDA mints one stable
     * {@code pdaSyncId} per local read BEFORE uploading and keeps re-sending
     * the same row while the server has not confirmed it (offline retries,
     * timeout ambiguities). A hit here means the read was already stored —
     * the caller must return the existing row instead of inserting a twin.
     */
    Optional<ScanEvent> findByPdaSyncId(String pdaSyncId);
}