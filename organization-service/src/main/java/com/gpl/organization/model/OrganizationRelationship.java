package com.gpl.organization.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "organization_relationships", indexes = {
    @Index(name = "idx_org_rel_source", columnList = "sourceOrganizationId"),
    @Index(name = "idx_org_rel_target", columnList = "targetOrganizationId"),
    @Index(name = "idx_org_rel_type", columnList = "type"),
    @Index(name = "idx_org_rel_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRelationship extends AuditableEntity {

    @Column(nullable = false)
    private String sourceOrganizationId;

    @Column(nullable = false)
    private String targetOrganizationId;

    @Column(nullable = false)
    private String type;

    private String typeDescription;
    private String contractReference;
    private Instant validFrom;
    private Instant validUntil;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private boolean isExclusive = false;

    private String description;
}
