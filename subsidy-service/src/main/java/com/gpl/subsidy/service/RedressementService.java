package com.gpl.subsidy.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.subsidy.dto.CreateRedressementRequest;
import com.gpl.subsidy.dto.RedressementResponse;
import org.springframework.data.domain.Pageable;

public interface RedressementService {
    RedressementResponse createRedressement(CreateRedressementRequest request, String username);
    RedressementResponse getRedressement(String id);
    PageResponse<RedressementResponse> listRedressements(String reconciliationId, String status, Pageable pageable);
    RedressementResponse markAsPaid(String id, String transactionRef, String username);
    RedressementResponse cancelRedressement(String id, String username);
    void deleteRedressement(String id);
}
