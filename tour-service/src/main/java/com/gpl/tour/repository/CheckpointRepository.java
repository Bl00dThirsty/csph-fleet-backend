package com.gpl.tour.repository;

import com.gpl.tour.model.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour l'accès aux données des arrêts de tournée (checkpoints).
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@Repository
public interface CheckpointRepository extends JpaRepository<Checkpoint, String> {

    List<Checkpoint> findByTourIdOrderBySequenceAsc(String tourId);

    Optional<Checkpoint> findByTourIdAndSequence(String tourId, int sequence);

    void deleteByTourId(String tourId);
}
