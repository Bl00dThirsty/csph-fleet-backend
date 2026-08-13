import os

base_dir = r"c:\Users\User\Downloads\gpl-rfid-livraisons\backend\user-service"

files = {}

files["src/main/java/com/gpl/user/model/Person.java"] = """package com.gpl.user.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "persons", indexes = {
        @Index(name = "idx_person_personid", columnList = "personId", unique = true),
        @Index(name = "idx_person_username", columnList = "username", unique = true),
        @Index(name = "idx_person_org", columnList = "organizationId"),
        @Index(name = "idx_person_org_site", columnList = "orgId, siteId"),
        @Index(name = "idx_person_supervisor", columnList = "supervisorId"),
        @Index(name = "idx_person_status", columnList = "status"),
        @Index(name = "idx_person_jobcode", columnList = "jobCode")
})
@Getter
@Setter
public class Person extends AuditableEntity {
    @Column(unique = true, nullable = false)
    private String personId;

    private Long personUid;
    private String organizationId;
    private String orgId;
    private String primarySiteId;
    private String siteId;
    private String locationOrg;
    private String locationSite;
    private String location;

    @Column(unique = true)
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
    private String language = "FR";
    private String avatarUrl;
    private String supervisorId;

    private boolean isActive = true;
    private boolean isLocked = false;
    private boolean isCertified = false;
    private boolean acceptingWfMail = true;
    private boolean locToServReq = false;
    private boolean statusIface = false;

    private Integer deviceClass = 0;
    private String deviceClassDescription;

    private String wfMailElection = "PROCESS";
    private String wfMailElectionDescription;
    private String transEmailElection = "NEVER";
    private String transEmailElectionDescription;

    private Instant lastLoginAt;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PersonPhone> phones = new ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PersonEmail> emails = new ArrayList<>();
}
"""

files["src/main/java/com/gpl/user/model/PersonPhone.java"] = """package com.gpl.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "person_phones", indexes = {
        @Index(name = "idx_personphone_personid", columnList = "personId")
})
@Getter
@Setter
public class PersonPhone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer phoneId;

    @Column(name = "personId", insertable = false, updatable = false)
    private String personId;

    private String phoneNum;
    private String type;
    private String typeDescription;
    private boolean isPrimary;

    @Version
    private Long rowStamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personId", referencedColumnName = "personId")
    private Person person;
}
"""

files["src/main/java/com/gpl/user/model/PersonEmail.java"] = """package com.gpl.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "person_emails", indexes = {
        @Index(name = "idx_personemail_personid", columnList = "personId")
})
@Getter
@Setter
public class PersonEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer emailId;

    @Column(name = "personId", insertable = false, updatable = false)
    private String personId;

    private String emailAddress;
    private String type;
    private String typeDescription;
    private boolean isPrimary;

    @Version
    private Long rowStamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personId", referencedColumnName = "personId")
    private Person person;
}
"""

files["src/main/java/com/gpl/user/model/Role.java"] = """package com.gpl.user.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles", indexes = {
        @Index(name = "idx_role_code", columnList = "code", unique = true),
        @Index(name = "idx_role_scope", columnList = "scopeOrgType"),
        @Index(name = "idx_role_status", columnList = "status")
})
@Getter
@Setter
public class Role extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String code;
    private String name;
    private String description;
    private String scopeOrgType;
    private String minTier;
    private String status = "ACTIVE";
    private String statusDescription = "Actif";
    private boolean isSystemRole = false;
    private boolean isActive = true;
    private int sortOrder;
}
"""

files["src/main/java/com/gpl/user/model/Permission.java"] = """package com.gpl.user.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permissions", indexes = {
        @Index(name = "idx_permission_code", columnList = "code", unique = true),
        @Index(name = "idx_permission_module", columnList = "module")
})
@Getter
@Setter
public class Permission extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String code;
    private String name;
    private String description;
    private String module;
    private String moduleDescription;
    private boolean isActive = true;
    private int sortOrder;
}
"""

files["src/main/java/com/gpl/user/model/RolePermission.java"] = """package com.gpl.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "role_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"roleId", "permissionId"})
})
@Getter
@Setter
public class RolePermission {
    @Id
    private String id;
    private String roleId;
    private String permissionId;
    private boolean isGranted = true;
    private String grantedBy;
    private Instant grantedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (grantedAt == null) {
            grantedAt = Instant.now();
        }
    }
}
"""

files["src/main/java/com/gpl/user/model/UserRoleAssignment.java"] = """package com.gpl.user.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "user_role_assignments", indexes = {
        @Index(name = "idx_ura_person", columnList = "personId"),
        @Index(name = "idx_ura_role", columnList = "roleId"),
        @Index(name = "idx_ura_org", columnList = "organizationId"),
        @Index(name = "idx_ura_site", columnList = "siteId")
})
@Getter
@Setter
public class UserRoleAssignment extends AuditableEntity {
    private String personId;
    private String roleId;
    private String organizationId;
    private String siteId;
    private boolean isPrimary;
    private boolean isActive = true;
    private Instant validFrom;
    private Instant validUntil;
}
"""

files["src/main/java/com/gpl/user/model/UserSiteAssignment.java"] = """package com.gpl.user.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_site_assignments", indexes = {
        @Index(name = "idx_usa_person", columnList = "personId"),
        @Index(name = "idx_usa_site", columnList = "siteId")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"personId", "siteId"})
})
@Getter
@Setter
public class UserSiteAssignment extends AuditableEntity {
    private String personId;
    private String siteId;
    private String organizationId;
    private boolean isPrimary = false;
    private boolean isDefault = false;
    private boolean isActive = true;
}
"""

files["src/main/java/com/gpl/user/model/UserGroup.java"] = """package com.gpl.user.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_groups", indexes = {
        @Index(name = "idx_ug_code", columnList = "code", unique = true),
        @Index(name = "idx_ug_org", columnList = "organizationId"),
        @Index(name = "idx_ug_site", columnList = "siteId")
})
@Getter
@Setter
public class UserGroup extends AuditableEntity {
    @Column(unique = true, nullable = false)
    private String code;
    private String name;
    private String description;
    private String organizationId;
    private String siteId;
    private boolean isActive = true;
    private boolean isSystemGroup = false;
    private int memberCount = 0;
}
"""

files["src/main/java/com/gpl/user/model/UserGroupMembership.java"] = """package com.gpl.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_group_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"personId", "groupId"})
})
@Getter
@Setter
public class UserGroupMembership {
    @Id
    private String id;
    private String personId;
    private String groupId;
    private boolean isActive = true;
    private Instant joinedAt;
    private String addedBy;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }
}
"""

for path, content in files.items():
    full_path = os.path.join(base_dir, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\\n")

print("Generated models.")
