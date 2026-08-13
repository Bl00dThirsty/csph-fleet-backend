package com.gpl.user.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "persons", indexes = {
        @Index(name = "idx_person_personid", columnList = "personId", unique = true),
        @Index(name = "idx_person_org", columnList = "organizationId"),
        @Index(name = "idx_person_org_site", columnList = "orgId, siteId"),
        @Index(name = "idx_person_supervisor", columnList = "supervisorId"),
        @Index(name = "idx_person_status", columnList = "status"),
        @Index(name = "idx_person_jobcode", columnList = "jobCode")
})
@Getter
@Setter
public class Person extends AuditableEntity {
    @Column(unique = true, nullable = false)
    private String personId;

    private Long personUid;
    private String organizationId;
    private String orgId;
    private String primarySiteId;
    private String siteId;
    private String locationOrg;
    private String locationSite;
    private String location;

    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private String title;
    private String jobCode;
    private String jobCodeDescription;
    private String primaryPhone;
    private String addressLine1;
    private String city;
    private String language = "FR";
    private String avatarUrl;
    private String supervisorId;

    private boolean isActive = true;
    private boolean isLocked = false;
    private boolean isCertified = false;
    private boolean acceptingWfMail = true;
    private boolean locToServReq = false;
    private boolean statusIface = false;

    private Integer deviceClass = 0;
    private String deviceClassDescription;

    private String wfMailElection = "PROCESS";
    private String wfMailElectionDescription;
    private String transEmailElection = "NEVER";
    private String transEmailElectionDescription;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PersonPhone> phones = new ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PersonEmail> emails = new ArrayList<>();
}
