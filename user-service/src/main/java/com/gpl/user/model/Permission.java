package com.gpl.user.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permissions", indexes = {
        @Index(name = "idx_permission_code", columnList = "code", unique = true),
        @Index(name = "idx_permission_module", columnList = "module")
})
@Getter
@Setter
public class Permission extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String code;
    private String name;
    private String description;
    private String module;
    private String moduleDescription;
    private boolean isActive = true;
    private int sortOrder;
}
