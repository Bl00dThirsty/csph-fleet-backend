package com.gpl.fleet.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.fleet.dto.CreateVehicleRequest;
import com.gpl.fleet.dto.UpdateVehicleRequest;
import com.gpl.fleet.dto.VehicleResponse;
import org.springframework.data.domain.Pageable;

public interface VehicleService {

    VehicleResponse createVehicle(CreateVehicleRequest request, String createdBy);

    VehicleResponse updateVehicle(String id, UpdateVehicleRequest request, String changedBy);

    VehicleResponse getVehicle(String id);

    PageResponse<VehicleResponse> listVehicles(String organizationId, String type, String status, Pageable pageable);

    void deleteVehicle(String id);
}
