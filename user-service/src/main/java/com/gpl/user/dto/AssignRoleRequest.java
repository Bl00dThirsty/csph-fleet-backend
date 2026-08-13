package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.Instant;
@Data
public class AssignRoleRequest {
    @NotBlank private String personId;
    @NotBlank private String roleId;
    private String siteId;
    private Instant validFrom;
    private Instant validUntil;
    private boolean isPrimary;
}
