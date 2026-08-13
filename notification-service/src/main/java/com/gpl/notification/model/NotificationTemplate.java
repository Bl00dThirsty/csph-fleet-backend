package com.gpl.notification.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_templates", indexes = {
        @Index(name = "idx_template_code", columnList = "code", unique = true),
        @Index(name = "idx_template_module", columnList = "module"),
        @Index(name = "idx_template_is_active", columnList = "isActive")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;

    @Column(nullable = false)
    private String module;

    @Column(nullable = false)
    private String channel; // e.g. "EMAIL", "SMS", "PUSH"

    @Column(nullable = false)
    private String language = "FR";

    @Column(nullable = false)
    private Boolean isActive = true;

    private Integer sortOrder = 0;
}
