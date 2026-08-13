package com.gpl.cylinder.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.security.RequiresPermission;
import com.gpl.cylinder.dto.CreateRfidTagRequest;
import com.gpl.cylinder.dto.UpdateRfidTagRequest;
import com.gpl.cylinder.dto.RfidTagResponse;
import com.gpl.cylinder.service.RfidTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rfid")
@RequiredArgsConstructor
public class RfidTagController {

    private final RfidTagService rfidTagService;

    @RequiresPermission("RFID_VIEW")
    @GetMapping
    public PageResponse<RfidTagResponse> listRfidTags(
            @RequestParam(required = false) String tagUid,
            @RequestParam(required = false) String bottleSerial,
            @RequestParam(required = false) String currentSiteId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return rfidTagService.listRfidTags(tagUid, bottleSerial, currentSiteId, status, pageable);
    }

    @RequiresPermission("RFID_VIEW")
    @GetMapping("/{id}")
    public ApiResponse<RfidTagResponse> getRfidTag(@PathVariable String id) {
        return ApiResponse.success(rfidTagService.getRfidTag(id));
    }

    @RequiresPermission("RFID_VIEW")
    @GetMapping("/tag/{tagUid}")
    public ApiResponse<RfidTagResponse> getByTagUid(@PathVariable String tagUid) {
        return ApiResponse.success(rfidTagService.getByTagUid(tagUid));
    }

    @RequiresPermission("RFID_CREATE")
    @PostMapping
    public ApiResponse<RfidTagResponse> createRfidTag(
            @Valid @RequestBody CreateRfidTagRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String userId) {
        return ApiResponse.success(rfidTagService.createRfidTag(request, userId));
    }

    @RequiresPermission("RFID_UPDATE")
    @PutMapping("/{id}")
    public ApiResponse<RfidTagResponse> updateRfidTag(
            @PathVariable String id,
            @RequestBody UpdateRfidTagRequest request,
            @RequestHeader(value = "X-User-PersonId", required = false) String userId) {
        return ApiResponse.success(rfidTagService.updateRfidTag(id, request, userId));
    }

    @RequiresPermission("RFID_DELETE")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRfidTag(@PathVariable String id) {
        rfidTagService.deleteRfidTag(id);
        return ApiResponse.success(null);
    }
}
