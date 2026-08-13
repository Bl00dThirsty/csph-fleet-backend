package com.gpl.fleet.repository;

import com.gpl.fleet.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    Optional<Vehicle> findByLicensePlate(String licensePlate);
}
