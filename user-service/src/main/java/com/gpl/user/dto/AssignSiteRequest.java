package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class AssignSiteRequest {
    @NotBlank private String personId;
    @NotBlank private String siteId;
    private String organizationId;
    private boolean isPrimary;
    private boolean isDefault;
}
