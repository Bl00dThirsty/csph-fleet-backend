package com.gpl.user.repository;
import com.gpl.user.model.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, String> {
    List<UserRoleAssignment> findByPersonId(String personId);
    List<UserRoleAssignment> findByRoleId(String roleId);
    List<UserRoleAssignment> findByPersonIdAndIsActiveTrue(String personId);
    List<UserRoleAssignment> findByPersonIdAndOrganizationId(String personId, String organizationId);
    
    @Query("SELECT ura FROM UserRoleAssignment ura WHERE ura.personId = :personId AND ura.isActive = true AND (ura.validUntil IS NULL OR ura.validUntil > CURRENT_TIMESTAMP)")
    List<UserRoleAssignment> findActiveByPersonId(@Param("personId") String personId);
}
