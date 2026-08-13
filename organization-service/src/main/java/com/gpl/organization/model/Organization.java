package com.gpl.organization.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_org_code", columnList = "code", unique = true),
    @Index(name = "idx_org_parent", columnList = "parentOrganizationId"),
    @Index(name = "idx_org_type", columnList = "type"),
    @Index(name = "idx_org_tier", columnList = "tier"),
    @Index(name = "idx_org_status", columnList = "status"),
    @Index(name = "idx_org_hierarchy", columnList = "hierarchyPath"),
    @Index(name = "idx_org_class_struct", columnList = "classStructureId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String type;

    private String typeDescription;

    @Column(nullable = false)
    private String tier;

    private String tierDescription;

    private String classStructureId;

    private String hierarchyPath;

    @Column(name = "parentOrganizationId", insertable = false, updatable = false)
    private String parentOrganizationId;

    @Builder.Default
    private int hierarchyLevel = 0;

    private String logoUrl;
    private String taxId;
    private String registrationNumber;
    private String contactEmail;
    private String contactPhone;
    private String website;

    @Builder.Default
    private String currency = "XAF";

    @Builder.Default
    private String language = "FR";

    @Builder.Default
    private String timezone = "Africa/Douala";

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private boolean isLocked = false;

    @Builder.Default
    private boolean isHeadquarters = false;

    @Builder.Default
    private boolean hasChildren = false;

    @Builder.Default
    private boolean isSystemOrg = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentOrganizationId")
    private Organization parent;

    @OneToMany(mappedBy = "parent")
    @Builder.Default
    private List<Organization> children = new ArrayList<>();
}
