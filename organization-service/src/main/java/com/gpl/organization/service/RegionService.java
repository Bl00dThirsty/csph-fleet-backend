package com.gpl.organization.service;

import com.gpl.organization.dto.RegionResponse;
import com.gpl.organization.model.Region;
import com.gpl.organization.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;

    public List<RegionResponse> listRegions() {
        return regionRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RegionResponse getRegion(String id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Region not found with id: " + id));
        return mapToResponse(region);
    }

    public RegionResponse getRegionByCode(String code) {
        Region region = regionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Region not found with code: " + code));
        return mapToResponse(region);
    }

    private RegionResponse mapToResponse(Region region) {
        return RegionResponse.builder()
                .id(region.getId())
                .name(region.getName())
                .code(region.getCode())
                .createdAt(region.getCreatedAt())
                .updatedAt(region.getUpdatedAt())
                .build();
    }
}
