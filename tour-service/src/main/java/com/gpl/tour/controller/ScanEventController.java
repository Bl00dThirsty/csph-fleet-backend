package com.gpl.tour.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.tour.dto.BulkScanEventsDto;
import com.gpl.tour.dto.CreateScanEventDto;
import com.gpl.tour.dto.ScanEventResponseDto;
import com.gpl.tour.service.ScanEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller pour l'enregistrement des événements de scan RFID.
 *
 * <p>Les scans sont émis par le PDA embarqué du livreur lors du passage sur
 * un arrêt de tournée (checkpoint). Une requête par scan en mode online ;
 * le mode offline accumule localement puis pousse tout en une requête {@code /bulk}
 * lors de la resynchronisation.</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   28.09.2026
 */
@RestController
@RequestMapping("/api/v1/scan-events")
@RequiredArgsConstructor
@Slf4j
public class ScanEventController {

    private final ScanEventService scanEventService;

    /*
     * Enregistre un événement de scan RFID unitaire.
     */
    @RequiresPermission("SCAN_CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<ScanEventResponseDto>> createScanEvent(
            @Valid @RequestBody CreateScanEventDto dto,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("Requête REST pour enregistrer un scan sur le checkpoint {} (direction={}, meterReading={})",
                dto.getCheckpointId(), dto.getDirection(), dto.getMeterReading());
        ScanEventResponseDto created = scanEventService.create(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Événement de scan enregistré avec succès"));
    }

    /*
     * Enregistre un lot d'événements de scan RFID (resynchronisation offline du PDA).
     */
    @RequiresPermission("SCAN_CREATE")
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<ScanEventResponseDto>>> createScanEventsBulk(
            @Valid @RequestBody BulkScanEventsDto dto,
            @RequestHeader(value = "X-User-Username", required = false) String username) {
        log.info("Requête REST pour enregistrer {} scans en lot", dto.getItems() != null ? dto.getItems().size() : 0);
        List<ScanEventResponseDto> created = scanEventService.createBulk(dto.getItems(), username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Lot d'événements de scan enregistré avec succès"));
    }
}