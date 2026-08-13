package com.gpl.user.dto;
import lombok.Data;
@Data
public class PermissionResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private String module;
    private String moduleDescription;
    private boolean isActive;
    private int sortOrder;
}
