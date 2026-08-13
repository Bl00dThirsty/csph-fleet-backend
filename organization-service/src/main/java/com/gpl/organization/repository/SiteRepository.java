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
