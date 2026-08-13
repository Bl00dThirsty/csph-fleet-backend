package com.gpl.cylinder.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.cylinder.dto.CreateRfidTagRequest;
import com.gpl.cylinder.dto.UpdateRfidTagRequest;
import com.gpl.cylinder.dto.RfidTagResponse;
import org.springframework.data.domain.Pageable;

public interface RfidTagService {
    RfidTagResponse createRfidTag(CreateRfidTagRequest request, String createdBy);
    RfidTagResponse updateRfidTag(String id, UpdateRfidTagRequest request, String changedBy);
    RfidTagResponse getRfidTag(String id);
    RfidTagResponse getByTagUid(String tagUid);
    PageResponse<RfidTagResponse> listRfidTags(
            String tagUid,
            String bottleSerial,
            String currentSiteId,
            String status,
            Pageable pageable);
    void deleteRfidTag(String id);
}
