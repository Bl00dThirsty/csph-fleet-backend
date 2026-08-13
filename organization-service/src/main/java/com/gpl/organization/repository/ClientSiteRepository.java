package com.gpl.organization.repository;

import com.gpl.organization.model.ClientSite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientSiteRepository extends JpaRepository<ClientSite, String> {
    Optional<ClientSite> findBySiteId(String siteId);
    Page<ClientSite> findByClientOrganizationId(String clientOrganizationId, Pageable pageable);
    boolean existsBySiteId(String siteId);
}
