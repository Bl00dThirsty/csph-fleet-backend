package com.gpl.user.repository;
import com.gpl.user.model.UserSiteAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface UserSiteAssignmentRepository extends JpaRepository<UserSiteAssignment, String> {
    List<UserSiteAssignment> findByPersonId(String personId);
    List<UserSiteAssignment> findBySiteId(String siteId);
    
    @Query("SELECT usa FROM UserSiteAssignment usa WHERE usa.personId = :personId AND usa.isPrimary = true AND usa.isActive = true")
    Optional<UserSiteAssignment> findPrimaryByPersonId(@Param("personId") String personId);
    
    List<UserSiteAssignment> findByPersonIdAndIsActiveTrue(String personId);
}
