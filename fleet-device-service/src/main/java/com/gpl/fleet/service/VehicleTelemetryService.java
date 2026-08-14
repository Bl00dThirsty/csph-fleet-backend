package com.gpl.fleet.service;

import com.gpl.fleet.dto.TelemetryIngestRequest;
import com.gpl.fleet.dto.TelemetryResponse;

import java.time.Instant;
import java.util.List;

public interface VehicleTelemetryService {
    
    void ingestTelemetry(TelemetryIngestRequest request);
    
    List<TelemetryResponse> getVehicleTrajectory(String vehicleId, Instant start, Instant end);
    
    TelemetryResponse getLatestPosition(String vehicleId);
}
