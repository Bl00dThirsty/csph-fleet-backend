package com.gpl.fleet.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.fleet.dto.AssignDeviceRequest;
import com.gpl.fleet.dto.CreateDeviceRequest;
import com.gpl.fleet.dto.DeviceResponse;
import com.gpl.fleet.dto.UpdateDeviceRequest;
import org.springframework.data.domain.Pageable;

public interface DeviceService {

    DeviceResponse createDevice(CreateDeviceRequest request, String createdBy);

    DeviceResponse updateDevice(String id, UpdateDeviceRequest request, String changedBy);

    DeviceResponse getDevice(String id);

    PageResponse<DeviceResponse> listDevices(String organizationId, String deviceType, String status, Pageable pageable);

    void deleteDevice(String id);

    DeviceResponse assignDevice(String id, AssignDeviceRequest request, String changedBy);

    DeviceResponse unassignDevice(String id, String changedBy);
}
