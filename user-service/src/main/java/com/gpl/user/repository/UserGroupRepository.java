package com.gpl.user.repository;
import com.gpl.user.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserGroupRepository extends JpaRepository<UserGroup, String> {
    List<UserGroup> findByOrganizationId(String organizationId);
    Optional<UserGroup> findByCode(String code);
    boolean existsByCode(String code);
}
