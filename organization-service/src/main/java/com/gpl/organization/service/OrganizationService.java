package com.gpl.organization.service;

import com.gpl.common.exception.DuplicateResourceException;
import com.gpl.common.dto.PageResponse;
import com.gpl.organization.dto.*;
import com.gpl.organization.model.Organization;
import com.gpl.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request, String createdBy) {
        if (organizationRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Organization with code " + request.getCode() + " already exists");
        }

        Organization org = Organization.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .tier(request.getTier())
                .classStructureId(request.getClassStructureId())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .website(request.getWebsite())
                .isHeadquarters(request.isHeadquarters())
                .build();
        
        org.setCreatedBy(createdBy);

        if (request.getParentOrganizationId() != null) {
            Organization parent = organizationRepository.findById(request.getParentOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Parent organization not found"));
            
            org.setParentOrganizationId(parent.getId());
            org.setHierarchyLevel(parent.getHierarchyLevel() + 1);
            org.setHierarchyPath(parent.getHierarchyPath() + "/" + org.getCode());
            
            parent.setHasChildren(true);
            organizationRepository.save(parent);
        } else {
            org.setHierarchyLevel(0);
            org.setHierarchyPath(org.getCode());
        }

        Organization savedOrg = organizationRepository.save(org);
        return buildOrganizationResponse(savedOrg);
    }

    @Transactional
    public OrganizationResponse updateOrganization(String id, UpdateOrganizationRequest request, String changedBy) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        if (request.getName() != null) org.setName(request.getName());
        if (request.getDescription() != null) org.setDescription(request.getDescription());
        if (request.getContactEmail() != null) org.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) org.setContactPhone(request.getContactPhone());
        if (request.getWebsite() != null) org.setWebsite(request.getWebsite());
        if (request.getLogoUrl() != null) org.setLogoUrl(request.getLogoUrl());
        if (request.getTaxId() != null) org.setTaxId(request.getTaxId());
        if (request.getRegistrationNumber() != null) org.setRegistrationNumber(request.getRegistrationNumber());
        if (request.getCurrency() != null) org.setCurrency(request.getCurrency());
        if (request.getLanguage() != null) org.setLanguage(request.getLanguage());
        if (request.getTimezone() != null) org.setTimezone(request.getTimezone());
        if (request.getIsHeadquarters() != null) org.setHeadquarters(request.getIsHeadquarters());

        org.setChangeby(changedBy);

        Organization updatedOrg = organizationRepository.save(org);
        return buildOrganizationResponse(updatedOrg);
    }

    public OrganizationResponse getOrganization(String id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        return buildOrganizationResponse(org);
    }

    public PageResponse<OrganizationSummaryResponse> listOrganizations(String type, String tier, Boolean isActive, Pageable pageable) {
        Page<Organization> page;
        page = organizationRepository.findAll(pageable); // simplified for this example
        
        List<OrganizationSummaryResponse> list = page.getContent().stream()
                .map(this::buildOrganizationSummaryResponse)
                .collect(Collectors.toList());
                
        PageResponse<OrganizationSummaryResponse> res = new PageResponse<>();
        res.setContent(list);
        res.setTotalElements(page.getTotalElements());
        res.setTotalPages(page.getTotalPages());
        return res;
    }

    public PageResponse<OrganizationSummaryResponse> searchOrganizations(String query, Pageable pageable) {
        Page<Organization> page = organizationRepository.findByNameContainingIgnoreCase(query, pageable);
        List<OrganizationSummaryResponse> list = page.getContent().stream()
                .map(this::buildOrganizationSummaryResponse)
                .collect(Collectors.toList());
                
        PageResponse<OrganizationSummaryResponse> res = new PageResponse<>();
        res.setContent(list);
        res.setTotalElements(page.getTotalElements());
        res.setTotalPages(page.getTotalPages());
        return res;
    }

    public List<OrganizationSummaryResponse> getChildren(String parentId) {
        return organizationRepository.findByParentOrganizationId(parentId).stream()
                .map(this::buildOrganizationSummaryResponse)
                .collect(Collectors.toList());
    }

    public Object getHierarchy(String orgId) {
        // returning simplified for now
        Organization org = organizationRepository.findById(orgId).orElseThrow(() -> new RuntimeException("Not found"));
        return organizationRepository.findByHierarchyPathStartingWith(org.getHierarchyPath());
    }

    @Transactional
    public Organization updateStatus(String id, UpdateStatusRequest request, String changedBy) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        org.setStatus(request.getNewStatus());
        org.setChangeby(changedBy);
        
        return organizationRepository.save(org);
    }

    @Transactional
    public void deleteOrganization(String id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        org.setStatus("ARCHIVED");
        organizationRepository.save(org);
    }

    private OrganizationResponse buildOrganizationResponse(Organization org) {
        OrganizationResponse res = new OrganizationResponse();
        res.setId(org.getId());
        res.setCode(org.getCode());
        res.setName(org.getName());
        res.setDescription(org.getDescription());
        res.setType(org.getType());
        res.setTypeDescription(org.getTypeDescription());
        res.setTier(org.getTier());
        res.setTierDescription(org.getTierDescription());
        res.setClassStructureId(org.getClassStructureId());
        res.setHierarchyPath(org.getHierarchyPath());
        res.setParentOrganizationId(org.getParentOrganizationId());
        res.setHierarchyLevel(org.getHierarchyLevel());
        res.setLogoUrl(org.getLogoUrl());
        res.setTaxId(org.getTaxId());
        res.setRegistrationNumber(org.getRegistrationNumber());
        res.setContactEmail(org.getContactEmail());
        res.setContactPhone(org.getContactPhone());
        res.setWebsite(org.getWebsite());
        res.setCurrency(org.getCurrency());
        res.setLanguage(org.getLanguage());
        res.setTimezone(org.getTimezone());
        res.setActive(org.isActive());
        res.setLocked(org.isLocked());
        res.setHeadquarters(org.isHeadquarters());
        res.setHasChildren(org.isHasChildren());
        res.setSystemOrg(org.isSystemOrg());
        res.setStatus(org.getStatus());
        res.setModificationsRef("/api/v1/organizations/" + org.getId() + "/modifications");
        return res;
    }

    private OrganizationSummaryResponse buildOrganizationSummaryResponse(Organization org) {
        OrganizationSummaryResponse res = new OrganizationSummaryResponse();
        res.setId(org.getId());
        res.setCode(org.getCode());
        res.setName(org.getName());
        res.setType(org.getType());
        res.setTypeDescription(org.getTypeDescription());
        res.setTier(org.getTier());
        res.setTierDescription(org.getTierDescription());
        res.setStatus(org.getStatus());
        res.setActive(org.isActive());
        res.setHierarchyPath(org.getHierarchyPath());
        res.setHierarchyLevel(org.getHierarchyLevel());
        res.setHasChildren(org.isHasChildren());
        return res;
    }
}
