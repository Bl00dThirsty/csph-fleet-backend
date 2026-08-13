package com.gpl.subsidy.repository;

import com.gpl.subsidy.model.Reconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReconciliationRepository extends JpaRepository<Reconciliation, String>, JpaSpecificationExecutor<Reconciliation> {
    Optional<Reconciliation> findByDeclarationId(String declarationId);
}
