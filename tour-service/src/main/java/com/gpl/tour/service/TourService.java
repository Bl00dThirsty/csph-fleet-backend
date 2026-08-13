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
}
