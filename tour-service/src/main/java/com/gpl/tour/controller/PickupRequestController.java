package com.gpl.tour.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.tour.dto.CreatePickupRequestDto;
import com.gpl.tour.dto.PickupRequestResponseDto;
import com.gpl.tour.dto.UpdatePickupRequestDto;
import com.gpl.tour.service.PickupRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour la gestion des demandes d'enlèvement vrac GPL dans les grands dépôts pétroliers (SCDP / SNH).
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   13.08.2026
 */
@RestController
@RequestMapping("/api/v1/pickups")
@RequiredArgsConstructor
public class PickupRequestController {

    private final PickupRequestService pickupRequestService;

    @RequiresPermission("PICKUP_VIEW")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PickupRequestResponseDto>>> listPickupRequests(
            @RequestParam(required = false) String marketerOrganizationId,
            @RequestParam(required = false) String sourceSiteId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(pickupRequestService.listPickupRequests(marketerOrganizationId, sourceSiteId, status, pageable)));
    }

    @RequiresPermission("PICKUP_VIEW")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PickupRequestResponseDto>> getPickupRequest(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(pickupRequestService.getPickupRequest(id)));
    }

    @RequiresPermission("PICKUP_CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<PickupRequestResponseDto>> createPickupRequest(
            @Valid @RequestBody CreatePickupRequestDto dto,
            @RequestHeader(value = "X-User-PersonId", required = false) String createdBy) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(pickupRequestService.createPickupRequest(dto, createdBy)));
    }

    @RequiresPermission("PICKUP_UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PickupRequestResponseDto>> updatePickupRequest(
            @PathVariable String id,
            @Valid @RequestBody UpdatePickupRequestDto dto,
            @RequestHeader(value = "X-User-PersonId", required = false) String updatedBy) {
        return ResponseEntity.ok(ApiResponse.success(pickupRequestService.updatePickupRequest(id, dto, updatedBy)));
    }

    @RequiresPermission("PICKUP_APPROVE")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PickupRequestResponseDto>> approvePickupRequest(
            @PathVariable String id,
            @RequestParam double approvedQuantity,
            @RequestHeader(value = "X-User-PersonId", required = false) String approvedBy) {
        return ResponseEntity.ok(ApiResponse.success(pickupRequestService.approvePickupRequest(id, approvedQuantity, approvedBy)));
    }

    @RequiresPermission("PICKUP_APPROVE")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<PickupRequestResponseDto>> rejectPickupRequest(
            @PathVariable String id,
            @RequestHeader(value = "X-User-PersonId", required = false) String rejectedBy) {
        return ResponseEntity.ok(ApiResponse.success(pickupRequestService.rejectPickupRequest(id, rejectedBy)));
    }

    @RequiresPermission("PICKUP_DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePickupRequest(@PathVariable String id) {
        pickupRequestService.deletePickupRequest(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
