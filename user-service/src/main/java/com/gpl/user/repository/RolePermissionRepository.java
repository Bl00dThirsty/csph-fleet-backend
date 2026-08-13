package com.gpl.user.repository;
import com.gpl.user.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface RolePermissionRepository extends JpaRepository<RolePermission, String> {
    List<RolePermission> findByRoleId(String roleId);
    Optional<RolePermission> findByRoleIdAndPermissionId(String roleId, String permissionId);
    @Query("SELECT p.code FROM Permission p JOIN RolePermission rp ON p.id = rp.permissionId WHERE rp.roleId = :roleId AND rp.isGranted = true")
    List<String> findPermissionCodesByRoleId(@Param("roleId") String roleId);
}
