package com.gpl.subsidy.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.subsidy.dto.CreateDeclarationRequest;
import com.gpl.subsidy.dto.DeclarationResponse;
import com.gpl.subsidy.dto.UpdateDeclarationRequest;
import org.springframework.data.domain.Pageable;

public interface DeclarationService {
    DeclarationResponse createDeclaration(CreateDeclarationRequest request, String username);
    DeclarationResponse updateDeclaration(String id, UpdateDeclarationRequest request, String username);
    DeclarationResponse getDeclaration(String id);
    PageResponse<DeclarationResponse> listDeclarations(String declaringOrganizationId, String siteId, String status, String period, Pageable pageable);
    DeclarationResponse submitDeclaration(String id, String username);
    DeclarationResponse reviewDeclaration(String id, String reviewerPersonId, String notes, String username);
    DeclarationResponse approveDeclaration(String id, String approvedByPersonId, String notes, String username);
    DeclarationResponse rejectDeclaration(String id, String rejectedByPersonId, String reason, String username);
    void deleteDeclaration(String id);
}
