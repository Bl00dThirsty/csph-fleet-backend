package com.gpl.fleet.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.fleet.model.Vehicle;
import com.gpl.fleet.repository.VehicleRepository;
import com.gpl.common.security.RequiresPermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {
    private final VehicleRepository vehicleRepository;

    public VehicleController(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @RequiresPermission("VEHICLE_VIEW")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Vehicle>>> listVehicles() {
        return ResponseEntity.ok(ApiResponse.ok(vehicleRepository.findAll()));
    }

    @RequiresPermission("VEHICLE_CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<Vehicle>> createVehicle(@RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleRepository.save(vehicle)));
    }
}
