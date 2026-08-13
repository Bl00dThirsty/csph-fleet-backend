package com.gpl.user.model;

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
