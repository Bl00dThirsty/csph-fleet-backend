package com.gpl.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateResponse {
    private String id;
    private String code;
    private String name;
    private String subject;
    private String bodyTemplate;
    private String module;
    private String channel;
    private String language;
    private Boolean isActive;
    private Integer sortOrder;
}
