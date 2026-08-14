package com.gpl.fleet.repository;

import com.gpl.fleet.model.VehicleTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleTelemetryRepository extends JpaRepository<VehicleTelemetry, UUID> {
    
    List<VehicleTelemetry> findByVehicleIdAndTimestampBetweenOrderByTimestampAsc(String vehicleId, Instant start, Instant end);
    
    Optional<VehicleTelemetry> findFirstByVehicleIdOrderByTimestampDesc(String vehicleId);
}
