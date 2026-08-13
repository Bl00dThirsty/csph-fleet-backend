package com.gpl.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_token", columnList = "token", unique = true),
        @Index(name = "idx_refresh_tokens_person_id", columnList = "personId"),
        @Index(name = "idx_refresh_tokens_expires_at", columnList = "expiresAt")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @Column(length = 36)
    private String id;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private String personId;

    private String username;

    @Column(nullable = false)
    private Instant expiresAt;

    @Builder.Default
    private boolean isRevoked = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private String deviceInfo;

    @Column(length = 45)
    private String ipAddress;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
