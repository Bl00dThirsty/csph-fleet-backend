package com.gpl.tour.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.tour.dto.CheckpointResponseDto;
import com.gpl.tour.dto.CreateCheckpointDto;
import com.gpl.tour.dto.UpdateCheckpointDto;
import com.gpl.tour.service.TourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gpl.common.security.RequiresPermission;

import java.util.List;

/**
 * REST Controller pour la gestion des arrêts de tournée (Checkpoints multi-clients / multi-sites).
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class CheckpointController {

    private final TourService tourService;

    /*
     * Récupère la liste des arrêts d'une tournée ordonnés par séquence.
     */
    @RequiresPermission("TOUR_VIEW")
    @GetMapping("/api/v1/tours/{tourId}/checkpoints")
    public ResponseEntity<ApiResponse<List<CheckpointResponseDto>>> getCheckpointsByTour(@PathVariable String tourId) {
        log.info("Requête REST pour récupérer les arrêts de la tournée ID: {}", tourId);
        List<CheckpointResponseDto> checkpoints = tourService.getCheckpointsByTour(tourId);
        return ResponseEntity.ok(ApiResponse.ok(checkpoints));
    }

    /*
     * Ajoute un nouvel arrêt (site ou clientSite) à une tournée.
     */
    @RequiresPermission("TOUR_UPDATE")
    @PostMapping("/api/v1/tours/{tourId}/checkpoints")
    public ResponseEntity<ApiResponse<CheckpointResponseDto>> addCheckpoint(
            @PathVariable String tourId,
            @Valid @RequestBody CreateCheckpointDto dto,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("Requête REST pour ajouter un arrêt à la tournée ID: {} (séquence {})", tourId, dto.getSequence());
        dto.setTourId(tourId);
        CheckpointResponseDto created = tourService.addCheckpoint(tourId, dto, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Arrêt de tournée créé avec succès"));
    }

    /*
     * Récupère un arrêt spécifique par son ID.
     */
    @RequiresPermission("TOUR_VIEW")
    @GetMapping("/api/v1/checkpoints/{id}")
    public ResponseEntity<ApiResponse<CheckpointResponseDto>> getCheckpointById(@PathVariable String id) {
        log.info("Requête REST pour récupérer l'arrêt ID: {}", id);
        CheckpointResponseDto checkpoint = tourService.getCheckpointById(id);
        return ResponseEntity.ok(ApiResponse.ok(checkpoint));
    }

    /*
     * Met à jour un arrêt de tournée par son ID.
     */
    @RequiresPermission("TOUR_UPDATE")
    @PutMapping("/api/v1/checkpoints/{id}")
    public ResponseEntity<ApiResponse<CheckpointResponseDto>> updateCheckpoint(
            @PathVariable String id,
            @Valid @RequestBody UpdateCheckpointDto dto,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("Requête REST pour mettre à jour l'arrêt ID: {}", id);
        CheckpointResponseDto updated = tourService.updateCheckpoint(id, dto, username);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Arrêt de tournée mis à jour avec succès"));
    }

    /*
     * Supprime un arrêt de tournée par son ID.
     */
    @RequiresPermission("TOUR_DELETE")
    @DeleteMapping("/api/v1/checkpoints/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCheckpoint(@PathVariable String id) {
        log.info("Requête REST pour supprimer l'arrêt ID: {}", id);
        tourService.deleteCheckpoint(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Arrêt de tournée supprimé avec succès"));
    }

    /* ── Phase 3 Checkpoint Execution Endpoints ───────────────────────────── */

    @RequiresPermission("CHECKPOINT_VALIDATE")
    @PostMapping("/api/v1/checkpoints/{id}/validate")
    public ResponseEntity<ApiResponse<CheckpointResponseDto>> validateCheckpoint(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("Requête REST pour valider l'arrêt ID: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(tourService.validateCheckpoint(id, username), "Arrêt de tournée validé avec succès"));
    }

    @RequiresPermission("CHECKPOINT_SKIP")
    @PostMapping("/api/v1/checkpoints/{id}/skip")
    public ResponseEntity<ApiResponse<CheckpointResponseDto>> skipCheckpoint(
            @PathVariable String id,
            @RequestParam String reason,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("Requête REST pour sauter l'arrêt ID: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(tourService.skipCheckpoint(id, reason, username), "Arrêt de tournée ignoré avec succès"));
    }
}
