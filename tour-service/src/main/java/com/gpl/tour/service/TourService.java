package com.gpl.tour.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.tour.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for Tour and Tour Stop (Checkpoint) management.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
public interface TourService {

    /*
     * Creates a new Tour.
     */
    TourResponseDto create(CreateTourDto dto, String createdBy);

    /*
     * Retrieves a Tour by its ID.
     */
    TourResponseDto getById(String id);

    /*
     * Retrieves all Tours with pagination.
     */
    PageResponse<TourResponseDto> getAll(Pageable pageable);

    /*
     * Updates an existing Tour by its ID.
     */
    TourResponseDto update(String id, UpdateTourDto dto, String updatedBy);

    /*
     * Deletes a Tour by its ID.
     */
    void delete(String id);

    /*
     * Adds a new Checkpoint (stop) to a Tour.
     */
    CheckpointResponseDto addCheckpoint(String tourId, CreateCheckpointDto dto, String createdBy);

    /*
     * Retrieves all Checkpoints for a given Tour ordered by sequence.
     */
    List<CheckpointResponseDto> getCheckpointsByTour(String tourId);

    /*
     * Retrieves a Checkpoint by its ID.
     */
    CheckpointResponseDto getCheckpointById(String checkpointId);

    /*
     * Updates an existing Checkpoint.
     */
    CheckpointResponseDto updateCheckpoint(String checkpointId, UpdateCheckpointDto dto, String updatedBy);

    /*
     * Deletes a Checkpoint by its ID.
     */
    void deleteCheckpoint(String checkpointId);

    /* ── Operation Lifecycle & Dynamic Assignments (Phase 3) ─────────────── */

    /*
     * Démarrer une tournée (PLANNED -> STARTED).
     */
    TourResponseDto startTour(String id, String startedBy);

    /*
     * Clôturer une tournée (STARTED -> COMPLETED).
     */
    TourResponseDto closeTour(String id, Double loadedQuantity, Double deliveredQuantity, String closedBy);

    /*
     * Annuler une tournée.
     */
    TourResponseDto cancelTour(String id, String reason, String cancelledBy);

    /*
     * Assigner un chauffeur à la tournée.
     */
    TourResponseDto assignDriver(String id, String driverId, String driverPersonId, String assignedBy);

    /*
     * Assigner un véhicule à la tournée.
     */
    TourResponseDto assignVehicle(String id, String vehicleId, String assignedBy);

    /*
     * Valider la réalisation d'un arrêt de tournée avec horodatage actualArrival.
     */
    CheckpointResponseDto validateCheckpoint(String checkpointId, String validatedBy);

    /*
     * Marquer un arrêt de tournée comme atteint par le livreur (PENDING -> REACHED)
     * avec horodatage actualArrival, avant la saisie des scans et la validation finale.
     */
    CheckpointResponseDto reachCheckpoint(String checkpointId, String reachedBy);

    /*
     * Sauter un arrêt de tournée avec justification obligatoire.
     */
    CheckpointResponseDto skipCheckpoint(String checkpointId, String reason, String skippedBy);
}
