package com.gpl.cylinder.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.cylinder.dto.CreateScanEventRequest;
import com.gpl.cylinder.dto.ScanEventResponse;
import org.springframework.data.domain.Pageable;

public interface ScanEventService {
    ScanEventResponse createScanEvent(CreateScanEventRequest request, String createdBy);
    ScanEventResponse getScanEvent(String id);
    PageResponse<ScanEventResponse> listScanEvents(
            String checkpointId,
            String livreurPersonId,
            String rfidTagId,
            String direction,
            Pageable pageable);
    ScanEventResponse resolveConflict(String id, com.gpl.cylinder.dto.ResolveScanConflictRequest request, String resolvedBy);
    void deleteScanEvent(String id);
}
