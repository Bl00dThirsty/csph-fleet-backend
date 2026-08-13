package com.gpl.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "person_phones", indexes = {
        @Index(name = "idx_personphone_personid", columnList = "personId")
})
@Getter
@Setter
public class PersonPhone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer phoneId;

    @Column(name = "personId", insertable = false, updatable = false)
    private String personId;

    private String phoneNum;
    private String type;
    private String typeDescription;
    private boolean isPrimary;

    @Version
    private Long rowStamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personId", referencedColumnName = "personId")
    private Person person;
}
