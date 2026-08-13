package com.gpl.tour.repository;

import com.gpl.tour.model.PickupRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PickupRequestRepository extends JpaRepository<PickupRequest, String>, JpaSpecificationExecutor<PickupRequest> {
    Page<PickupRequest> findByMarketerOrganizationId(String marketerOrganizationId, Pageable pageable);
    Page<PickupRequest> findBySourceSiteId(String sourceSiteId, Pageable pageable);
}
