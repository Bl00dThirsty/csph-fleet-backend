package com.gpl.organization.repository;

import com.gpl.organization.model.OrganizationRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRelationshipRepository extends JpaRepository<OrganizationRelationship, String> {
    List<OrganizationRelationship> findBySourceOrganizationId(String sourceOrganizationId);
    List<OrganizationRelationship> findByTargetOrganizationId(String targetOrganizationId);
    List<OrganizationRelationship> findByType(String type);
    List<OrganizationRelationship> findByIsActiveTrue();
}
