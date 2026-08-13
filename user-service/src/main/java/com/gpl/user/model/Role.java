package com.gpl.user.model;

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
