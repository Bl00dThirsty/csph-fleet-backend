package com.gpl.audit.repository;

import com.gpl.common.model.EntityModification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EntityModificationRepository extends JpaRepository<EntityModification, String> {
    
    List<EntityModification> findByEntityTypeAndEntityIdOrderByChangedateDesc(String entityType, String entityId);

    Page<EntityModification> findByEntityTypeAndEntityIdOrderByChangedateDesc(String entityType, String entityId, Pageable pageable);

    Page<EntityModification> findByChangebyOrderByChangedateDesc(String changeby, Pageable pageable);

    Page<EntityModification> findByEntityTypeAndActionOrderByChangedateDesc(String entityType, String action, Pageable pageable);

    Page<EntityModification> findByChangedateBetweenOrderByChangedateDesc(Instant dateFrom, Instant dateTo, Pageable pageable);

    Page<EntityModification> findByEntityTypeAndEntityIdAndChangedateBetweenOrderByChangedateDesc(String entityType, String entityId, Instant dateFrom, Instant dateTo, Pageable pageable);
}
