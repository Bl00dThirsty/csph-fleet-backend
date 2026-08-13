package com.gpl.organization.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.DuplicateResourceException;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.organization.dto.ClientSiteResponse;
import com.gpl.organization.dto.CreateClientSiteRequest;
import com.gpl.organization.dto.UpdateClientSiteRequest;
import com.gpl.organization.model.ClientSite;
import com.gpl.organization.model.Organization;
import com.gpl.organization.model.Site;
import com.gpl.organization.repository.ClientSiteRepository;
import com.gpl.organization.repository.OrganizationRepository;
import com.gpl.organization.repository.SiteRepository;
import com.gpl.organization.service.ClientSiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientSiteServiceImpl implements ClientSiteService {

    private final ClientSiteRepository clientSiteRepository;
    private final SiteRepository siteRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public ClientSiteResponse createClientSite(CreateClientSiteRequest request, String createdBy) {
        if (clientSiteRepository.existsBySiteId(request.getSiteId())) {
            throw new DuplicateResourceException("ClientSite already exists for siteId: " + request.getSiteId());
        }

        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new ResourceNotFoundException("SITE_NOT_FOUND", "Site not found with ID: " + request.getSiteId()));

        Organization clientOrg = organizationRepository.findById(request.getClientOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("ORG_NOT_FOUND", "Client Organization not found with ID: " + request.getClientOrganizationId()));

        ClientSite clientSite = ClientSite.builder()
                .site(site)
                .clientOrganization(clientOrg)
                .siteContactPersonId(request.getSiteContactPersonId())
                .deliveryInstructions(request.getDeliveryInstructions())
                .specificRequirements(request.getSpecificRequirements())
                .requiresAuthorization(request.isRequiresAuthorization())
                .operatingConstraints(request.getOperatingConstraints())
                .build();
        clientSite.setCreatedBy(createdBy);

        ClientSite saved = clientSiteRepository.save(clientSite);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ClientSiteResponse updateClientSite(String id, UpdateClientSiteRequest request, String updatedBy) {
        ClientSite clientSite = clientSiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_SITE_NOT_FOUND", "ClientSite not found with ID: " + id));

        if (request.getSiteContactPersonId() != null) {
            clientSite.setSiteContactPersonId(request.getSiteContactPersonId());
        }
        if (request.getDeliveryInstructions() != null) {
            clientSite.setDeliveryInstructions(request.getDeliveryInstructions());
        }
        if (request.getSpecificRequirements() != null) {
            clientSite.setSpecificRequirements(request.getSpecificRequirements());
        }
        if (request.getRequiresAuthorization() != null) {
            clientSite.setRequiresAuthorization(request.getRequiresAuthorization());
        }
        if (request.getOperatingConstraints() != null) {
            clientSite.setOperatingConstraints(request.getOperatingConstraints());
        }
        clientSite.setChangeby(updatedBy);

        ClientSite updated = clientSiteRepository.save(clientSite);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientSiteResponse getClientSite(String id) {
        ClientSite clientSite = clientSiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_SITE_NOT_FOUND", "ClientSite not found with ID: " + id));
        return mapToResponse(clientSite);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientSiteResponse getClientSiteBySiteId(String siteId) {
        ClientSite clientSite = clientSiteRepository.findBySiteId(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_SITE_NOT_FOUND", "ClientSite not found for siteId: " + siteId));
        return mapToResponse(clientSite);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientSiteResponse> listClientSites(String clientOrganizationId, Pageable pageable) {
        Page<ClientSite> page;
        if (clientOrganizationId != null && !clientOrganizationId.isEmpty()) {
            page = clientSiteRepository.findByClientOrganizationId(clientOrganizationId, pageable);
        } else {
            page = clientSiteRepository.findAll(pageable);
        }

        List<ClientSiteResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(content, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    @Transactional
    public void deleteClientSite(String id) {
        if (!clientSiteRepository.existsById(id)) {
            throw new ResourceNotFoundException("CLIENT_SITE_NOT_FOUND", "ClientSite not found with ID: " + id);
        }
        clientSiteRepository.deleteById(id);
    }

    private ClientSiteResponse mapToResponse(ClientSite cs) {
        return ClientSiteResponse.builder()
                .id(cs.getId())
                .siteId(cs.getSite() != null ? cs.getSite().getId() : null)
                .siteCode(cs.getSite() != null ? cs.getSite().getCode() : null)
                .siteName(cs.getSite() != null ? cs.getSite().getName() : null)
                .clientOrganizationId(cs.getClientOrganization() != null ? cs.getClientOrganization().getId() : null)
                .clientOrganizationName(cs.getClientOrganization() != null ? cs.getClientOrganization().getName() : null)
                .siteContactPersonId(cs.getSiteContactPersonId())
                .deliveryInstructions(cs.getDeliveryInstructions())
                .specificRequirements(cs.getSpecificRequirements())
                .requiresAuthorization(cs.isRequiresAuthorization())
                .operatingConstraints(cs.getOperatingConstraints())
                .createdAt(cs.getCreatedAt())
                .updatedAt(cs.getChangedate())
                .build();
    }
}
