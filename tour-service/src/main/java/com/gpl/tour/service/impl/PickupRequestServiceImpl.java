package com.gpl.tour.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.tour.dto.CreatePickupRequestDto;
import com.gpl.tour.dto.PickupRequestResponseDto;
import com.gpl.tour.dto.UpdatePickupRequestDto;
import com.gpl.tour.model.PickupRequest;
import com.gpl.tour.repository.PickupRequestRepository;
import com.gpl.tour.service.PickupRequestService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PickupRequestServiceImpl implements PickupRequestService {

    private final PickupRequestRepository repository;

    @Override
    @Transactional
    public PickupRequestResponseDto createPickupRequest(CreatePickupRequestDto dto, String createdBy) {
        PickupRequest pr = new PickupRequest();
        pr.setMarketerOrganizationId(dto.getMarketerOrganizationId());
        pr.setSourceSiteId(dto.getSourceSiteId());
        pr.setDestinationSiteId(dto.getDestinationSiteId());
        pr.setRequestedQuantity(dto.getRequestedQuantity());
        pr.updateStatus("SUBMITTED", "Demande d'enlèvement vrac soumise");
        pr.setCreatedBy(createdBy);

        PickupRequest saved = repository.save(pr);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public PickupRequestResponseDto updatePickupRequest(String id, UpdatePickupRequestDto dto, String updatedBy) {
        PickupRequest pr = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PICKUP_NOT_FOUND", "PickupRequest not found with ID: " + id));

        if (dto.getApprovedQuantity() != null) {
            pr.setApprovedQuantity(dto.getApprovedQuantity());
        }
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            pr.updateStatus(dto.getStatus(), dto.getStatusDescription() != null ? dto.getStatusDescription() : dto.getStatus());
        }
        pr.setChangeby(updatedBy);

        PickupRequest saved = repository.save(pr);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PickupRequestResponseDto getPickupRequest(String id) {
        PickupRequest pr = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PICKUP_NOT_FOUND", "PickupRequest not found with ID: " + id));
        return mapToDto(pr);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PickupRequestResponseDto> listPickupRequests(String marketerOrganizationId, String sourceSiteId, String status, Pageable pageable) {
        Specification<PickupRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (marketerOrganizationId != null && !marketerOrganizationId.isBlank()) {
                predicates.add(cb.equal(root.get("marketerOrganizationId"), marketerOrganizationId));
            }
            if (sourceSiteId != null && !sourceSiteId.isBlank()) {
                predicates.add(cb.equal(root.get("sourceSiteId"), sourceSiteId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PickupRequest> page = repository.findAll(spec, pageable);
        List<PickupRequestResponseDto> content = page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());
        return PageResponse.of(content, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    @Transactional
    public PickupRequestResponseDto approvePickupRequest(String id, double approvedQuantity, String approvedBy) {
        PickupRequest pr = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PICKUP_NOT_FOUND", "PickupRequest not found with ID: " + id));
        pr.setApprovedQuantity(approvedQuantity);
        pr.updateStatus("APPROVED", "Demande d'enlèvement vrac approuvée pour " + approvedQuantity + " kg/tonnes");
        pr.setChangeby(approvedBy);
        return mapToDto(repository.save(pr));
    }

    @Override
    @Transactional
    public PickupRequestResponseDto rejectPickupRequest(String id, String rejectedBy) {
        PickupRequest pr = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PICKUP_NOT_FOUND", "PickupRequest not found with ID: " + id));
        pr.updateStatus("REJECTED", "Demande d'enlèvement vrac rejetée");
        pr.setChangeby(rejectedBy);
        return mapToDto(repository.save(pr));
    }

    @Override
    @Transactional
    public void deletePickupRequest(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("PICKUP_NOT_FOUND", "PickupRequest not found with ID: " + id);
        }
        repository.deleteById(id);
    }

    private PickupRequestResponseDto mapToDto(PickupRequest pr) {
        return PickupRequestResponseDto.builder()
                .id(pr.getId())
                .marketerOrganizationId(pr.getMarketerOrganizationId())
                .sourceSiteId(pr.getSourceSiteId())
                .destinationSiteId(pr.getDestinationSiteId())
                .requestedQuantity(pr.getRequestedQuantity())
                .approvedQuantity(pr.getApprovedQuantity())
                .status(pr.getStatus())
                .statusDescription(pr.getStatusDescription())
                .statusDate(pr.getStatusDate())
                .createdBy(pr.getCreatedBy())
                .createdAt(pr.getCreatedAt())
                .changedate(pr.getChangedate())
                .build();
    }
}
