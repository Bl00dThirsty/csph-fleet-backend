package com.gpl.user.repository;
import com.gpl.user.model.UserGroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserGroupMembershipRepository extends JpaRepository<UserGroupMembership, String> {
    List<UserGroupMembership> findByGroupId(String groupId);
    List<UserGroupMembership> findByPersonId(String personId);
    List<UserGroupMembership> findByGroupIdAndIsActiveTrue(String groupId);
    boolean existsByPersonIdAndGroupId(String personId, String groupId);
    Optional<UserGroupMembership> findByPersonIdAndGroupId(String personId, String groupId);
}
