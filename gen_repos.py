import os

base_dir = r"c:\Users\User\Downloads\gpl-rfid-livraisons\backend\organization-service\src\main\java\com\gpl\organization"

def write_file(sub_dir, name, content):
    d = os.path.join(base_dir, sub_dir)
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, name), "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

write_file("repository", "OrganizationRepository.java", """
package com.gpl.organization.repository;

import com.gpl.organization.model.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {
    Optional<Organization> findByCode(String code);
    List<Organization> findByType(String type);
    List<Organization> findByTier(String tier);
    List<Organization> findByParentOrganizationId(String parentOrganizationId);
    List<Organization> findByHierarchyPathStartingWith(String hierarchyPathPrefix);
    List<Organization> findByIsActiveTrue();
    Page<Organization> findByNameContainingIgnoreCase(String name, Pageable pageable);
    boolean existsByCode(String code);
}
""")

write_file("repository", "SiteRepository.java", """
package com.gpl.organization.repository;

import com.gpl.organization.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, String> {
    List<Site> findByOrganizationId(String organizationId);
    Optional<Site> findByCode(String code);
    Optional<Site> findByOrgIdAndSiteId(String orgId, String siteId);
    List<Site> findByType(String type);
    List<Site> findByCity(String city);
    List<Site> findByIsOperationalTrue();

    @Query("SELECT s FROM Site s WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(s.latitude)))) < :radius")
    List<Site> findNearby(@Param("lat") double lat, @Param("lon") double lon, @Param("radius") double radiusKm);

    boolean existsByCode(String code);
}
""")

write_file("repository", "ClassStructureRepository.java", """
package com.gpl.organization.repository;

import com.gpl.organization.model.ClassStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassStructureRepository extends JpaRepository<ClassStructure, String> {
    List<ClassStructure> findByObjectName(String objectName);
    List<ClassStructure> findByParentClassStructureId(String parentClassStructureId);
    List<ClassStructure> findByHierarchyPathStartingWith(String hierarchyPathPrefix);
    List<ClassStructure> findByOrgIdAndSiteId(String orgId, String siteId);
}
""")

write_file("repository", "OrganizationRelationshipRepository.java", """
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
""")

print("Repositories generated!")
