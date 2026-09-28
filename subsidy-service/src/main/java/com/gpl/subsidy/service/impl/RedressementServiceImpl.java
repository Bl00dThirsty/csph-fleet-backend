package com.gpl.subsidy.service.impl;

import com.gpl.common.dto.PageResponse;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.subsidy.dto.CreateRedressementRequest;
import com.gpl.subsidy.dto.RedressementResponse;
import com.gpl.subsidy.model.Reconciliation;
import com.gpl.subsidy.model.Redressement;
import com.gpl.subsidy.repository.ReconciliationRepository;
import com.gpl.subsidy.repository.RedressementRepository;
import com.gpl.subsidy.service.RedressementService;
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
public class RedressementServiceImpl implements RedressementService {

    private final RedressementRepository redressementRepository;
    private final ReconciliationRepository reconciliationRepository;

    @Override
    public RedressementResponse createRedressement(CreateRedressementRequest request, String username) {
        log.info("Creating redressement for reconciliation: {}", request.getReconciliationId());
        String author = (username != null && !username.isBlank()) ? username : "SYSTEM";

        Reconciliation reconciliation = reconciliationRepository.findById(request.getReconciliationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation", request.getReconciliationId()));

        double amount = request.getAmount() != null ? request.getAmount() : Math.abs(reconciliation.getSubsidyImpact());

        Redressement redressement = Redressement.builder()
                .reconciliationId(reconciliation.getId())
                .amount(amount)
                .issuedAt(Instant.now())
                .dueDate(request.getDueDate())
                .transactionRef(request.getTransactionRef())
                .build();

        redressement.updateStatus("ISSUED", "Émis");
        redressement.setCreatedBy(author);
        redressement.setChangeby(author);

        Redressement saved = redressementRepository.save(redressement);

        // Update reconciliation status
        reconciliation.updateStatus("REDRESSEMENTAPPLIED", "Redressement Appliqué");
        reconciliation.setChangeby(author);
        reconciliationRepository.save(reconciliation);

        return RedressementResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RedressementResponse getRedressement(String id) {
        log.info("Retrieving redressement: {}", id);
        Redressement redressement = redressementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Redressement", id));
        return RedressementResponse.fromEntity(redressement);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RedressementResponse> listRedressements(String reconciliationId, String status, Pageable pageable) {
        log.info("Listing redressements - reconciliationId: {}, status: {}", reconciliationId, status);

        Specification<Redressement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (reconciliationId != null && !reconciliationId.isBlank()) {
                predicates.add(cb.equal(root.get("reconciliationId"), reconciliationId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Redressement> page = redressementRepository.findAll(spec, pageable);
        List<RedressementResponse> content = page.getContent().stream()
                .map(RedressementResponse::fromEntity)
                .toList();

        return PageResponse.of(content, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    public RedressementResponse markAsPaid(String id, String transactionRef, String username) {
        log.info("Marking redressement as paid: {}", id);
        Redressement redressement = redressementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Redressement", id));

        String author = (username != null && !username.isBlank()) ? username : "SYSTEM";

        redressement.updateStatus("PAID", "Payé");
        redressement.setPaidAt(Instant.now());
        if (transactionRef != null && !transactionRef.isBlank()) {
            redressement.setTransactionRef(transactionRef);
        }
        redressement.setChangeby(author);

        Redressement updated = redressementRepository.save(redressement);
        return RedressementResponse.fromEntity(updated);
    }

    @Override
    public RedressementResponse cancelRedressement(String id, String username) {
        log.info("Cancelling redressement: {}", id);
        Redressement redressement = redressementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Redressement", id));

        String author = (username != null && !username.isBlank()) ? username : "SYSTEM";

        redressement.updateStatus("CANCELLED", "Annulé");
        redressement.setChangeby(author);

        Redressement updated = redressementRepository.save(redressement);
        return RedressementResponse.fromEntity(updated);
    }

    @Override
    public void deleteRedressement(String id) {
        log.info("Deleting redressement: {}", id);
        Redressement redressement = redressementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Redressement", id));
        redressementRepository.delete(redressement);
    }
}
