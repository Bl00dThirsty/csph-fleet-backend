package com.gpl.user.repository;
import com.gpl.user.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByCode(String code);
    List<Role> findByScopeOrgType(String scopeOrgType);
    List<Role> findByIsSystemRoleTrue();
    List<Role> findByIsActiveTrue();
}
