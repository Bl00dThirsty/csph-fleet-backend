package com.gpl.subsidy.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.subsidy.dto.CreateReconciliationRequest;
import com.gpl.subsidy.dto.ReconciliationResponse;
import com.gpl.subsidy.model.Declaration;
import com.gpl.subsidy.model.Reconciliation;
import com.gpl.subsidy.repository.DeclarationRepository;
import com.gpl.subsidy.repository.ReconciliationRepository;
import com.gpl.subsidy.service.ReconciliationService;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReconciliationServiceImpl implements ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;
    private final DeclarationRepository declarationRepository;

    private static final double DEFAULT_SUBSIDY_RATE = 100.0;

    @Override
    public ReconciliationResponse createReconciliation(CreateReconciliationRequest request, String username) {
        log.info("Creating reconciliation for declaration: {}", request.getDeclarationId());
        String author = (username != null && !username.isBlank()) ? username : "SYSTEM";

        Declaration declaration = declarationRepository.findById(request.getDeclarationId())
                .orElseThrow(() -> new ResourceNotFoundException("Declaration", request.getDeclarationId()));

        double declaredVolume = declaration.getDeclaredVolume();
        double trackedVolume = request.getTrackedVolume();
        double volumeGap = declaredVolume - trackedVolume;
        double rate = (request.getSubsidyRate() != null && request.getSubsidyRate() > 0) ? request.getSubsidyRate() : DEFAULT_SUBSIDY_RATE;
        double subsidyImpact = volumeGap * rate;

        Reconciliation reconciliation = Reconciliation.builder()
                .declarationId(declaration.getId())
                .trackedVolume(trackedVolume)
                .trackedBottlesOut(request.getTrackedBottlesOut())
                .trackedBottlesIn(request.getTrackedBottlesIn())
                .volumeGap(volumeGap)
                .subsidyImpact(subsidyImpact)
                .verifiedByPersonId(request.getVerifiedByPersonId())
                .notes(request.getNotes())
                .build();

        reconciliation.setStatus("PENDING");
        reconciliation.setStatusDescription("En attente");
        reconciliation.setStatusDate(Instant.now());
        reconciliation.setCreatedBy(author);
        reconciliation.setChangeby(author);

        Reconciliation saved = reconciliationRepository.save(reconciliation);

        // Update declaration status
        declaration.updateStatus("RECONCILED", "Réconcilié");
        declaration.setChangeby(author);
        declarationRepository.save(declaration);

        return ReconciliationResponse.fromEntity(saved, declaredVolume);
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationResponse getReconciliation(String id) {
        log.info("Retrieving reconciliation: {}", id);
        Reconciliation reconciliation = reconciliationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation", id));

        Double declaredVolume = declarationRepository.findById(reconciliation.getDeclarationId())
                .map(Declaration::getDeclaredVolume)
                .orElse(0.0);

        return ReconciliationResponse.fromEntity(reconciliation, declaredVolume);
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationResponse getReconciliationByDeclarationId(String declarationId) {
        log.info("Retrieving reconciliation for declaration: {}", declarationId);
        Reconciliation reconciliation = reconciliationRepository.findByDeclarationId(declarationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation", "declarationId", declarationId));

        Double declaredVolume = declarationRepository.findById(declarationId)
                .map(Declaration::getDeclaredVolume)
                .orElse(0.0);

        return ReconciliationResponse.fromEntity(reconciliation, declaredVolume);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReconciliationResponse> listReconciliations(String declarationId, String status, Pageable pageable) {
        log.info("Listing reconciliations - declarationId: {}, status: {}", declarationId, status);

        Specification<Reconciliation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (declarationId != null && !declarationId.isBlank()) {
                predicates.add(cb.equal(root.get("declarationId"), declarationId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Reconciliation> page = reconciliationRepository.findAll(spec, pageable);
        
        List<String> declarationIds = page.getContent().stream()
                .map(Reconciliation::getDeclarationId)
                .distinct()
                .toList();

        Map<String, Double> declaredVolumes = declarationRepository.findAllById(declarationIds).stream()
                .collect(Collectors.toMap(Declaration::getId, Declaration::getDeclaredVolume));

        List<ReconciliationResponse> content = page.getContent().stream()
                .map(rec -> ReconciliationResponse.fromEntity(rec, declaredVolumes.getOrDefault(rec.getDeclarationId(), 0.0)))
                .toList();

        return PageResponse.of(content, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    public ReconciliationResponse verifyReconciliation(String id, String verifiedByPersonId, String notes, String username) {
        log.info("Verifying reconciliation: {}", id);
        Reconciliation reconciliation = reconciliationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation", id));

        String author = (username != null && !username.isBlank()) ? username : "SYSTEM";
        String verifier = (verifiedByPersonId != null && !verifiedByPersonId.isBlank()) ? verifiedByPersonId : author;

        reconciliation.updateStatus("VERIFIED", "Vérifié");
        reconciliation.setVerifiedByPersonId(verifier);
        reconciliation.setVerifiedAt(Instant.now());
        reconciliation.setChangeby(author);
        if (notes != null && !notes.isBlank()) {
            String existingNotes = reconciliation.getNotes() != null ? reconciliation.getNotes() + " | " : "";
            reconciliation.setNotes(existingNotes + notes);
        }

        Reconciliation updated = reconciliationRepository.save(reconciliation);

        Double declaredVolume = declarationRepository.findById(updated.getDeclarationId())
                .map(Declaration::getDeclaredVolume)
                .orElse(0.0);

        return ReconciliationResponse.fromEntity(updated, declaredVolume);
    }

    @Override
    public void deleteReconciliation(String id) {
        log.info("Deleting reconciliation: {}", id);
        Reconciliation reconciliation = reconciliationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation", id));
        reconciliationRepository.delete(reconciliation);
    }
}
