package com.gpl.fleet.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.BusinessException;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.fleet.dto.AssignDeviceRequest;
import com.gpl.fleet.dto.CreateDeviceRequest;
import com.gpl.fleet.dto.DeviceResponse;
import com.gpl.fleet.dto.UpdateDeviceRequest;
import com.gpl.fleet.model.Device;
import com.gpl.fleet.repository.DeviceRepository;
import com.gpl.fleet.service.DeviceService;
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
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    @Override
    public DeviceResponse createDevice(CreateDeviceRequest request, String createdBy) {
        log.info("Creating device with serial number: {}", request.getSerialNumber());
        if (request.getSerialNumber() != null && deviceRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new BusinessException("Un équipement avec le numéro de série '" + request.getSerialNumber() + "' existe déjà.");
        }

        Device device = new Device();
        device.setSerialNumber(request.getSerialNumber());
        device.setDeviceType(request.getDeviceType());
        device.setFirmwareVersion(request.getFirmwareVersion());
        device.setBatteryLevel(request.getBatteryLevel() != null ? request.getBatteryLevel() : 100);
        device.setBatteryCritical(request.getBatteryCritical() != null ? request.getBatteryCritical() : false);
        device.setOrganizationId(request.getOrganizationId());
        device.setAssignedToPersonId(request.getAssignedToPersonId());
        device.setAssignedToVehicleId(request.getAssignedToVehicleId());
        if (request.getStatus() != null || request.getStatusDescription() != null) {
            device.updateStatus(
                    request.getStatus() != null ? request.getStatus() : device.getStatus(),
                    request.getStatusDescription() != null
                            ? request.getStatusDescription() : device.getStatusDescription());
        }
        device.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");

        Device saved = deviceRepository.save(device);
        log.debug("Device created successfully with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    public DeviceResponse updateDevice(String id, UpdateDeviceRequest request, String changedBy) {
        log.info("Updating device with ID: {}", id);
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", id));

        if (request.getSerialNumber() != null && !request.getSerialNumber().equalsIgnoreCase(device.getSerialNumber())) {
            if (deviceRepository.existsBySerialNumber(request.getSerialNumber())) {
                throw new BusinessException("Un équipement avec le numéro de série '" + request.getSerialNumber() + "' existe déjà.");
            }
            device.setSerialNumber(request.getSerialNumber());
        }
        if (request.getDeviceType() != null) {
            device.setDeviceType(request.getDeviceType());
        }
        if (request.getFirmwareVersion() != null) {
            device.setFirmwareVersion(request.getFirmwareVersion());
        }
        if (request.getBatteryLevel() != null) {
            device.setBatteryLevel(request.getBatteryLevel());
        }
        if (request.getBatteryCritical() != null) {
            device.setBatteryCritical(request.getBatteryCritical());
        }
        if (request.getLastSync() != null) {
            device.setLastSync(request.getLastSync());
        }
        if (request.getLastLatitude() != null) {
            device.setLastLatitude(request.getLastLatitude());
        }
        if (request.getLastLongitude() != null) {
            device.setLastLongitude(request.getLastLongitude());
        }
        if (request.getOrganizationId() != null) {
            device.setOrganizationId(request.getOrganizationId());
        }
        if (request.getAssignedToPersonId() != null) {
            device.setAssignedToPersonId(request.getAssignedToPersonId());
        }
        if (request.getAssignedToVehicleId() != null) {
            device.setAssignedToVehicleId(request.getAssignedToVehicleId());
        }
        if (request.getStatus() != null || request.getStatusDescription() != null) {
            device.updateStatus(
                    request.getStatus() != null ? request.getStatus() : device.getStatus(),
                    request.getStatusDescription() != null
                            ? request.getStatusDescription() : device.getStatusDescription());
        }
        device.setChangeby(changedBy != null ? changedBy : "SYSTEM");

        Device updated = deviceRepository.save(device);
        log.debug("Device updated successfully with ID: {}", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponse getDevice(String id) {
        log.info("Fetching device with ID: {}", id);
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", id));
        return mapToResponse(device);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeviceResponse> listDevices(String organizationId, String deviceType, String status, Pageable pageable) {
        log.info("Listing devices with filters - organizationId: {}, deviceType: {}, status: {}", organizationId, deviceType, status);
        Specification<Device> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (organizationId != null && !organizationId.isBlank()) {
                predicates.add(cb.equal(root.get("organizationId"), organizationId));
            }
            if (deviceType != null && !deviceType.isBlank()) {
                predicates.add(cb.equal(root.get("deviceType"), deviceType));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Device> page = deviceRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(this::mapToResponse));
    }

    @Override
    public void deleteDevice(String id) {
        log.info("Deleting device with ID: {}", id);
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", id));
        deviceRepository.delete(device);
        log.debug("Device deleted successfully: {}", id);
    }

    @Override
    public DeviceResponse assignDevice(String id, AssignDeviceRequest request, String changedBy) {
        log.info("Assigning device ID: {} to personId: {} / vehicleId: {}", id, request.getAssignedToPersonId(), request.getAssignedToVehicleId());
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", id));

        device.setAssignedToPersonId(request.getAssignedToPersonId());
        device.setAssignedToVehicleId(request.getAssignedToVehicleId());
        device.setChangeby(changedBy != null ? changedBy : "SYSTEM");

        Device updated = deviceRepository.save(device);
        log.debug("Device assigned successfully: {}", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    public DeviceResponse unassignDevice(String id, String changedBy) {
        log.info("Unassigning device ID: {}", id);
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", id));

        device.setAssignedToPersonId(null);
        device.setAssignedToVehicleId(null);
        device.setChangeby(changedBy != null ? changedBy : "SYSTEM");

        Device updated = deviceRepository.save(device);
        log.debug("Device unassigned successfully: {}", updated.getId());
        return mapToResponse(updated);
    }

    private DeviceResponse mapToResponse(Device device) {
        if (device == null) {
            return null;
        }
        return DeviceResponse.builder()
                .id(device.getId())
                .serialNumber(device.getSerialNumber())
                .deviceType(device.getDeviceType())
                .firmwareVersion(device.getFirmwareVersion())
                .batteryLevel(device.getBatteryLevel())
                .batteryCritical(device.isBatteryCritical())
                .lastSync(device.getLastSync())
                .lastLatitude(device.getLastLatitude())
                .lastLongitude(device.getLastLongitude())
                .assignedToPersonId(device.getAssignedToPersonId())
                .assignedToVehicleId(device.getAssignedToVehicleId())
                .organizationId(device.getOrganizationId())
                .status(device.getStatus())
                .statusDescription(device.getStatusDescription())
                .statusDate(device.getStatusDate())
                .createdAt(device.getCreatedAt())
                .createdBy(device.getCreatedBy())
                .changedate(device.getChangedate())
                .changeby(device.getChangeby())
                .rowStamp(device.getRowStamp())
                .build();
    }
}
