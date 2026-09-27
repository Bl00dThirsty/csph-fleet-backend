package com.gpl.organization.service;

import com.gpl.common.exception.DuplicateResourceException;
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
            throw new DuplicateResourceException("Site code already exists");
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
