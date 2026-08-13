package com.gpl.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "auth_users",
    indexes = {
        @Index(name = "idx_auth_users_person_id", columnList = "personId", unique = true),
        @Index(name = "idx_auth_users_username", columnList = "username", unique = true),
        @Index(name = "idx_auth_users_email", columnList = "email"),
        @Index(name = "idx_auth_users_status", columnList = "status")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {

    @Id
    @Column(length = 36)
    private String id;

    @Column(unique = true, nullable = false)
    private String personId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private String email;

    private String organizationId;

    private String orgId;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @Builder.Default
    private String statusDescription = "Actif";

    private Instant statusDate;

    @Builder.Default
    private boolean isLocked = false;

    @Builder.Default
    private int failedLoginAttempts = 0;

    private Instant lastLoginAt;

    @Column(length = 45)
    private String lastLoginIp;

    private Instant passwordChangedAt;

    @Builder.Default
    private boolean mustChangePassword = false;

    @Builder.Default
    private boolean twoFactorEnabled = false;

    private String twoFactorSecret;

    @Version
    private Long rowStamp;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private String createdBy;

    private String changeby;

    private Instant changedate;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
