package com.gpl.user.model;

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

    /**
     * Getter providing roleCode for auth-service JWT role mapping compatibility.
     *
     * @return the role identifier/code
     */
    public String getRoleCode() {
        return roleId;
    }
}
