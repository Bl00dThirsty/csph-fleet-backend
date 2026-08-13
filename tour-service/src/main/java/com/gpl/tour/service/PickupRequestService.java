package com.gpl.tour.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.tour.dto.CreatePickupRequestDto;
import com.gpl.tour.dto.PickupRequestResponseDto;
import com.gpl.tour.dto.UpdatePickupRequestDto;
import org.springframework.data.domain.Pageable;

public interface PickupRequestService {
    PickupRequestResponseDto createPickupRequest(CreatePickupRequestDto dto, String createdBy);
    PickupRequestResponseDto updatePickupRequest(String id, UpdatePickupRequestDto dto, String updatedBy);
    PickupRequestResponseDto getPickupRequest(String id);
    PageResponse<PickupRequestResponseDto> listPickupRequests(String marketerOrganizationId, String sourceSiteId, String status, Pageable pageable);
    PickupRequestResponseDto approvePickupRequest(String id, double approvedQuantity, String approvedBy);
    PickupRequestResponseDto rejectPickupRequest(String id, String rejectedBy);
    void deletePickupRequest(String id);
}
