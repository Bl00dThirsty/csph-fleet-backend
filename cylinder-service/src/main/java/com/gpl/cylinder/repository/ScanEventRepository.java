package com.gpl.cylinder.repository;

import com.gpl.cylinder.model.ScanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanEventRepository extends JpaRepository<ScanEvent, String>, JpaSpecificationExecutor<ScanEvent> {
    List<ScanEvent> findByRfidTagId(String rfidTagId);
    List<ScanEvent> findByLivreurPersonId(String livreurPersonId);
}
