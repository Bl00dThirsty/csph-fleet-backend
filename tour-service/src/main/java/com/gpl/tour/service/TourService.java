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
     * Retrieves all Tours with pagination, optionally scoped to one driver.
     *
     * @param driverPersonId null or blank lists every tour; a value restricts the
     *                       page to the tours assigned to that driver.
     */
    PageResponse<TourResponseDto> getAll(String driverPersonId, Pageable pageable);

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
     * Every method below goes through com.gpl.common.lifecycle.Lifecycle, which owns
     * the transition tables for both execution modes and both status domains. The
     * service does not hold a status of its own and does not decide what is legal:
     * an illegal move is a 422 naming the attempted transition and the legal ones.
     *
     * INTERNAL: DRAFT -> PLANNED -> INPROGRESS -> CHECKPOINTACTIVE -> CLOSED
     * EXTERNAL: DRAFT -> PENDINGTRANSPORTERACK -> ACKNOWLEDGED -> INPROGRESS
     *           -> CHECKPOINTACTIVE -> CLOSED
     * Either mode may also reach CANCELLED before rollout begins.
     */

    /*
     * Planifier une tournée (DRAFT -> PLANNED). Mode INTERNAL.
     */
    TourResponseDto plan(String id, String plannedBy);

    /*
     * Transmettre une tournée à un transporteur (DRAFT -> PENDINGTRANSPORTERACK).
     * Mode EXTERNAL uniquement, et exige un transporteur identifié.
     */
    TourResponseDto sendToTransporter(String id, String sentBy);

    /*
     * Enregistrer l'accusé de réception du transporteur
     * (PENDINGTRANSPORTERACK -> ACKNOWLEDGED), en horodatant l'affectation.
     */
    TourResponseDto acknowledge(String id, String acknowledgedBy);

    /*
     * Démarrer une tournée (PLANNED ou ACKNOWLEDGED -> INPROGRESS).
     */
    TourResponseDto startTour(String id, String startedBy);

    /*
     * Clôturer une tournée (CHECKPOINTACTIVE -> CLOSED).
     *
     * <p>Exige que chaque arrêt soit COMPLETED ou SKIPPED, et que la quantité
     * livrée soit cohérente avec la quantité chargée : c'est le chiffre sur lequel
     * se construit la réconciliation de subvention.</p>
     */
    TourResponseDto closeTour(String id, Double loadedQuantity, Double deliveredQuantity, String closedBy);

    /*
     * Annuler une tournée (vers CANCELLED). Autorisé tant que la tournée n'est pas
     * engagée sur la route.
     */
    TourResponseDto cancelTour(String id, String reason, String cancelledBy);

    /*
     * Assigner un chauffeur à la tournée. Ne change aucun statut : nommer un
     * chauffeur n'est pas accuser réception d'une mission.
     */
    TourResponseDto assignDriver(String id, String driverId, String driverPersonId, String assignedBy);

    /*
     * Assigner un véhicule à la tournée. Ne change aucun statut.
     */
    TourResponseDto assignVehicle(String id, String vehicleId, String assignedBy);

    /*
     * Terminer la livraison d'un arrêt (REACHED -> COMPLETED).
     *
     * <p>Ne touche pas actualArrival : l'heure d'arrivée est le fait enregistré par
     * reachCheckpoint. Confondre les deux perd la preuve d'arrivée.</p>
     */
    CheckpointResponseDto completeCheckpoint(String checkpointId, String completedBy);

    /*
     * Marquer un arrêt de tournée comme atteint par le livreur (PENDING -> REACHED)
     * avec horodatage actualArrival, avant la saisie des scans et la fin de livraison.
     *
     * <p>Première arrivée d'une tournée : promeut la tournée INPROGRESS ->
     * CHECKPOINTACTIVE.</p>
     */
    CheckpointResponseDto reachCheckpoint(String checkpointId, String reachedBy);

    /*
     * Sauter un arrêt de tournée (PENDING ou REACHED -> SKIPPED) avec
     * justification obligatoire.
     */
    CheckpointResponseDto skipCheckpoint(String checkpointId, String reason, String skippedBy);
}
