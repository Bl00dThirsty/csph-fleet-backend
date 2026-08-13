package com.gpl.tour.repository;

import com.gpl.tour.model.TransporterContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransporterContractRepository extends JpaRepository<TransporterContract, String>, JpaSpecificationExecutor<TransporterContract> {
    List<TransporterContract> findByMarketerOrganizationId(String marketerOrganizationId);
    List<TransporterContract> findByTransporterOrganizationId(String transporterOrganizationId);
    Page<TransporterContract> findByMarketerOrganizationId(String marketerOrganizationId, Pageable pageable);
}
