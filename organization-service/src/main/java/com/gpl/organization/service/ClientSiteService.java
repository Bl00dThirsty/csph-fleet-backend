package com.gpl.organization.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.organization.dto.ClientSiteResponse;
import com.gpl.organization.dto.CreateClientSiteRequest;
import com.gpl.organization.dto.UpdateClientSiteRequest;
import org.springframework.data.domain.Pageable;

public interface ClientSiteService {
    ClientSiteResponse createClientSite(CreateClientSiteRequest request, String createdBy);
    ClientSiteResponse updateClientSite(String id, UpdateClientSiteRequest request, String updatedBy);
    ClientSiteResponse getClientSite(String id);
    ClientSiteResponse getClientSiteBySiteId(String siteId);
    PageResponse<ClientSiteResponse> listClientSites(String clientOrganizationId, Pageable pageable);
    void deleteClientSite(String id);
}
