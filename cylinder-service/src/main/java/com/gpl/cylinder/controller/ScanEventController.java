package com.gpl.cylinder.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.cylinder.dto.CreateScanEventRequest;
import com.gpl.cylinder.dto.ScanEventResponse;
import com.gpl.cylinder.service.ScanEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
public class ScanEventController {

    private final ScanEventService scanEventService;

    @RequiresPermission("SCAN_VIEW")
    @GetMapping
    public PageResponse<ScanEventResponse> listScanEvents(
            @RequestParam(required = false) String checkpointId,
            @RequestParam(required = false) String driverPersonId,
            @RequestParam(required = false) String rfidTagId,
            @RequestParam(required = false) String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return scanEventService.listScanEvents(checkpointId, driverPersonId, rfidTagId, direction, pageable);
    }

    @RequiresPermission("SCAN_VIEW")
    @GetMapping("/{id}")
    public ApiResponse<ScanEventResponse> getScanEvent(@PathVariable String id) {
        return ApiResponse.success(scanEventService.getScanEvent(id));
    }

    @RequiresPermission("SCAN_CREATE")
    @PostMapping
    public ApiResponse<ScanEventResponse> createScanEvent(
            @Valid @RequestBody CreateScanEventRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String userId) {
        return ApiResponse.success(scanEventService.createScanEvent(request, userId));
    }

    @RequiresPermission("SCAN_RESOLVE_CONFLICT")
    @PostMapping("/{id}/resolve-conflict")
    public ApiResponse<ScanEventResponse> resolveConflict(
            @PathVariable String id,
            @Valid @RequestBody com.gpl.cylinder.dto.ResolveScanConflictRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String userId) {
        return ApiResponse.success(scanEventService.resolveConflict(id, request, userId));
    }

    @RequiresPermission("SCAN_DELETE")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteScanEvent(@PathVariable String id) {
        scanEventService.deleteScanEvent(id);
        return ApiResponse.success(null);
    }
}
