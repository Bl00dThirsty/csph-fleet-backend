import os

base_dir = r"c:\Users\User\Downloads\gpl-rfid-livraisons\backend\organization-service\src\main\java\com\gpl\organization"

def write_file(sub_dir, name, content):
    d = os.path.join(base_dir, sub_dir)
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, name), "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

write_file("service", "OrganizationService.java", """
package com.gpl.organization.service;

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
            throw new RuntimeException("Organization with code " + request.getCode() + " already exists");
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
            org.setHierarchyPath(parent.getHierarchyPath() + "\\\\" + org.getCode());
            
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
""")

write_file("service", "SiteService.java", """
package com.gpl.organization.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.organization.dto.*;
import com.gpl.organization.model.Organization;
import com.gpl.organization.model.Site;
import com.gpl.organization.repository.OrganizationRepository;
import com.gpl.organization.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public SiteResponse createSite(CreateSiteRequest request, String createdBy) {
        if (siteRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Site code already exists");
        }

        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        Site site = Site.builder()
                .code(request.getCode())
                .siteId(request.getSiteId())
                .name(request.getName())
                .description(request.getDescription())
                .organizationId(org.getId())
                .orgId(org.getCode())
                .type(request.getType())
                .addressLine1(request.getAddressLine1())
                .city(request.getCity())
                .region(request.getRegion())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .geofenceRadiusMeters(request.getGeofenceRadiusMeters() != null ? request.getGeofenceRadiusMeters() : 200)
                .storageCapacityTons(request.getStorageCapacityTons())
                .maxVehicleBays(request.getMaxVehicleBays())
                .build();
        
        site.setCreatedBy(createdBy);
        return buildSiteResponse(siteRepository.save(site));
    }

    @Transactional
    public SiteResponse updateSite(String id, UpdateSiteRequest request, String changedBy) {
        Site site = siteRepository.findById(id).orElseThrow(() -> new RuntimeException("Site not found"));
        
        if (request.getName() != null) site.setName(request.getName());
        if (request.getDescription() != null) site.setDescription(request.getDescription());
        if (request.getAddressLine1() != null) site.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) site.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) site.setCity(request.getCity());
        if (request.getRegion() != null) site.setRegion(request.getRegion());
        if (request.getPostalCode() != null) site.setPostalCode(request.getPostalCode());
        if (request.getLatitude() != null) site.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) site.setLongitude(request.getLongitude());
        if (request.getGeofenceRadiusMeters() != null) site.setGeofenceRadiusMeters(request.getGeofenceRadiusMeters());
        if (request.getOperatingHoursStart() != null) site.setOperatingHoursStart(request.getOperatingHoursStart());
        if (request.getOperatingHoursEnd() != null) site.setOperatingHoursEnd(request.getOperatingHoursEnd());
        
        site.setChangeby(changedBy);
        return buildSiteResponse(siteRepository.save(site));
    }

    public SiteResponse getSite(String id) {
        return buildSiteResponse(siteRepository.findById(id).orElseThrow(() -> new RuntimeException("Site not found")));
    }

    public PageResponse<SiteSummaryResponse> listSites(String organizationId, String type, String city, Boolean isOperational, Pageable pageable) {
        Page<Site> page = siteRepository.findAll(pageable); // Simplified
        List<SiteSummaryResponse> content = page.getContent().stream().map(this::buildSiteSummaryResponse).collect(Collectors.toList());
        PageResponse<SiteSummaryResponse> res = new PageResponse<>();
        res.setContent(content);
        res.setTotalElements(page.getTotalElements());
        res.setTotalPages(page.getTotalPages());
        return res;
    }

    public List<SiteSummaryResponse> findNearbySites(double lat, double lon, double radiusKm) {
        return siteRepository.findNearby(lat, lon, radiusKm).stream().map(this::buildSiteSummaryResponse).collect(Collectors.toList());
    }

    @Transactional
    public Site updateSiteStatus(String id, UpdateStatusRequest request, String changedBy) {
        Site site = siteRepository.findById(id).orElseThrow(() -> new RuntimeException("Site not found"));
        site.setStatus(request.getNewStatus());
        site.setChangeby(changedBy);
        return siteRepository.save(site);
    }

    private SiteResponse buildSiteResponse(Site site) {
        SiteResponse res = new SiteResponse();
        res.setId(site.getId());
        res.setCode(site.getCode());
        res.setSiteId(site.getSiteId());
        res.setName(site.getName());
        res.setDescription(site.getDescription());
        res.setOrganizationId(site.getOrganizationId());
        res.setOrgId(site.getOrgId());
        res.setType(site.getType());
        res.setTypeDescription(site.getTypeDescription());
        res.setClassStructureId(site.getClassStructureId());
        res.setAddressLine1(site.getAddressLine1());
        res.setAddressLine2(site.getAddressLine2());
        res.setCity(site.getCity());
        res.setRegion(site.getRegion());
        res.setCountry(site.getCountry());
        res.setPostalCode(site.getPostalCode());
        res.setLatitude(site.getLatitude());
        res.setLongitude(site.getLongitude());
        res.setGeofenceRadiusMeters(site.getGeofenceRadiusMeters());
        res.setDefault(site.isDefault());
        res.setActive(site.isActive());
        res.setLocked(site.isLocked());
        res.setDisabled(site.isDisabled());
        res.setOperational(site.isOperational());
        res.setHasStorageCapacity(site.isHasStorageCapacity());
        res.setRepairFacility(site.isRepairFacility());
        res.setStorageCapacityTons(site.getStorageCapacityTons());
        res.setMaxVehicleBays(site.getMaxVehicleBays());
        res.setOperatingHoursStart(site.getOperatingHoursStart());
        res.setOperatingHoursEnd(site.getOperatingHoursEnd());
        res.setStatus(site.getStatus());
        res.setModificationsRef("/api/v1/sites/" + site.getId() + "/modifications");
        return res;
    }

    private SiteSummaryResponse buildSiteSummaryResponse(Site site) {
        SiteSummaryResponse res = new SiteSummaryResponse();
        res.setId(site.getId());
        res.setCode(site.getCode());
        res.setSiteId(site.getSiteId());
        res.setName(site.getName());
        res.setType(site.getType());
        res.setTypeDescription(site.getTypeDescription());
        res.setOrgId(site.getOrgId());
        res.setCity(site.getCity());
        res.setStatus(site.getStatus());
        res.setOperational(site.isOperational());
        res.setLatitude(site.getLatitude());
        res.setLongitude(site.getLongitude());
        return res;
    }
}
""")

write_file("service", "ClassStructureService.java", """
package com.gpl.organization.service;

import com.gpl.organization.dto.ClassStructureResponse;
import com.gpl.organization.model.ClassStructure;
import com.gpl.organization.repository.ClassStructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassStructureService {
    private final ClassStructureRepository classStructureRepository;

    public List<ClassStructureResponse> getAll() {
        return classStructureRepository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    private ClassStructureResponse map(ClassStructure c) {
        ClassStructureResponse r = new ClassStructureResponse();
        r.setId(c.getId());
        r.setClassificationId(c.getClassificationId());
        r.setDescription(c.getDescription());
        r.setHierarchyPath(c.getHierarchyPath());
        r.setParentClassStructureId(c.getParentClassStructureId());
        r.setObjectName(c.getObjectName());
        r.setSortOrder(c.getSortOrder());
        r.setShow(c.isShow());
        r.setUseClassInDesc(c.isUseClassInDesc());
        r.setTopLevel(c.isTopLevel());
        r.setOrgId(c.getOrgId());
        r.setSiteId(c.getSiteId());
        return r;
    }
}
""")

write_file("service", "OrganizationRelationshipService.java", """
package com.gpl.organization.service;

import com.gpl.organization.dto.CreateRelationshipRequest;
import com.gpl.organization.dto.RelationshipResponse;
import com.gpl.organization.model.Organization;
import com.gpl.organization.model.OrganizationRelationship;
import com.gpl.organization.repository.OrganizationRelationshipRepository;
import com.gpl.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationRelationshipService {
    private final OrganizationRelationshipRepository relationshipRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public RelationshipResponse createRelationship(CreateRelationshipRequest request, String createdBy) {
        Organization source = organizationRepository.findById(request.getSourceOrganizationId())
                .orElseThrow(() -> new RuntimeException("Source org not found"));
        Organization target = organizationRepository.findById(request.getTargetOrganizationId())
                .orElseThrow(() -> new RuntimeException("Target org not found"));

        if (source.getId().equals(target.getId())) {
            throw new RuntimeException("Cannot create self-relationship");
        }

        OrganizationRelationship rel = OrganizationRelationship.builder()
                .sourceOrganizationId(source.getId())
                .targetOrganizationId(target.getId())
                .type(request.getType())
                .contractReference(request.getContractReference())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .isExclusive(request.isExclusive())
                .description(request.getDescription())
                .build();
        
        rel.setCreatedBy(createdBy);
        return buildResponse(relationshipRepository.save(rel), source.getName(), target.getName());
    }

    private RelationshipResponse buildResponse(OrganizationRelationship rel, String srcName, String tgtName) {
        RelationshipResponse res = new RelationshipResponse();
        res.setId(rel.getId());
        res.setSourceOrganizationId(rel.getSourceOrganizationId());
        res.setTargetOrganizationId(rel.getTargetOrganizationId());
        res.setSourceOrganizationName(srcName);
        res.setTargetOrganizationName(tgtName);
        res.setType(rel.getType());
        res.setTypeDescription(rel.getTypeDescription());
        res.setContractReference(rel.getContractReference());
        res.setValidFrom(rel.getValidFrom());
        res.setValidUntil(rel.getValidUntil());
        res.setActive(rel.isActive());
        res.setExclusive(rel.isExclusive());
        res.setDescription(rel.getDescription());
        res.setStatus(rel.getStatus());
        return res;
    }
}
""")

print("Services generated!")
