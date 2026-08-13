package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class CreateGroupRequest {
    @NotBlank private String code;
    @NotBlank private String name;
    private String description;
    @NotBlank private String organizationId;
    private String siteId;
}
