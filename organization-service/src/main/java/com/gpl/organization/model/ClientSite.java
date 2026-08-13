package com.gpl.organization.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "client_sites", indexes = {
    @Index(name = "idx_clientsite_site", columnList = "siteId", unique = true),
    @Index(name = "idx_clientsite_org", columnList = "clientOrganizationId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSite extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siteId", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clientOrganizationId", nullable = false)
    private Organization clientOrganization;

    @Column(name = "siteContactPersonId")
    private String siteContactPersonId;

    @Column(columnDefinition = "TEXT")
    private String deliveryInstructions;

    @Column(columnDefinition = "TEXT")
    private String specificRequirements;

    @Builder.Default
    private boolean requiresAuthorization = false;

    @Column(columnDefinition = "TEXT")
    private String operatingConstraints;
}
