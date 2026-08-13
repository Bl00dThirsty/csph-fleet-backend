package com.gpl.user.model;

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
