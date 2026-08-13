package com.gpl.audit.repository;

import com.gpl.common.model.StatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, String> {

    List<StatusHistory> findByEntityTypeAndEntityIdOrderByChangedateDesc(String entityType, String entityId);
    
    Page<StatusHistory> findByEntityTypeAndEntityIdOrderByChangedateDesc(String entityType, String entityId, Pageable pageable);

    Page<StatusHistory> findByChangebyOrderByChangedateDesc(String changeby, Pageable pageable);

    Page<StatusHistory> findByChangedateBetweenOrderByChangedateDesc(Instant dateFrom, Instant dateTo, Pageable pageable);
}
