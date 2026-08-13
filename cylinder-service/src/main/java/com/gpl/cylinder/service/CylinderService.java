package com.gpl.cylinder.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.cylinder.dto.CreateCylinderRequest;
import com.gpl.cylinder.dto.UpdateCylinderRequest;
import com.gpl.cylinder.dto.CylinderResponse;
import org.springframework.data.domain.Pageable;

public interface CylinderService {
    CylinderResponse createCylinder(CreateCylinderRequest request, String createdBy);
    CylinderResponse updateCylinder(String id, UpdateCylinderRequest request, String changedBy);
    CylinderResponse getCylinder(String id);
    PageResponse<CylinderResponse> listCylinders(
            String ownerOrganizationId,
            String currentSiteId,
            String brand,
            Double capacityKg,
            String status,
            Pageable pageable);
    void deleteCylinder(String id);
}
