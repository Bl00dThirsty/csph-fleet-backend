package com.gpl.user.model;

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
