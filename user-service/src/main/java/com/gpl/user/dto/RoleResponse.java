package com.gpl.user.dto;
import lombok.Data;
import java.util.List;
@Data
public class RoleResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private String scopeOrgType;
    private String minTier;
    private String status;
    private String statusDescription;
    private boolean isSystemRole;
    private boolean isActive;
    private int sortOrder;
    private List<PermissionResponse> permissions;
}
