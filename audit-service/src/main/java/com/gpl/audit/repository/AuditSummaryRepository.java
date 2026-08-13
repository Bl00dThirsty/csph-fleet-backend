package com.gpl.audit.repository;

import com.gpl.audit.model.AuditSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditSummaryRepository extends JpaRepository<AuditSummary, String> {
    Optional<AuditSummary> findByEntityTypeAndEntityId(String entityType, String entityId);
}
