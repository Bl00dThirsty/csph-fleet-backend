package com.gpl.audit.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_summaries", indexes = {
        @Index(name = "idx_audit_summary_entity", columnList = "entityType, entityId", unique = true),
        @Index(name = "idx_audit_summary_modified", columnList = "lastModifiedAt"),
        @Index(name = "idx_audit_summary_modified_by", columnList = "lastModifiedBy")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditSummary extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String entityType;

    @Column(nullable = false, length = 100)
    private String entityId;

    @Column(length = 255)
    private String entityName;

    @Column(nullable = false)
    @Builder.Default
    private int totalModifications = 0;

    private Instant lastModifiedAt;

    @Column(length = 100)
    private String lastModifiedBy;

    @Column(length = 255)
    private String lastModifiedByDisplayName;

    @Column(length = 100)
    private String lastAction;

    @Column(columnDefinition = "TEXT")
    private String lastActionDescription;

    private Instant firstCreatedAt;

    @Column(length = 100)
    private String firstCreatedBy;

    @Column(length = 255)
    private String firstCreatedByDisplayName;
}
