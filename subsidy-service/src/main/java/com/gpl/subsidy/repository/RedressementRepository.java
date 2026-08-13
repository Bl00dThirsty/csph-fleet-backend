package com.gpl.subsidy.repository;

import com.gpl.subsidy.model.Redressement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedressementRepository extends JpaRepository<Redressement, String>, JpaSpecificationExecutor<Redressement> {
    List<Redressement> findByReconciliationId(String reconciliationId);
}
