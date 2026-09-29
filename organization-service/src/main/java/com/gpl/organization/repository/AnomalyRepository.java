package com.gpl.organization.repository;

import com.gpl.organization.model.Anomaly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, String> {
    List<Anomaly> findBySiteId(UUID siteId);
    List<Anomaly> findByStatus(String status);
    List<Anomaly> findBySeverity(String severity);
    List<Anomaly> findByCategory(String category);
    Page<Anomaly> findAll(Pageable pageable);
}
