package com.gpl.fleet.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.fleet.dto.AssignDeviceRequest;
import com.gpl.fleet.dto.CreateDeviceRequest;
import com.gpl.fleet.dto.DeviceResponse;
import com.gpl.fleet.dto.UpdateDeviceRequest;
import com.gpl.fleet.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final DeviceService deviceService;

    @RequiresPermission("fleet.devices.read")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DeviceResponse>>> listDevices(
            @RequestParam(required = false) String organizationId,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("REST request to list devices - org: {}, deviceType: {}, status: {}", organizationId, deviceType, status);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageResponse<DeviceResponse> result = deviceService.listDevices(organizationId, deviceType, status, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @RequiresPermission("fleet.devices.read")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> getDevice(@PathVariable String id) {
        log.info("REST request to get device with ID: {}", id);
        DeviceResponse result = deviceService.getDevice(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @RequiresPermission("fleet.devices.create")
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceResponse>> createDevice(
            @Valid @RequestBody CreateDeviceRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String createdBy) {
        log.info("REST request to create device with serial: {}", request.getSerialNumber());
        DeviceResponse result = deviceService.createDevice(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "Équipement créé avec succès"));
    }

    @RequiresPermission("fleet.devices.write")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
            @PathVariable String id,
            @Valid @RequestBody UpdateDeviceRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String changedBy) {
        log.info("REST request to update device: {}", id);
        DeviceResponse result = deviceService.updateDevice(id, request, changedBy);
        return ResponseEntity.ok(ApiResponse.ok(result, "Équipement mis à jour avec succès"));
    }

    @RequiresPermission("fleet.devices.manage")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable String id) {
        log.info("REST request to delete device: {}", id);
        deviceService.deleteDevice(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Équipement supprimé avec succès"));
    }

    @RequiresPermission("fleet.devices.write")
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<DeviceResponse>> assignDevice(
            @PathVariable String id,
            @RequestBody AssignDeviceRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String changedBy) {
        log.info("REST request to assign device: {}", id);
        DeviceResponse result = deviceService.assignDevice(id, request, changedBy);
        return ResponseEntity.ok(ApiResponse.ok(result, "Équipement assigné avec succès"));
    }

    @RequiresPermission("fleet.devices.write")
    @PostMapping("/{id}/unassign")
    public ResponseEntity<ApiResponse<DeviceResponse>> unassignDevice(
            @PathVariable String id,
            @RequestHeader(value = "X-User-PersonId", required = false) String changedBy) {
        log.info("REST request to unassign device: {}", id);
        DeviceResponse result = deviceService.unassignDevice(id, changedBy);
        return ResponseEntity.ok(ApiResponse.ok(result, "Équipement désassigné avec succès"));
    }
}
