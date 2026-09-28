package com.gpl.cylinder.repository;

import com.gpl.cylinder.model.ScanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanEventRepository extends JpaRepository<ScanEvent, String>, JpaSpecificationExecutor<ScanEvent> {
    List<ScanEvent> findByRfidTagId(String rfidTagId);
    List<ScanEvent> findByDriverPersonId(String driverPersonId);

    /*
     * Scans d'un arrêt, du plus ancien au plus récent.
     *
     * <p>Sans cette méthode, lister les scans d'un checkpoint passait par une
     * Specification construite à l'exécution, soit un full scan de la
     * hypertable Timescale qui conserve 5 ans de preuve.</p>
     */
    List<ScanEvent> findByCheckpointIdOrderByTimestampAsc(String checkpointId);
}
