package com.gpl.fleet.service.impl;

import com.gpl.common.exception.BusinessException;
import com.gpl.fleet.dto.TelemetryIngestRequest;
import com.gpl.fleet.dto.TelemetryResponse;
import com.gpl.fleet.model.VehicleTelemetry;
import com.gpl.fleet.repository.VehicleTelemetryRepository;
import com.gpl.fleet.service.VehicleTelemetryService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleTelemetryServiceImpl implements VehicleTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(VehicleTelemetryServiceImpl.class);
    private final VehicleTelemetryRepository telemetryRepository;
    private final GeometryFactory geometryFactory;

    public VehicleTelemetryServiceImpl(VehicleTelemetryRepository telemetryRepository) {
        this.telemetryRepository = telemetryRepository;
        // SRID 4326 for standard GPS coordinates (WGS 84)
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }

    @Override
    @Transactional
    public void ingestTelemetry(TelemetryIngestRequest request) {
        log.debug("Ingesting telemetry for vehicle {}", request.getVehicleId());
        
        Point location = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));
        Instant timestamp = request.getTimestamp() != null ? request.getTimestamp() : Instant.now();
        
        VehicleTelemetry telemetry = new VehicleTelemetry(
                request.getVehicleId(),
                timestamp,
                location,
                request.getSpeed(),
                request.getHeading(),
                request.getBatteryLevel()
        );
        
        telemetryRepository.save(telemetry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TelemetryResponse> getVehicleTrajectory(String vehicleId, Instant start, Instant end) {
        if (start == null) {
            start = Instant.now().minusSeconds(24 * 3600); // Default last 24h
        }
        if (end == null) {
            end = Instant.now();
        }
        
        List<VehicleTelemetry> history = telemetryRepository.findByVehicleIdAndTimestampBetweenOrderByTimestampAsc(vehicleId, start, end);
        return history.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TelemetryResponse getLatestPosition(String vehicleId) {
        return telemetryRepository.findFirstByVehicleIdOrderByTimestampDesc(vehicleId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new BusinessException("No telemetry data found for vehicle " + vehicleId));
    }
    
    private TelemetryResponse mapToResponse(VehicleTelemetry telemetry) {
        TelemetryResponse response = new TelemetryResponse();
        response.setId(telemetry.getId());
        response.setVehicleId(telemetry.getVehicleId());
        response.setLatitude(telemetry.getLocation().getY());
        response.setLongitude(telemetry.getLocation().getX());
        response.setSpeed(telemetry.getSpeed());
        response.setHeading(telemetry.getHeading());
        response.setBatteryLevel(telemetry.getBatteryLevel());
        response.setTimestamp(telemetry.getTimestamp());
        return response;
    }
}
