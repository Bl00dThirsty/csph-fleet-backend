package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class CreateRoleRequest {
    @NotBlank private String code;
    @NotBlank private String name;
    private String description;
    private String scopeOrgType;
    private String minTier;
    private int sortOrder;
}
