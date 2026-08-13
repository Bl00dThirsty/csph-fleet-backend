package com.gpl.user.model;

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
