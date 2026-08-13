package com.gpl.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "person_emails", indexes = {
        @Index(name = "idx_personemail_personid", columnList = "personId")
})
@Getter
@Setter
public class PersonEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer emailId;

    @Column(name = "personId", insertable = false, updatable = false)
    private String personId;

    private String emailAddress;
    private String type;
    private String typeDescription;
    private boolean isPrimary;

    @Version
    private Long rowStamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personId", referencedColumnName = "personId")
    private Person person;
}
