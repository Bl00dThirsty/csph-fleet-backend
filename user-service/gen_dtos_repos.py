import os

base_dir = r"c:\Users\User\Downloads\gpl-rfid-livraisons\backend\user-service"

files = {}

# DTOs
files["src/main/java/com/gpl/user/dto/PhoneDto.java"] = """package com.gpl.user.dto;
import lombok.Data;
@Data
public class PhoneDto {
    private Integer phoneId;
    private String phoneNum;
    private String type;
    private String typeDescription;
    private boolean isPrimary;
}"""

files["src/main/java/com/gpl/user/dto/EmailDto.java"] = """package com.gpl.user.dto;
import lombok.Data;
@Data
public class EmailDto {
    private Integer emailId;
    private String emailAddress;
    private String type;
    private String typeDescription;
    private boolean isPrimary;
}"""

files["src/main/java/com/gpl/user/dto/CreatePersonRequest.java"] = """package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
@Data
public class CreatePersonRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @NotBlank private String username;
    private String email;
    private String organizationId;
    private String orgId;
    private String primarySiteId;
    private String title;
    private String jobCode;
    private String primaryPhone;
    private String city;
    private String language;
    private String supervisorId;
    private Integer deviceClass;
    private List<PhoneDto> phones;
    private List<EmailDto> emails;
}"""

files["src/main/java/com/gpl/user/dto/UpdatePersonRequest.java"] = """package com.gpl.user.dto;
import lombok.Data;
@Data
public class UpdatePersonRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String title;
    private String jobCode;
    private String jobCodeDescription;
    private String primaryPhone;
    private String addressLine1;
    private String city;
    private String avatarUrl;
    private String supervisorId;
    private Integer deviceClass;
    private String deviceClassDescription;
    private String wfMailElection;
    private String transEmailElection;
}"""

files["src/main/java/com/gpl/user/dto/PersonResponse.java"] = """package com.gpl.user.dto;
import com.gpl.common.dto.ModificationSubObject;
import com.gpl.user.model.UserRoleAssignment;
import com.gpl.user.model.UserSiteAssignment;
import lombok.Data;
import java.time.Instant;
import java.util.List;
@Data
public class PersonResponse {
    private String id;
    private String personId;
    private Long personUid;
    private String organizationId;
    private String orgId;
    private String primarySiteId;
    private String siteId;
    private String locationOrg;
    private String locationSite;
    private String location;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private String title;
    private String jobCode;
    private String jobCodeDescription;
    private String primaryPhone;
    private String addressLine1;
    private String city;
    private String language;
    private String avatarUrl;
    private String supervisorId;
    private boolean isActive;
    private boolean isLocked;
    private boolean isCertified;
    private boolean acceptingWfMail;
    private boolean locToServReq;
    private boolean statusIface;
    private Integer deviceClass;
    private String deviceClassDescription;
    private String wfMailElection;
    private String wfMailElectionDescription;
    private String transEmailElection;
    private String transEmailElectionDescription;
    private Instant lastLoginAt;
    private String status;
    private String statusDescription;
    private Instant statusDate;
    
    private List<PhoneDto> phones;
    private List<EmailDto> emails;
    private List<UserRoleAssignment> roles;
    private List<UserSiteAssignment> sites;
    
    private String modificationsRef;
    private List<ModificationSubObject> recentModifications;
}"""

files["src/main/java/com/gpl/user/dto/PersonSummaryResponse.java"] = """package com.gpl.user.dto;
import lombok.Data;
@Data
public class PersonSummaryResponse {
    private String id;
    private String personId;
    private String displayName;
    private String title;
    private String jobCode;
    private String orgId;
    private String siteId;
    private String status;
    private String statusDescription;
    private boolean isActive;
}"""

files["src/main/java/com/gpl/user/dto/CreateRoleRequest.java"] = """package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class CreateRoleRequest {
    @NotBlank private String code;
    @NotBlank private String name;
    private String description;
    private String scopeOrgType;
    private String minTier;
    private int sortOrder;
}"""

files["src/main/java/com/gpl/user/dto/RoleResponse.java"] = """package com.gpl.user.dto;
import lombok.Data;
import java.util.List;
@Data
public class RoleResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private String scopeOrgType;
    private String minTier;
    private String status;
    private String statusDescription;
    private boolean isSystemRole;
    private boolean isActive;
    private int sortOrder;
    private List<PermissionResponse> permissions;
}"""

files["src/main/java/com/gpl/user/dto/PermissionResponse.java"] = """package com.gpl.user.dto;
import lombok.Data;
@Data
public class PermissionResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private String module;
    private String moduleDescription;
    private boolean isActive;
    private int sortOrder;
}"""

files["src/main/java/com/gpl/user/dto/AssignRoleRequest.java"] = """package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.Instant;
@Data
public class AssignRoleRequest {
    @NotBlank private String personId;
    @NotBlank private String roleId;
    private String siteId;
    private Instant validFrom;
    private Instant validUntil;
    private boolean isPrimary;
}"""

files["src/main/java/com/gpl/user/dto/AssignSiteRequest.java"] = """package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class AssignSiteRequest {
    @NotBlank private String personId;
    @NotBlank private String siteId;
    private String organizationId;
    private boolean isPrimary;
    private boolean isDefault;
}"""

files["src/main/java/com/gpl/user/dto/CreateGroupRequest.java"] = """package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class CreateGroupRequest {
    @NotBlank private String code;
    @NotBlank private String name;
    private String description;
    @NotBlank private String organizationId;
    private String siteId;
}"""

files["src/main/java/com/gpl/user/dto/GroupResponse.java"] = """package com.gpl.user.dto;
import lombok.Data;
@Data
public class GroupResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private String organizationId;
    private String siteId;
    private boolean isActive;
    private boolean isSystemGroup;
    private int memberCount;
}"""

files["src/main/java/com/gpl/user/dto/AddGroupMemberRequest.java"] = """package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class AddGroupMemberRequest {
    @NotBlank private String personId;
}"""

files["src/main/java/com/gpl/user/dto/GroupMemberResponse.java"] = """package com.gpl.user.dto;
import lombok.Data;
import java.time.Instant;
@Data
public class GroupMemberResponse {
    private String personId;
    private String displayName;
    private Instant joinedAt;
    private String addedBy;
    private boolean isActive;
}"""

files["src/main/java/com/gpl/user/dto/UpdateStatusRequest.java"] = """package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class UpdateStatusRequest {
    @NotBlank private String newStatus;
    private String reason;
}"""

# Repositories
files["src/main/java/com/gpl/user/repository/PersonRepository.java"] = """package com.gpl.user.repository;
import com.gpl.user.model.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PersonRepository extends JpaRepository<Person, String> {
    Optional<Person> findByPersonId(String personId);
    Optional<Person> findByUsername(String username);
    Page<Person> findByOrganizationId(String organizationId, Pageable pageable);
    List<Person> findBySupervisorId(String supervisorId);
    List<Person> findByOrgIdAndSiteId(String orgId, String siteId);
    List<Person> findByJobCode(String jobCode);
    Page<Person> findByDisplayNameContainingIgnoreCase(String search, Pageable pageable);
    Page<Person> findByIsActiveTrue(Pageable pageable);
    boolean existsByPersonId(String personId);
    boolean existsByUsername(String username);
}"""

files["src/main/java/com/gpl/user/repository/PersonPhoneRepository.java"] = """package com.gpl.user.repository;
import com.gpl.user.model.PersonPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PersonPhoneRepository extends JpaRepository<PersonPhone, Integer> {
    List<PersonPhone> findByPersonId(String personId);
    void deleteByPersonIdAndPhoneId(String personId, Integer phoneId);
}"""

files["src/main/java/com/gpl/user/repository/PersonEmailRepository.java"] = """package com.gpl.user.repository;
import com.gpl.user.model.PersonEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PersonEmailRepository extends JpaRepository<PersonEmail, Integer> {
    List<PersonEmail> findByPersonId(String personId);
    void deleteByPersonIdAndEmailId(String personId, Integer emailId);
}"""

files["src/main/java/com/gpl/user/repository/RoleRepository.java"] = """package com.gpl.user.repository;
import com.gpl.user.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByCode(String code);
    List<Role> findByScopeOrgType(String scopeOrgType);
    List<Role> findByIsSystemRoleTrue();
    List<Role> findByIsActiveTrue();
}"""

files["src/main/java/com/gpl/user/repository/PermissionRepository.java"] = """package com.gpl.user.repository;
import com.gpl.user.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PermissionRepository extends JpaRepository<Permission, String> {
    Optional<Permission> findByCode(String code);
    List<Permission> findByModule(String module);
}"""

files["src/main/java/com/gpl/user/repository/RolePermissionRepository.java"] = """package com.gpl.user.repository;
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
}"""

files["src/main/java/com/gpl/user/repository/UserRoleAssignmentRepository.java"] = """package com.gpl.user.repository;
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
}"""

files["src/main/java/com/gpl/user/repository/UserSiteAssignmentRepository.java"] = """package com.gpl.user.repository;
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
}"""

files["src/main/java/com/gpl/user/repository/UserGroupRepository.java"] = """package com.gpl.user.repository;
import com.gpl.user.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserGroupRepository extends JpaRepository<UserGroup, String> {
    List<UserGroup> findByOrganizationId(String organizationId);
    Optional<UserGroup> findByCode(String code);
    boolean existsByCode(String code);
}"""

files["src/main/java/com/gpl/user/repository/UserGroupMembershipRepository.java"] = """package com.gpl.user.repository;
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
}"""

for path, content in files.items():
    full_path = os.path.join(base_dir, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\\n")

print("Generated dtos and repositories.")
