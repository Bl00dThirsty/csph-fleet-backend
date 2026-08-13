import os

base_dir = r"c:\Users\User\Downloads\gpl-rfid-livraisons\backend\organization-service\src\main\java\com\gpl\organization"
model_dir = os.path.join(base_dir, "model")

def write_file(name, content):
    os.makedirs(model_dir, exist_ok=True)
    with open(os.path.join(model_dir, name), "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

write_file("Organization.java", """
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
""")

write_file("Site.java", """
package com.gpl.organization.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sites", indexes = {
    @Index(name = "idx_site_code", columnList = "code", unique = true),
    @Index(name = "idx_site_org", columnList = "organizationId"),
    @Index(name = "idx_site_type", columnList = "type"),
    @Index(name = "idx_site_status", columnList = "status"),
    @Index(name = "idx_site_class_struct", columnList = "classStructureId"),
    @Index(name = "idx_site_city", columnList = "city")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String siteId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "organizationId", insertable = false, updatable = false)
    private String organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizationId")
    private Organization organization;

    private String orgId;

    @Column(nullable = false)
    private String type;

    private String typeDescription;

    private String classStructureId;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String region;

    @Builder.Default
    private String country = "CM";

    private String postalCode;
    private Double latitude;
    private Double longitude;

    @Builder.Default
    private int geofenceRadiusMeters = 200;

    @Builder.Default
    private boolean isDefault = false;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private boolean isLocked = false;

    @Builder.Default
    private boolean disabled = false;

    @Builder.Default
    private boolean isOperational = true;

    @Builder.Default
    private boolean hasStorageCapacity = false;

    @Builder.Default
    private boolean isRepairFacility = false;

    private Double storageCapacityTons;
    private Integer maxVehicleBays;
    private String operatingHoursStart;
    private String operatingHoursEnd;
}
""")

write_file("ClassStructure.java", """
package com.gpl.organization.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_structures", indexes = {
    @Index(name = "idx_class_classification", columnList = "classificationId"),
    @Index(name = "idx_class_parent", columnList = "parentClassStructureId"),
    @Index(name = "idx_class_hierarchy", columnList = "hierarchyPath"),
    @Index(name = "idx_class_object", columnList = "objectName")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassStructure extends BaseEntity {

    @Id
    @Column(name = "classStructureId", updatable = false, nullable = false)
    private String id; // Overriding inherited UUID ID

    private String classificationId;
    private String description;
    private String hierarchyPath;
    private String parentClassStructureId;
    private String objectName;
    private int sortOrder;

    @Builder.Default
    private boolean show = true;

    @Builder.Default
    private boolean useClassInDesc = true;

    @Builder.Default
    private boolean isTopLevel = false;

    private String orgId;
    private String siteId;
}
""")

write_file("OrganizationRelationship.java", """
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
""")

print("Models generated!")
