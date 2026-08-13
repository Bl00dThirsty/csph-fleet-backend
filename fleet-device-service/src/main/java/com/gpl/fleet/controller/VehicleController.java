package com.gpl.fleet.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.fleet.dto.CreateVehicleRequest;
import com.gpl.fleet.dto.UpdateVehicleRequest;
import com.gpl.fleet.dto.VehicleResponse;
import com.gpl.fleet.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;

    @RequiresPermission("fleet.vehicles.read")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VehicleResponse>>> listVehicles(
            @RequestParam(required = false) String organizationId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("REST request to list vehicles - org: {}, type: {}, status: {}", organizationId, type, status);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageResponse<VehicleResponse> result = vehicleService.listVehicles(organizationId, type, status, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @RequiresPermission("fleet.vehicles.read")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(@PathVariable String id) {
        log.info("REST request to get vehicle with ID: {}", id);
        VehicleResponse result = vehicleService.getVehicle(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @RequiresPermission("fleet.vehicles.create")
    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String createdBy) {
        log.info("REST request to create vehicle: {}", request.getLicensePlate());
        VehicleResponse result = vehicleService.createVehicle(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "Véhicule créé avec succès"));
    }

    @RequiresPermission("fleet.vehicles.write")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable String id,
            @Valid @RequestBody UpdateVehicleRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String changedBy) {
        log.info("REST request to update vehicle: {}", id);
        VehicleResponse result = vehicleService.updateVehicle(id, request, changedBy);
        return ResponseEntity.ok(ApiResponse.ok(result, "Véhicule mis à jour avec succès"));
    }

    @RequiresPermission("fleet.vehicles.manage")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable String id) {
        log.info("REST request to delete vehicle: {}", id);
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Véhicule supprimé avec succès"));
    }
}
