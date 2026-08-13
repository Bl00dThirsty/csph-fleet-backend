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

import java.time.Instant;

@Entity
@Table(name = "notification_logs", indexes = {
        @Index(name = "idx_log_recipient_person_id", columnList = "recipientPersonId"),
        @Index(name = "idx_log_channel", columnList = "channel"),
        @Index(name = "idx_log_status", columnList = "status"),
        @Index(name = "idx_log_sent_at", columnList = "sentAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog extends BaseEntity {

    @Column(nullable = false)
    private String templateCode;

    @Column(nullable = false)
    private String recipientPersonId;

    private String recipientEmail;

    private String recipientPhone;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(nullable = false)
    private String status; // e.g. "PENDING", "SENT", "FAILED", "DELIVERED"

    private String statusDescription;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant sentAt;

    private Instant deliveredAt;

    private String relatedEntityType;

    private String relatedEntityId;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON
}
