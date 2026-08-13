package com.gpl.subsidy.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.subsidy.dto.CreateReconciliationRequest;
import com.gpl.subsidy.dto.ReconciliationResponse;
import org.springframework.data.domain.Pageable;

public interface ReconciliationService {
    ReconciliationResponse createReconciliation(CreateReconciliationRequest request, String username);
    ReconciliationResponse getReconciliation(String id);
    ReconciliationResponse getReconciliationByDeclarationId(String declarationId);
    PageResponse<ReconciliationResponse> listReconciliations(String declarationId, String status, Pageable pageable);
    ReconciliationResponse verifyReconciliation(String id, String verifiedByPersonId, String notes, String username);
    void deleteReconciliation(String id);
}
