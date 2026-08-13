package com.gpl.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateRequest {
    @NotBlank(message = "code is required")
    private String code;
    
    @NotBlank(message = "name is required")
    private String name;
    
    @NotBlank(message = "subject is required")
    private String subject;
    
    @NotBlank(message = "bodyTemplate is required")
    private String bodyTemplate;
    
    @NotBlank(message = "module is required")
    private String module;
    
    @NotBlank(message = "channel is required")
    private String channel;
    
    @NotBlank(message = "language is required")
    private String language;
}
