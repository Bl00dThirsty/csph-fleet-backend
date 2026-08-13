package com.gpl.tour.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.BusinessException;
import com.gpl.common.exception.ResourceNotFoundException;
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
        tour.setLivreurPersonId(dto.getLivreurPersonId());

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
     * Retrieves all Tours with pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TourResponseDto> getAll(Pageable pageable) {
        log.info("Fetching all Tours with pagination");
        Page<Tour> page = tourRepository.findAll(pageable);
        return PageResponse.of(page.map(this::mapToDto));
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
        if (dto.getLivreurPersonId() != null) {
            tour.setLivreurPersonId(dto.getLivreurPersonId());
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
        if (dto.getActualArrival() != null) {
            checkpoint.setActualArrival(dto.getActualArrival());
        }
        if (dto.getStatus() != null) {
            checkpoint.setStatus(dto.getStatus());
        }
        if (dto.getSkipReason() != null) {
            checkpoint.setSkipReason(dto.getSkipReason());
        }

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

    /* ── Lifecycle Operation Workflows ────────────────────────────────────── */

    @Override
    public TourResponseDto startTour(String id, String startedBy) {
        log.info("Starting Tour ID: {} by user: {}", id, startedBy);
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", "id", id));

        if ("COMPLETED".equals(tour.getStatus()) || "CANCELLED".equals(tour.getStatus()) || "CLOSED".equals(tour.getStatus())) {
            throw new BusinessException("Impossible de démarrer une tournée qui est déjà terminée ou annulée. Statut actuel: " + tour.getStatus());
        }

        tour.setStatus("STARTED");
        tour.setStatusDescription("Tournée démarrée et en cours d'exécution");
        tour.setStatusDate(java.time.Instant.now());
        tour.setStartedAt(java.time.Instant.now());
        tour.setChangeby(startedBy != null ? startedBy : "SYSTEM");

        Tour saved = tourRepository.save(tour);
        return mapToDto(saved);
    }

    @Override
    public TourResponseDto closeTour(String id, Double loadedQuantity, Double deliveredQuantity, String closedBy) {
        log.info("Closing Tour ID: {} by user: {}", id, closedBy);
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", "id", id));

        if ("CLOSED".equals(tour.getStatus()) || "CANCELLED".equals(tour.getStatus())) {
            throw new BusinessException("La tournée ID " + id + " est déjà clôturée ou annulée.");
        }

        tour.setStatus("CLOSED");
        tour.setStatusDescription("Tournée achevée et clôturée avec succès");
        tour.setStatusDate(java.time.Instant.now());
        tour.setClosedAt(java.time.Instant.now());

        if (loadedQuantity != null) {
            tour.setLoadedQuantity(loadedQuantity);
        }
        if (deliveredQuantity != null) {
            tour.setDeliveredQuantity(deliveredQuantity);
        }
        tour.setChangeby(closedBy != null ? closedBy : "SYSTEM");

        Tour saved = tourRepository.save(tour);
        return mapToDto(saved);
    }

    @Override
    public TourResponseDto cancelTour(String id, String reason, String cancelledBy) {
        log.info("Cancelling Tour ID: {} by user: {}, reason: {}", id, cancelledBy, reason);
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", "id", id));

        if ("CLOSED".equals(tour.getStatus()) || "COMPLETED".equals(tour.getStatus())) {
            throw new BusinessException("Impossible d'annuler une tournée déjà clôturée.");
        }

        tour.setStatus("CANCELLED");
        tour.setStatusDescription(reason != null && !reason.isBlank() ? "Annulée: " + reason : "Tournée annulée");
        tour.setStatusDate(java.time.Instant.now());
        tour.setChangeby(cancelledBy != null ? cancelledBy : "SYSTEM");

        Tour saved = tourRepository.save(tour);
        return mapToDto(saved);
    }

    @Override
    public TourResponseDto assignDriver(String id, String driverId, String livreurPersonId, String assignedBy) {
        log.info("Assigning driver to Tour ID: {}, driverId: {}, livreurPersonId: {}", id, driverId, livreurPersonId);
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", "id", id));

        if (driverId != null) {
            tour.setDriverId(driverId);
        }
        if (livreurPersonId != null) {
            tour.setLivreurPersonId(livreurPersonId);
        }

        if ("EXTERNAL".equalsIgnoreCase(tour.getExecutionMode())) {
            tour.setAssignedByTransporterPersonId(assignedBy != null ? assignedBy : "SYSTEM");
            tour.setTransporterAssignedAt(java.time.Instant.now());
        }

        tour.setChangeby(assignedBy != null ? assignedBy : "SYSTEM");
        Tour saved = tourRepository.save(tour);
        return mapToDto(saved);
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
    public CheckpointResponseDto validateCheckpoint(String checkpointId, String validatedBy) {
        log.info("Validating Checkpoint ID: {} by user: {}", checkpointId, validatedBy);
        Checkpoint checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint", "id", checkpointId));

        checkpoint.setStatus("VALIDATED");
        checkpoint.setStatusDescription("Arrêt validé et confirmé");
        checkpoint.setStatusDate(java.time.Instant.now());
        checkpoint.setActualArrival(java.time.Instant.now());
        checkpoint.setChangeby(validatedBy != null ? validatedBy : "SYSTEM");

        Checkpoint saved = checkpointRepository.save(checkpoint);
        return mapCheckpointToDto(saved);
    }

    @Override
    public CheckpointResponseDto skipCheckpoint(String checkpointId, String reason, String skippedBy) {
        log.info("Skipping Checkpoint ID: {} with reason: {}", checkpointId, reason);
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Un motif de saut est obligatoire pour ignorer un arrêt de tournée.");
        }

        Checkpoint checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint", "id", checkpointId));

        checkpoint.setStatus("SKIPPED");
        checkpoint.setStatusDescription("Arrêt sauté : " + reason);
        checkpoint.setStatusDate(java.time.Instant.now());
        checkpoint.setSkipReason(reason);
        checkpoint.setChangeby(skippedBy != null ? skippedBy : "SYSTEM");

        Checkpoint saved = checkpointRepository.save(checkpoint);
        return mapCheckpointToDto(saved);
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
                .livreurPersonId(tour.getLivreurPersonId())
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
