package com.gpl.tour.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.tour.dto.CreateTourDto;
import com.gpl.tour.dto.TourResponseDto;
import com.gpl.tour.dto.UpdateTourDto;
import com.gpl.tour.service.TourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gpl.common.security.RequiresPermission;

/**
 * REST Controller for Tour management.
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@RestController
@RequestMapping("/api/v1/tours")
@RequiredArgsConstructor
@Slf4j
public class TourController {

    private final TourService tourService;

    /*
     * Retrieves all tours with pagination.
     */
    @RequiresPermission("TOUR_VIEW")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TourResponseDto>>> listTours(Pageable pageable) {
        log.info("REST request to get a page of Tours");
        return ResponseEntity.ok(ApiResponse.ok(tourService.getAll(pageable)));
    }

    /*
     * Creates a new tour.
     */
    @RequiresPermission("TOUR_CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<TourResponseDto>> createTour(
            @Valid @RequestBody CreateTourDto dto,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to save Tour: {}", dto.getTourCode());
        TourResponseDto result = tourService.create(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "Tour created successfully"));
    }

    /*
     * Retrieves a tour by ID.
     */
    @RequiresPermission("TOUR_VIEW")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TourResponseDto>> getTour(@PathVariable String id) {
        log.info("REST request to get Tour: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(tourService.getById(id)));
    }

    /*
     * Updates a tour by ID.
     */
    @RequiresPermission("TOUR_UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TourResponseDto>> updateTour(
            @PathVariable String id,
            @Valid @RequestBody UpdateTourDto dto,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to update Tour: {}", id);
        TourResponseDto result = tourService.update(id, dto, username);
        return ResponseEntity.ok(ApiResponse.ok(result, "Tour updated successfully"));
    }

    /*
     * Deletes a tour by ID.
     */
    @RequiresPermission("TOUR_DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTour(@PathVariable String id) {
        log.info("REST request to delete Tour: {}", id);
        tourService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Tour deleted successfully"));
    }

    /* ── Phase 3 Lifecycle & Dynamic Assignment Endpoints ─────────────────── */

    @RequiresPermission("TOUR_START")
    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<TourResponseDto>> startTour(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to start Tour: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(tourService.startTour(id, username), "Tournée démarrée avec succès"));
    }

    @RequiresPermission("TOUR_CLOSE")
    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<TourResponseDto>> closeTour(
            @PathVariable String id,
            @RequestParam(required = false) Double loadedQuantity,
            @RequestParam(required = false) Double deliveredQuantity,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to close Tour: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(tourService.closeTour(id, loadedQuantity, deliveredQuantity, username), "Tournée clôturée avec succès"));
    }

    @RequiresPermission("TOUR_CANCEL")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<TourResponseDto>> cancelTour(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to cancel Tour: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(tourService.cancelTour(id, reason, username), "Tournée annulée avec succès"));
    }

    @RequiresPermission("TOUR_ASSIGN_DRIVER")
    @PostMapping("/{id}/assign-driver")
    public ResponseEntity<ApiResponse<TourResponseDto>> assignDriver(
            @PathVariable String id,
            @RequestParam(required = false) String driverId,
            @RequestParam(required = false) String livreurPersonId,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to assign driver to Tour: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(tourService.assignDriver(id, driverId, livreurPersonId, username), "Chauffeur assigné avec succès"));
    }

    @RequiresPermission("TOUR_ASSIGN_VEHICLE")
    @PostMapping("/{id}/assign-vehicle")
    public ResponseEntity<ApiResponse<TourResponseDto>> assignVehicle(
            @PathVariable String id,
            @RequestParam String vehicleId,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("REST request to assign vehicle to Tour: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(tourService.assignVehicle(id, vehicleId, username), "Véhicule assigné avec succès"));
    }
}
