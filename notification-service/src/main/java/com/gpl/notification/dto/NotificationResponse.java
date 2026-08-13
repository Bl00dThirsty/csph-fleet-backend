package com.gpl.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private String templateCode;
    private String recipientPersonId;
    private String channel;
    private String subject;
    private String status;
    private String statusDescription;
    private Instant sentAt;
}
