package com.gpl.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    @NotBlank(message = "templateCode is required")
    private String templateCode;

    @NotBlank(message = "recipientPersonId is required")
    private String recipientPersonId;
    
    private String recipientEmail;

    private String channel;

    private Map<String, String> variables;

    private String relatedEntityType;

    private String relatedEntityId;
}
