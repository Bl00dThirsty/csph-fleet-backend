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

    @OneToOne(mappedBy = "site", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ClientSite clientSite;
}
