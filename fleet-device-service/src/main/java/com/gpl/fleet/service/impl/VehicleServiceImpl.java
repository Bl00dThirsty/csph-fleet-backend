package com.gpl.fleet.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.BusinessException;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.fleet.dto.CreateVehicleRequest;
import com.gpl.fleet.dto.UpdateVehicleRequest;
import com.gpl.fleet.dto.VehicleResponse;
import com.gpl.fleet.model.Vehicle;
import com.gpl.fleet.repository.VehicleRepository;
import com.gpl.fleet.service.VehicleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    public VehicleResponse createVehicle(CreateVehicleRequest request, String createdBy) {
        log.info("Creating vehicle with license plate: {}", request.getLicensePlate());
        if (request.getLicensePlate() != null && vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new BusinessException("Un véhicule avec l'immatriculation '" + request.getLicensePlate() + "' existe déjà.");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setType(request.getType());
        vehicle.setOrganizationId(request.getOrganizationId());
        vehicle.setMaxVolume(request.getMaxVolume());
        vehicle.setMaxBottleCount(request.getMaxBottleCount());
        vehicle.setCertificateUrl(request.getCertificateUrl());
        vehicle.setCertificateNumber(request.getCertificateNumber());
        vehicle.setCertificateExpiryAt(request.getCertificateExpiryAt());
        vehicle.setTareWeight(request.getTareWeight());
        if (request.getIsActive() != null) {
            vehicle.setActive(request.getIsActive());
        }
        if (request.getStatus() != null || request.getStatusDescription() != null) {
            vehicle.updateStatus(
                    request.getStatus() != null ? request.getStatus() : vehicle.getStatus(),
                    request.getStatusDescription() != null
                            ? request.getStatusDescription() : vehicle.getStatusDescription());
        }
        vehicle.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");

        Vehicle saved = vehicleRepository.save(vehicle);
        log.debug("Vehicle created successfully with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    public VehicleResponse updateVehicle(String id, UpdateVehicleRequest request, String changedBy) {
        log.info("Updating vehicle with ID: {}", id);
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));

        if (request.getLicensePlate() != null && !request.getLicensePlate().equalsIgnoreCase(vehicle.getLicensePlate())) {
            if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
                throw new BusinessException("Un véhicule avec l'immatriculation '" + request.getLicensePlate() + "' existe déjà.");
            }
            vehicle.setLicensePlate(request.getLicensePlate());
        }
        if (request.getType() != null) {
            vehicle.setType(request.getType());
        }
        if (request.getOrganizationId() != null) {
            vehicle.setOrganizationId(request.getOrganizationId());
        }
        if (request.getMaxVolume() != null) {
            vehicle.setMaxVolume(request.getMaxVolume());
        }
        if (request.getMaxBottleCount() != null) {
            vehicle.setMaxBottleCount(request.getMaxBottleCount());
        }
        if (request.getCertificateUrl() != null) {
            vehicle.setCertificateUrl(request.getCertificateUrl());
        }
        if (request.getCertificateNumber() != null) {
            vehicle.setCertificateNumber(request.getCertificateNumber());
        }
        if (request.getCertificateExpiryAt() != null) {
            vehicle.setCertificateExpiryAt(request.getCertificateExpiryAt());
        }
        if (request.getTareWeight() != null) {
            vehicle.setTareWeight(request.getTareWeight());
        }
        if (request.getIsActive() != null) {
            vehicle.setActive(request.getIsActive());
        }
        if (request.getStatus() != null || request.getStatusDescription() != null) {
            vehicle.updateStatus(
                    request.getStatus() != null ? request.getStatus() : vehicle.getStatus(),
                    request.getStatusDescription() != null
                            ? request.getStatusDescription() : vehicle.getStatusDescription());
        }
        vehicle.setChangeby(changedBy != null ? changedBy : "SYSTEM");

        Vehicle updated = vehicleRepository.save(vehicle);
        log.debug("Vehicle updated successfully with ID: {}", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(String id) {
        log.info("Fetching vehicle with ID: {}", id);
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));
        return mapToResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> listVehicles(String organizationId, String type, String status, Pageable pageable) {
        log.info("Listing vehicles with filters - organizationId: {}, type: {}, status: {}", organizationId, type, status);
        Specification<Vehicle> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (organizationId != null && !organizationId.isBlank()) {
                predicates.add(cb.equal(root.get("organizationId"), organizationId));
            }
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Vehicle> page = vehicleRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(this::mapToResponse));
    }

    @Override
    public void deleteVehicle(String id) {
        log.info("Deleting vehicle with ID: {}", id);
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));
        vehicleRepository.delete(vehicle);
        log.debug("Vehicle deleted successfully: {}", id);
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .type(vehicle.getType())
                .organizationId(vehicle.getOrganizationId())
                .maxVolume(vehicle.getMaxVolume())
                .maxBottleCount(vehicle.getMaxBottleCount())
                .certificateUrl(vehicle.getCertificateUrl())
                .certificateNumber(vehicle.getCertificateNumber())
                .certificateExpiryAt(vehicle.getCertificateExpiryAt())
                .tareWeight(vehicle.getTareWeight())
                .isActive(vehicle.isActive())
                .status(vehicle.getStatus())
                .statusDescription(vehicle.getStatusDescription())
                .statusDate(vehicle.getStatusDate())
                .createdAt(vehicle.getCreatedAt())
                .createdBy(vehicle.getCreatedBy())
                .changedate(vehicle.getChangedate())
                .changeby(vehicle.getChangeby())
                .rowStamp(vehicle.getRowStamp())
                .build();
    }
}
