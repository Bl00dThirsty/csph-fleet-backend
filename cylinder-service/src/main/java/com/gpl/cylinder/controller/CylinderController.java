package com.gpl.cylinder.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.cylinder.model.Cylinder;
import com.gpl.cylinder.repository.CylinderRepository;
import com.gpl.common.security.RequiresPermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cylinders")
public class CylinderController {
    private final CylinderRepository cylinderRepository;

    public CylinderController(CylinderRepository cylinderRepository) {
        this.cylinderRepository = cylinderRepository;
    }

    @RequiresPermission("CYLINDER_VIEW")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Cylinder>>> listCylinders() {
        return ResponseEntity.ok(ApiResponse.ok(cylinderRepository.findAll()));
    }

    @RequiresPermission("CYLINDER_CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<Cylinder>> createCylinder(@RequestBody Cylinder cylinder) {
        return ResponseEntity.ok(ApiResponse.ok(cylinderRepository.save(cylinder)));
    }
}
