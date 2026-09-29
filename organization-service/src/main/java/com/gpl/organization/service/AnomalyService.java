package com.gpl.organization.service;

import com.gpl.common.dto.PageResponse;
import com.gpl.organization.dto.AnomalyResponse;
import com.gpl.organization.dto.AnomalySummaryResponse;
import com.gpl.organization.model.Anomaly;
import com.gpl.organization.repository.AnomalyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnomalyService {

    private final AnomalyRepository anomalyRepository;

    public PageResponse<AnomalySummaryResponse> listAnomalies(String status, String severity, String category, String siteId, Pageable pageable) {
        Anomaly exampleAnomaly = new Anomaly();
        if (status != null && !status.isEmpty()) exampleAnomaly.setStatus(status);
        if (severity != null && !severity.isEmpty()) exampleAnomaly.setSeverity(severity);
        if (category != null && !category.isEmpty()) exampleAnomaly.setCategory(category);
        if (siteId != null && !siteId.isEmpty()) exampleAnomaly.setSiteId(UUID.fromString(siteId));

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.EXACT);
        
        Example<Anomaly> example = Example.of(exampleAnomaly, matcher);

        Page<Anomaly> page = anomalyRepository.findAll(example, pageable);

        List<AnomalySummaryResponse> content = page.getContent().stream()
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());

        return PageResponse.<AnomalySummaryResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public AnomalyResponse getAnomaly(String id) {
        Anomaly anomaly = anomalyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Anomaly not found with id: " + id));
        return mapToResponse(anomaly);
    }

    private AnomalySummaryResponse mapToSummaryResponse(Anomaly anomaly) {
        return AnomalySummaryResponse.builder()
                .id(anomaly.getId())
                .type(anomaly.getType())
                .category(anomaly.getCategory())
                .severity(anomaly.getSeverity())
                .status(anomaly.getStatus())
                .entityType(anomaly.getEntityType())
                .entityId(anomaly.getEntityId())
                .siteId(anomaly.getSiteId())
                .clientSiteId(anomaly.getClientSiteId())
                .assignedToGroup(anomaly.getAssignedToGroup())
                .createdAt(anomaly.getCreatedAt())
                .resolvedAt(anomaly.getResolvedAt())
                .build();
    }

    private AnomalyResponse mapToResponse(Anomaly anomaly) {
        return AnomalyResponse.builder()
                .id(anomaly.getId())
                .type(anomaly.getType())
                .category(anomaly.getCategory())
                .severity(anomaly.getSeverity())
                .status(anomaly.getStatus())
                .entityType(anomaly.getEntityType())
                .entityId(anomaly.getEntityId())
                .siteId(anomaly.getSiteId())
                .clientSiteId(anomaly.getClientSiteId())
                .evidenceJson(anomaly.getEvidenceJson())
                .assignedToGroup(anomaly.getAssignedToGroup())
                .createdAt(anomaly.getCreatedAt())
                .updatedAt(anomaly.getUpdatedAt())
                .resolvedAt(anomaly.getResolvedAt())
                .resolvedBy(anomaly.getResolvedBy())
                .resolutionNotes(anomaly.getResolutionNotes())
                .createdBy(anomaly.getCreatedBy())
                .updatedBy(anomaly.getUpdatedBy())
                .build();
    }
}
