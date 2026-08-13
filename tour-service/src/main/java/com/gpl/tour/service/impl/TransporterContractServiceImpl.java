package com.gpl.tour.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.tour.dto.CreateTransporterContractDto;
import com.gpl.tour.dto.TransporterContractResponseDto;
import com.gpl.tour.dto.UpdateTransporterContractDto;
import com.gpl.tour.model.TransporterContract;
import com.gpl.tour.repository.TransporterContractRepository;
import com.gpl.tour.service.TransporterContractService;
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
public class TransporterContractServiceImpl implements TransporterContractService {

    private final TransporterContractRepository repository;

    @Override
    @Transactional
    public TransporterContractResponseDto createContract(CreateTransporterContractDto dto, String createdBy) {
        TransporterContract tc = new TransporterContract();
        tc.setMarketerOrganizationId(dto.getMarketerOrganizationId());
        tc.setTransporterOrganizationId(dto.getTransporterOrganizationId());
        tc.setContractReference(dto.getContractReference());
        tc.setPrimary(dto.isPrimary());
        tc.setStartedAt(dto.getStartedAt());
        tc.setEndedAt(dto.getEndedAt());
        tc.setActive(true);
        tc.setCreatedBy(createdBy);

        TransporterContract saved = repository.save(tc);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public TransporterContractResponseDto updateContract(String id, UpdateTransporterContractDto dto, String updatedBy) {
        TransporterContract tc = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CONTRACT_NOT_FOUND", "TransporterContract not found with ID: " + id));

        if (dto.getContractReference() != null) {
            tc.setContractReference(dto.getContractReference());
        }
        if (dto.getIsPrimary() != null) {
            tc.setPrimary(dto.getIsPrimary());
        }
        if (dto.getStartedAt() != null) {
            tc.setStartedAt(dto.getStartedAt());
        }
        if (dto.getEndedAt() != null) {
            tc.setEndedAt(dto.getEndedAt());
        }
        if (dto.getIsActive() != null) {
            tc.setActive(dto.getIsActive());
        }
        tc.setChangeby(updatedBy);

        TransporterContract saved = repository.save(tc);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TransporterContractResponseDto getContract(String id) {
        TransporterContract tc = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CONTRACT_NOT_FOUND", "TransporterContract not found with ID: " + id));
        return mapToDto(tc);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransporterContractResponseDto> listContracts(String marketerOrganizationId, String transporterOrganizationId, Pageable pageable) {
        Specification<TransporterContract> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (marketerOrganizationId != null && !marketerOrganizationId.isBlank()) {
                predicates.add(cb.equal(root.get("marketerOrganizationId"), marketerOrganizationId));
            }
            if (transporterOrganizationId != null && !transporterOrganizationId.isBlank()) {
                predicates.add(cb.equal(root.get("transporterOrganizationId"), transporterOrganizationId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TransporterContract> page = repository.findAll(spec, pageable);
        List<TransporterContractResponseDto> content = page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());
        return PageResponse.of(content, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransporterContractResponseDto> getContractsByMarketer(String marketerOrganizationId) {
        return repository.findByMarketerOrganizationId(marketerOrganizationId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteContract(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("CONTRACT_NOT_FOUND", "TransporterContract not found with ID: " + id);
        }
        repository.deleteById(id);
    }

    private TransporterContractResponseDto mapToDto(TransporterContract tc) {
        return TransporterContractResponseDto.builder()
                .id(tc.getId())
                .marketerOrganizationId(tc.getMarketerOrganizationId())
                .transporterOrganizationId(tc.getTransporterOrganizationId())
                .contractReference(tc.getContractReference())
                .isPrimary(tc.isPrimary())
                .startedAt(tc.getStartedAt())
                .endedAt(tc.getEndedAt())
                .isActive(tc.isActive())
                .createdBy(tc.getCreatedBy())
                .createdAt(tc.getCreatedAt())
                .changedate(tc.getChangedate())
                .build();
    }
}
