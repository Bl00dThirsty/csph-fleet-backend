package com.gpl.fleet.controller;

import com.gpl.common.security.RequiresPermission;
import com.gpl.fleet.dto.TelemetryIngestRequest;
import com.gpl.fleet.dto.TelemetryResponse;
import com.gpl.fleet.service.VehicleTelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/telemetry")
@Tag(name = "Telemetry", description = "Vehicle Telemetry API")
public class VehicleTelemetryController {

    private final VehicleTelemetryService telemetryService;

    public VehicleTelemetryController(VehicleTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping
    @Operation(summary = "Ingest vehicle telemetry data (GPS coordinates)")
    // Note: Assuming "TELEMETRY_INGEST" permission is needed, or this endpoint could be secured via API Key for IoT devices
    @RequiresPermission("TELEMETRY_INGEST")
    public ResponseEntity<Void> ingestTelemetry(@Valid @RequestBody TelemetryIngestRequest request) {
        telemetryService.ingestTelemetry(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/vehicles/{vehicleId}/latest")
    @Operation(summary = "Get the latest known position of a vehicle")
    @RequiresPermission("TELEMETRY_VIEW")
    public ResponseEntity<TelemetryResponse> getLatestPosition(@PathVariable String vehicleId) {
        TelemetryResponse response = telemetryService.getLatestPosition(vehicleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vehicles/{vehicleId}")
    @Operation(summary = "Get the trajectory of a vehicle within a time range")
    @RequiresPermission("TELEMETRY_VIEW")
    public ResponseEntity<List<TelemetryResponse>> getVehicleTrajectory(
            @PathVariable String vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        
        List<TelemetryResponse> trajectory = telemetryService.getVehicleTrajectory(vehicleId, start, end);
        return ResponseEntity.ok(trajectory);
    }
}
