package com.gpl.tour.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.enums.TourExecutionMode;
import com.gpl.common.exception.BusinessException;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.common.lifecycle.CheckpointStatus;
import com.gpl.common.lifecycle.Lifecycle;
import com.gpl.common.lifecycle.TourneeStatus;
import com.gpl.tour.dto.*;
import com.gpl.tour.model.Checkpoint;
import com.gpl.tour.model.Tour;
import com.gpl.tour.repository.CheckpointRepository;
import com.gpl.tour.repository.TourRepository;
import com.gpl.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of TourService including Checkpoint (multi-stop/multi-client) management.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepository;
    private final CheckpointRepository checkpointRepository;

    /*
     * Creates a new Tour.
     */
    @Override
    public TourResponseDto create(CreateTourDto dto, String createdBy) {
        log.info("Creating new Tour with code: {}", dto.getTourCode());
        Tour tour = new Tour();
        tour.setTourCode(dto.getTourCode());
        tour.setMarketerOrganizationId(dto.getMarketerOrganizationId());
        tour.setExecutionMode(dto.getExecutionMode());
        tour.setType(dto.getType());
        tour.setRequestedQuantity(dto.getRequestedQuantity());
        tour.setTransporterOrganizationId(dto.getTransporterOrganizationId());
        tour.setVehicleId(dto.getVehicleId());
        tour.setDriverId(dto.getDriverId());
        tour.setDriverPersonId(dto.getDriverPersonId());

        // A tour is born DRAFT, not "ACTIVE". "ACTIVE" is a Site status inherited from
        // AuditableEntity's field default, and it is not a member of tournee_status —
        // so every tour was born outside its own status domain and the transition
        // table could never have been applied to it.
        tour.updateStatus(TourneeStatus.DRAFT.name(), Lifecycle.labelOf(TourneeStatus.DRAFT));

        tour.setCreatedBy(createdBy);

        Tour savedTour = tourRepository.save(tour);
        log.debug("Tour created successfully with id: {}", savedTour.getId());

        return mapToDto(savedTour);
    }

    /*
     * Retrieves a Tour by its ID with its list of checkpoints.
     */
    @Override
    @Transactional(readOnly = true)
    public TourResponseDto getById(String id) {
        log.info("Fetching Tour by id: {}", id);
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Tour not found with id: {}", id);
                    return new ResourceNotFoundException("Tour", "id", id);
                });
        return mapToDto(tour);
    }

    /*
     * Retrieves all Tours with pagination, optionally scoped to one driver.
     *
     * A blank driverPersonId means "no scope" and resolves to findAll — the
     * regulator, the marketer and the superadmin all list the whole fleet.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TourResponseDto> getAll(String driverPersonId, Pageable pageable) {
        if (driverPersonId == null || driverPersonId.isBlank()) {
            log.info("Fetching all Tours with pagination");
            return PageResponse.of(tourRepository.findAll(pageable).map(this::mapToDto));
        }
        log.info("Fetching Tours with pagination for driver: {}", driverPersonId);
        return PageResponse.of(
                tourRepository.findByDriverPersonId(driverPersonId, pageable).map(this::mapToDto));
    }

    /*
     * Updates an existing Tour by its ID.
     */
    @Override
    public TourResponseDto update(String id, UpdateTourDto dto, String updatedBy) {
        log.info("Updating Tour with id: {}", id);
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Tour not found with id: {}", id);
                    return new ResourceNotFoundException("Tour", "id", id);
                });

        if (dto.getTourCode() != null) {
            tour.setTourCode(dto.getTourCode());
        }
        if (dto.getMarketerOrganizationId() != null) {
            tour.setMarketerOrganizationId(dto.getMarketerOrganizationId());
        }
        if (dto.getExecutionMode() != null) {
            tour.setExecutionMode(dto.getExecutionMode());
        }
        if (dto.getType() != null) {
            tour.setType(dto.getType());
        }
        if (dto.getRequestedQuantity() != null) {
            tour.setRequestedQuantity(dto.getRequestedQuantity());
        }
        if (dto.getTransporterOrganizationId() != null) {
            tour.setTransporterOrganizationId(dto.getTransporterOrganizationId());
        }
        if (dto.getVehicleId() != null) {
            tour.setVehicleId(dto.getVehicleId());
        }
        if (dto.getDriverId() != null) {
            tour.setDriverId(dto.getDriverId());
        }
        if (dto.getDriverPersonId() != null) {
            tour.setDriverPersonId(dto.getDriverPersonId());
        }

        tour.setChangeby(updatedBy);

        Tour updatedTour = tourRepository.save(tour);
        log.debug("Tour updated successfully with id: {}", updatedTour.getId());

        return mapToDto(updatedTour);
    }

    /*
     * Deletes a Tour and its associated checkpoints by its ID.
     */
    @Override
    public void delete(String id) {
        log.info("Deleting Tour with id: {}", id);
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Tour not found with id: {}", id);
                    return new ResourceNotFoundException("Tour", "id", id);
                });
        checkpointRepository.deleteByTourId(id);
        tourRepository.delete(tour);
        log.debug("Tour and associated checkpoints deleted successfully with id: {}", id);
    }

    /*
     * Adds a new Checkpoint (stop) to a Tour.
     */
    @Override
    public CheckpointResponseDto addCheckpoint(String tourId, CreateCheckpointDto dto, String createdBy) {
        log.info("Adding Checkpoint to Tour ID: {} (Sequence: {})", tourId, dto.getSequence());
        if (!tourRepository.existsById(tourId)) {
            throw new ResourceNotFoundException("Tour", "id", tourId);
        }

        if (dto.getSiteId() == null && dto.getClientSiteId() == null) {
            throw new BusinessException("Un arrêt doit spécifier soit un site opérationnel (siteId), soit un site client (clientSiteId)");
        }
        if (dto.getSiteId() != null && dto.getClientSiteId() != null) {
            throw new BusinessException("Un arrêt ne peut pas être simultanément un site opérationnel et un site client");
        }

        Checkpoint checkpoint = Checkpoint.builder()
                .tourId(tourId)
                .siteId(dto.getSiteId())
                .clientSiteId(dto.getClientSiteId())
                .sequence(dto.getSequence())
                .expectedArrival(dto.getExpectedArrival())
                .skipReason(dto.getSkipReason())
                .build();
        // Same defect as the tour birth state: the builder never set a status, so every
        // checkpoint was born "ACTIVE" — a Site status, not a checkpoint_status value.
        checkpoint.updateStatus(CheckpointStatus.PENDING.name(),
                Lifecycle.labelOf(CheckpointStatus.PENDING));
        checkpoint.setCreatedBy(createdBy);

        Checkpoint saved = checkpointRepository.save(checkpoint);
        log.debug("Checkpoint created with ID: {}", saved.getId());
        return mapCheckpointToDto(saved);
    }

    /*
     * Retrieves all Checkpoints for a given Tour ordered by sequence.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CheckpointResponseDto> getCheckpointsByTour(String tourId) {
        log.info("Fetching checkpoints for Tour ID: {}", tourId);
        if (!tourRepository.existsById(tourId)) {
            throw new ResourceNotFoundException("Tour", "id", tourId);
        }
        return checkpointRepository.findByTourIdOrderBySequenceAsc(tourId).stream()
                .map(this::mapCheckpointToDto)
                .toList();
    }

    /*
     * Retrieves a Checkpoint by its ID.
     */
    @Override
    @Transactional(readOnly = true)
    public CheckpointResponseDto getCheckpointById(String checkpointId) {
        log.info("Fetching Checkpoint by ID: {}", checkpointId);
        Checkpoint checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint", "id", checkpointId));
        return mapCheckpointToDto(checkpoint);
    }

    /*
     * Updates an existing Checkpoint.
     */
    @Override
    public CheckpointResponseDto updateCheckpoint(String checkpointId, UpdateCheckpointDto dto, String updatedBy) {
        log.info("Updating Checkpoint ID: {}", checkpointId);
        Checkpoint checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint", "id", checkpointId));

        if (dto.getSiteId() != null) {
            checkpoint.setSiteId(dto.getSiteId());
        }
        if (dto.getClientSiteId() != null) {
            checkpoint.setClientSiteId(dto.getClientSiteId());
        }
        if (dto.getSequence() != null) {
            checkpoint.setSequence(dto.getSequence());
        }
        if (dto.getExpectedArrival() != null) {
            checkpoint.setExpectedArrival(dto.getExpectedArrival());
        }
        if (dto.getSkipReason() != null) {
            checkpoint.setSkipReason(dto.getSkipReason());
        }

        // No status write, and no actualArrival write. Both were bypasses: a PUT
        // carrying `status` set any lifecycle state directly, and a PUT carrying
        // `actualArrival` backdated the arrival evidence that only reachCheckpoint
        // is allowed to capture. Reached and completed are reached via
        // POST /api/v1/checkpoints/{id}/reach and .../complete.

        checkpoint.setChangeby(updatedBy);
        Checkpoint updated = checkpointRepository.save(checkpoint);
        log.debug("Checkpoint updated successfully: {}", updated.getId());

        return mapCheckpointToDto(updated);
    }

    /*
     * Deletes a Checkpoint by its ID.
     */
    @Override
    public void deleteCheckpoint(String checkpointId) {
        log.info("Deleting Checkpoint ID: {}", checkpointId);
        Checkpoint checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint", "id", checkpointId));
        checkpointRepository.delete(checkpoint);
        log.debug("Checkpoint deleted successfully: {}", checkpointId);
    }

    /* ── Lifecycle Operation Workflows ─────────────────────────────────────
     *
     * Every guard below delegates to com.gpl.common.lifecycle.Lifecycle, which owns
     * the transition tables for both execution modes and both status domains. The
     * service holds no status logic of its own: it resolves the current code, asks
     * the domain whether the move is legal, and writes the pair (code, label)
     * together.
     *
     * The seven hand-written terminal-state blacklists this replaces had already
     * drifted apart — startTour wrote "STARTED", a code that is not a member of
     * TourneeStatus, so the transition table could never have described what the
     * service actually did.
     * ──────────────────────────────────────────────────────────────────── */

    @Override
    public TourResponseDto plan(String id, String plannedBy) {
        log.info("Planning Tour ID: {} by user: {}", id, plannedBy);
        Tour tour = requireTour(id);

        transition(tour, TourneeStatus.PLANNED);
        tour.setChangeby(actor(plannedBy));

        return mapToDto(tourRepository.save(tour));
    }

    @Override
    public TourResponseDto sendToTransporter(String id, String sentBy) {
        log.info("Sending Tour ID: {} to transporter by user: {}", id, sentBy);
        Tour tour = requireTour(id);

        // chk_tournee_external: a subcontracted tour must name its transporter,
        // otherwise there is nobody to send it to and the ack step has no actor.
        if (tour.getExecutionMode() == null
                || !TourExecutionMode.EXTERNAL.name().equalsIgnoreCase(tour.getExecutionMode())) {
            throw new BusinessException(
                    "Seule une tournée en mode EXTERNAL peut être transmise à un transporteur. "
                            + "Mode actuel : " + tour.getExecutionMode() + ".");
        }
        if (tour.getTransporterOrganizationId() == null || tour.getTransporterOrganizationId().isBlank()) {
            throw new BusinessException(
                    "Une tournée EXTERNAL doit identifier son transporteur avant d'être transmise "
                            + "(contrainte chk_tournee_external).");
        }

        transition(tour, TourneeStatus.PENDINGTRANSPORTERACK);
        tour.setChangeby(actor(sentBy));

        return mapToDto(tourRepository.save(tour));
    }

    @Override
    public TourResponseDto acknowledge(String id, String acknowledgedBy) {
        log.info("Transporter acknowledging Tour ID: {} by user: {}", id, acknowledgedBy);
        Tour tour = requireTour(id);

        transition(tour, TourneeStatus.ACKNOWLEDGED);
        // The acknowledgement IS an assignment: record who accepted, and when.
        tour.setAssignedByTransporterPersonId(actor(acknowledgedBy));
        tour.setTransporterAssignedAt(java.time.Instant.now());
        tour.setChangeby(actor(acknowledgedBy));

        return mapToDto(tourRepository.save(tour));
    }

    @Override
    public TourResponseDto startTour(String id, String startedBy) {
        log.info("Starting Tour ID: {} by user: {}", id, startedBy);
        Tour tour = requireTour(id);

        transition(tour, TourneeStatus.INPROGRESS);
        tour.setStartedAt(java.time.Instant.now());
        tour.setChangeby(actor(startedBy));

        return mapToDto(tourRepository.save(tour));
    }

    @Override
    public TourResponseDto closeTour(String id, Double loadedQuantity, Double deliveredQuantity, String closedBy) {
        log.info("Closing Tour ID: {} by user: {}", id, closedBy);
        Tour tour = requireTour(id);

        transition(tour, TourneeStatus.CLOSED);

        // A tour cannot be closed while a stop is still unaccounted for. Naming the
        // offending sequences turns a silent close into an actionable refusal.
        List<String> unresolved = checkpointRepository.findByTourIdOrderBySequenceAsc(tour.getId()).stream()
                .filter(cp -> !CheckpointStatus.COMPLETED.name().equals(cp.getStatus())
                        && !CheckpointStatus.SKIPPED.name().equals(cp.getStatus()))
                .map(cp -> "seq " + cp.getSequence() + " (" + cp.getStatus() + ")")
                .toList();
        if (!unresolved.isEmpty()) {
            throw new BusinessException(
                    "La tournée ne peut pas être clôturée tant que ses arrêts ne sont pas tous terminés. "
                            + "Arrêts restants : " + String.join(", ", unresolved) + ".");
        }

        // delivered_quantity is the subsidy reconciliation input and it arrives as an
        // unvalidated query parameter. Validating it at the edge is what makes the
        // figure trustworthy; deriving it from the scans (the TrackedVolume seam)
        // stays out of scope.
        if (deliveredQuantity == null) {
            throw new BusinessException(
                    "La quantité livrée est obligatoire à la clôture d'une tournée.");
        }
        if (deliveredQuantity < 0) {
            throw new BusinessException(
                    "La quantité livrée ne peut pas être négative (reçu : " + deliveredQuantity + ").");
        }
        if (loadedQuantity != null && deliveredQuantity > loadedQuantity) {
            throw new BusinessException(
                    "La quantité livrée (" + deliveredQuantity + ") ne peut pas dépasser la quantité chargée ("
                            + loadedQuantity + ").");
        }

        tour.setClosedAt(java.time.Instant.now());
        if (loadedQuantity != null) {
            tour.setLoadedQuantity(loadedQuantity);
        }
        tour.setDeliveredQuantity(deliveredQuantity);
        tour.setChangeby(actor(closedBy));

        return mapToDto(tourRepository.save(tour));
    }

    @Override
    public TourResponseDto cancelTour(String id, String reason, String cancelledBy) {
        log.info("Cancelling Tour ID: {} by user: {}, reason: {}", id, cancelledBy, reason);
        Tour tour = requireTour(id);

        transition(tour, TourneeStatus.CANCELLED);
        // A cancellation is explained, so the label carries the reason. The code
        // stays CANCELLED — a reason is not a status.
        if (reason != null && !reason.isBlank()) {
            tour.updateStatus(TourneeStatus.CANCELLED.name(),
                    Lifecycle.labelOf(TourneeStatus.CANCELLED) + " : " + reason);
        }
        tour.setChangeby(actor(cancelledBy));

        return mapToDto(tourRepository.save(tour));
    }

    @Override
    public TourResponseDto assignDriver(String id, String driverId, String driverPersonId, String assignedBy) {
        log.info("Assigning driver to Tour ID: {}, driverId: {}, driverPersonId: {}", id, driverId, driverPersonId);
        Tour tour = requireTour(id);

        if (driverId != null) {
            tour.setDriverId(driverId);
        }
        if (driverPersonId != null) {
            tour.setDriverPersonId(driverPersonId);
        }

        if ("EXTERNAL".equalsIgnoreCase(tour.getExecutionMode())) {
            // Audit only. This deliberately writes NO status: nominating a driver is
            // not acknowledging a mission, and treating it as such used to erase
            // PENDINGTRANSPORTERACK from the chain entirely.
            tour.setAssignedByTransporterPersonId(actor(assignedBy));
            tour.setTransporterAssignedAt(java.time.Instant.now());
        }

        tour.setChangeby(actor(assignedBy));
        return mapToDto(tourRepository.save(tour));
    }

    @Override
    public TourResponseDto assignVehicle(String id, String vehicleId, String assignedBy) {
        log.info("Assigning vehicle to Tour ID: {}, vehicleId: {}", id, vehicleId);
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", "id", id));

        tour.setVehicleId(vehicleId);
        tour.setChangeby(assignedBy != null ? assignedBy : "SYSTEM");
        Tour saved = tourRepository.save(tour);
        return mapToDto(saved);
    }

    @Override
    public CheckpointResponseDto completeCheckpoint(String checkpointId, String completedBy) {
        log.info("Completing Checkpoint ID: {} by user: {}", checkpointId, completedBy);
        Checkpoint checkpoint = requireCheckpoint(checkpointId);

        // REACHED -> COMPLETED. Note what is absent: actualArrival. The arrival
        // instant is the fact captured by reachCheckpoint, and for a regulator
        // "the vehicle arrived" and "the delivery finished" are separate events.
        // Setting both at once, as the old validateCheckpoint did, destroys the
        // difference the anomaly timeline is built on.
        transitionCheckpoint(checkpoint, CheckpointStatus.COMPLETED);
        checkpoint.setChangeby(actor(completedBy));

        return mapCheckpointToDto(checkpointRepository.save(checkpoint));
    }

    @Override
    public CheckpointResponseDto reachCheckpoint(String checkpointId, String reachedBy) {
        log.info("Reaching Checkpoint ID: {} by user: {}", checkpointId, reachedBy);
        Checkpoint checkpoint = requireCheckpoint(checkpointId);

        transitionCheckpoint(checkpoint, CheckpointStatus.REACHED);
        checkpoint.setActualArrival(java.time.Instant.now());
        checkpoint.setChangeby(actor(reachedBy));

        // The one implicit transition in the model: the tour enters CHECKPOINTACTIVE
        // on the FIRST arrival, not on the last completion.
        Tour tour = requireTour(checkpoint.getTourId());
        TourneeStatus tourStatus = TourneeStatus.fromCode(tour.getStatus());
        if (tourStatus == TourneeStatus.INPROGRESS) {
            transition(tour, TourneeStatus.CHECKPOINTACTIVE);
            tour.setChangeby(actor(reachedBy));
            tourRepository.save(tour);
        }

        return mapCheckpointToDto(checkpointRepository.save(checkpoint));
    }

    @Override
    public CheckpointResponseDto skipCheckpoint(String checkpointId, String reason, String skippedBy) {
        log.info("Skipping Checkpoint ID: {} with reason: {}", checkpointId, reason);
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Un motif de saut est obligatoire pour ignorer un arrêt de tournée.");
        }

        Checkpoint checkpoint = requireCheckpoint(checkpointId);

        // Reachable from PENDING as well as REACHED, deliberately. closeTour requires
        // every checkpoint terminal; if SKIPPED were only reachable from REACHED, a
        // stop the vehicle never drove to could never be terminalised and CLOSED
        // would be unreachable — a dead end in the state machine.
        transitionCheckpoint(checkpoint, CheckpointStatus.SKIPPED);
        checkpoint.setSkipReason(reason);
        checkpoint.setChangeby(actor(skippedBy));

        return mapCheckpointToDto(checkpointRepository.save(checkpoint));
    }

    /* ── Lifecycle helpers ────────────────────────────────────────────────
     *
     * The service owns no status logic. These three helpers are the whole bridge
     * between a varchar column and the domain: resolve, ask, write.
     * ────────────────────────────────────────────────────────────────────── */

    private Tour requireTour(String id) {
        return tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", "id", id));
    }

    private Checkpoint requireCheckpoint(String id) {
        return checkpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint", "id", id));
    }

    /** Guarded write of a tour status. Throws 422 if the move is not in the table. */
    private void transition(Tour tour, TourneeStatus to) {
        TourExecutionMode mode = TourExecutionMode.fromCode(tour.getExecutionMode());
        Lifecycle.requireTransition(TourneeStatus.fromCode(tour.getStatus()), to, mode);
        tour.updateStatus(to.name(), Lifecycle.labelOf(to));
    }

    /** Guarded write of a checkpoint status. Throws 422 if the move is not legal. */
    private void transitionCheckpoint(Checkpoint checkpoint, CheckpointStatus to) {
        Lifecycle.requireTransition(CheckpointStatus.fromCode(checkpoint.getStatus()), to);
        checkpoint.updateStatus(to.name(), Lifecycle.labelOf(to));
    }

    private String actor(String username) {
        return username != null ? username : "SYSTEM";
    }

    private TourResponseDto mapToDto(Tour tour) {
        if (tour == null) {
            return null;
        }

        List<CheckpointResponseDto> checkpoints = checkpointRepository
                .findByTourIdOrderBySequenceAsc(tour.getId()).stream()
                .map(this::mapCheckpointToDto)
                .toList();

        return TourResponseDto.builder()
                /* ── Identité ─────────────────────────────────────── */
                .id(tour.getId())
                .tourCode(tour.getTourCode())
                /* ── Logistique & Affectation ────────────────────── */
                .marketerOrganizationId(tour.getMarketerOrganizationId())
                .executionMode(tour.getExecutionMode())
                .transporterOrganizationId(tour.getTransporterOrganizationId())
                .vehicleId(tour.getVehicleId())
                .driverId(tour.getDriverId())
                .driverPersonId(tour.getDriverPersonId())
                .assignedByTransporterPersonId(tour.getAssignedByTransporterPersonId())
                .transporterAssignedAt(tour.getTransporterAssignedAt())
                /* ── Type & Quantités ────────────────────────────── */
                .type(tour.getType())
                .requestedQuantity(tour.getRequestedQuantity())
                .loadedQuantity(tour.getLoadedQuantity())
                .deliveredQuantity(tour.getDeliveredQuantity())
                /* ── Cycle de vie opérationnel ───────────────────── */
                .startedAt(tour.getStartedAt())
                .closedAt(tour.getClosedAt())
                /* ── Statut (pattern Maximo) ─────────────────────── */
                .status(tour.getStatus())
                .statusDescription(tour.getStatusDescription())
                .statusDate(tour.getStatusDate())
                /* ── Audit de traçabilité (pattern Maximo) ───────── */
                .createdAt(tour.getCreatedAt())
                .createdBy(tour.getCreatedBy())
                .changedate(tour.getChangedate())
                .changeby(tour.getChangeby())
                .rowStamp(tour.getRowStamp())
                /* ── Arrêts ──────────────────────────────────────── */
                .checkpoints(checkpoints)
                .build();
    }

    private CheckpointResponseDto mapCheckpointToDto(Checkpoint cp) {
        if (cp == null) {
            return null;
        }
        return CheckpointResponseDto.builder()
                /* ── Identité ─────────────────────────────────────── */
                .id(cp.getId())
                .tourId(cp.getTourId())
                /* ── Destination ─────────────────────────────────── */
                .siteId(cp.getSiteId())
                .clientSiteId(cp.getClientSiteId())
                /* ── Séquence & Horaires ─────────────────────────── */
                .sequence(cp.getSequence())
                .expectedArrival(cp.getExpectedArrival())
                .actualArrival(cp.getActualArrival())
                /* ── Statut (pattern Maximo) ─────────────────────── */
                .status(cp.getStatus())
                .statusDescription(cp.getStatusDescription())
                .statusDate(cp.getStatusDate())
                .skipReason(cp.getSkipReason())
                /* ── Audit de traçabilité (pattern Maximo) ───────── */
                .createdAt(cp.getCreatedAt())
                .createdBy(cp.getCreatedBy())
                .changedate(cp.getChangedate())
                .changeby(cp.getChangeby())
                .rowStamp(cp.getRowStamp())
                .build();
    }
}
