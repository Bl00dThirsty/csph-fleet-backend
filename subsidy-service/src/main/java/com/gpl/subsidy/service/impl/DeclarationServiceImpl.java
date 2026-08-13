package com.gpl.subsidy.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.subsidy.dto.CreateDeclarationRequest;
import com.gpl.subsidy.dto.DeclarationResponse;
import com.gpl.subsidy.dto.UpdateDeclarationRequest;
import com.gpl.subsidy.model.Declaration;
import com.gpl.subsidy.repository.DeclarationRepository;
import com.gpl.subsidy.service.DeclarationService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeclarationServiceImpl implements DeclarationService {

    private final DeclarationRepository declarationRepository;

    @Override
    public DeclarationResponse createDeclaration(CreateDeclarationRequest request, String username) {
        log.info("Creating declaration for org: {}", request.getEffectiveOrganizationId());
        String author = (username != null && !username.isBlank()) ? username : "SYSTEM";
        
        Declaration declaration = Declaration.builder()
                .marketerOrganizationId(request.getEffectiveOrganizationId())
                .declaringOrganizationId(request.getEffectiveOrganizationId())
                .siteId(request.getSiteId())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .declaredVolume(request.getDeclaredVolume())
                .submittedByPersonId(request.getSubmittedByPersonId() != null ? request.getSubmittedByPersonId() : author)
                .build();

        declaration.setStatus("DRAFT");
        declaration.setStatusDescription("Brouillon");
        declaration.setStatusDate(Instant.now());
        declaration.setCreatedBy(author);
        declaration.setChangeby(author);

        Declaration saved = declarationRepository.save(declaration);
        return DeclarationResponse.fromEntity(saved);
    }

    @Override
    public DeclarationResponse updateDeclaration(String id, UpdateDeclarationRequest request, String username) {
        log.info("Updating declaration: {}", id);
        Declaration declaration = declarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Declaration", id));

        if (request.getEffectiveOrganizationId() != null) {
            declaration.setMarketerOrganizationId(request.getEffectiveOrganizationId());
            declaration.setDeclaringOrganizationId(request.getEffectiveOrganizationId());
        }
        if (request.getSiteId() != null) {
            declaration.setSiteId(request.getSiteId());
        }
        if (request.getPeriodStart() != null) {
            declaration.setPeriodStart(request.getPeriodStart());
        }
        if (request.getPeriodEnd() != null) {
            declaration.setPeriodEnd(request.getPeriodEnd());
        }
        if (request.getDeclaredVolume() != null) {
            declaration.setDeclaredVolume(request.getDeclaredVolume());
        }
        if (request.getSubmittedByPersonId() != null) {
            declaration.setSubmittedByPersonId(request.getSubmittedByPersonId());
        }
        if (request.getStatus() != null) {
            declaration.updateStatus(request.getStatus(), request.getStatus());
        }

        String author = (username != null && !username.isBlank()) ? username : "SYSTEM";
        declaration.setChangeby(author);

        Declaration updated = declarationRepository.save(declaration);
        return DeclarationResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public DeclarationResponse getDeclaration(String id) {
        log.info("Retrieving declaration: {}", id);
        Declaration declaration = declarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Declaration", id));
        return DeclarationResponse.fromEntity(declaration);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeclarationResponse> listDeclarations(String declaringOrganizationId, String siteId, String status, String period, Pageable pageable) {
        log.info("Listing declarations with filters - org: {}, site: {}, status: {}, period: {}", declaringOrganizationId, siteId, status, period);

        Specification<Declaration> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (declaringOrganizationId != null && !declaringOrganizationId.isBlank()) {
                predicates.add(cb.or(
                        cb.equal(root.get("declaringOrganizationId"), declaringOrganizationId),
                        cb.equal(root.get("marketerOrganizationId"), declaringOrganizationId)
                ));
            }
            if (siteId != null && !siteId.isBlank()) {
                predicates.add(cb.equal(root.get("siteId"), siteId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (period != null && !period.isBlank()) {
                try {
                    Instant target = Instant.parse(period);
                    predicates.add(cb.and(
                            cb.lessThanOrEqualTo(root.get("periodStart"), target),
                            cb.greaterThanOrEqualTo(root.get("periodEnd"), target)
                    ));
                } catch (Exception ignored) {
                    // Filter by date range if not an instant parseable
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Declaration> page = declarationRepository.findAll(spec, pageable);
        List<DeclarationResponse> content = page.getContent().stream()
                .map(DeclarationResponse::fromEntity)
                .toList();

        return PageResponse.of(content, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    public DeclarationResponse submitDeclaration(String id, String username) {
        log.info("Submitting declaration: {}", id);
        Declaration declaration = declarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Declaration", id));

        declaration.updateStatus("SUBMITTED", "Soumis pour révision");
        String author = (username != null && !username.isBlank()) ? username : "SYSTEM";
        declaration.setChangeby(author);
        if (declaration.getSubmittedByPersonId() == null) {
            declaration.setSubmittedByPersonId(author);
        }

        Declaration saved = declarationRepository.save(declaration);
        return DeclarationResponse.fromEntity(saved);
    }

    @Override
    public DeclarationResponse reviewDeclaration(String id, String reviewerPersonId, String notes, String username) {
        log.info("Reviewing declaration: {}", id);
        Declaration declaration = declarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Declaration", id));

        declaration.updateStatus("UNDER_REVIEW", notes != null && !notes.isBlank() ? "En cours d'examen: " + notes : "En cours d'examen par la commission");
        String author = (username != null && !username.isBlank()) ? username : "SYSTEM";
        declaration.setChangeby(author);

        Declaration saved = declarationRepository.save(declaration);
        return DeclarationResponse.fromEntity(saved);
    }

    @Override
    public DeclarationResponse approveDeclaration(String id, String approvedByPersonId, String notes, String username) {
        log.info("Approving declaration: {}", id);
        Declaration declaration = declarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Declaration", id));

        declaration.updateStatus("APPROVED", notes != null && !notes.isBlank() ? "Approuvé: " + notes : "Déclaration de volume approuvée");
        String author = (username != null && !username.isBlank()) ? username : "SYSTEM";
        declaration.setChangeby(author);

        Declaration saved = declarationRepository.save(declaration);
        return DeclarationResponse.fromEntity(saved);
    }

    @Override
    public DeclarationResponse rejectDeclaration(String id, String rejectedByPersonId, String reason, String username) {
        log.info("Rejecting declaration: {}", id);
        Declaration declaration = declarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Declaration", id));

        declaration.updateStatus("REJECTED", reason != null && !reason.isBlank() ? "Rejeté: " + reason : "Déclaration de volume rejetée");
        String author = (username != null && !username.isBlank()) ? username : "SYSTEM";
        declaration.setChangeby(author);

        Declaration saved = declarationRepository.save(declaration);
        return DeclarationResponse.fromEntity(saved);
    }

    @Override
    public void deleteDeclaration(String id) {
        log.info("Deleting declaration: {}", id);
        Declaration declaration = declarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Declaration", id));
        declarationRepository.delete(declaration);
    }
}
