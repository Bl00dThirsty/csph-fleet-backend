package com.gpl.user.dto;
import lombok.Data;
@Data
public class GroupResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private String organizationId;
    private String siteId;
    private boolean isActive;
    private boolean isSystemGroup;
    private int memberCount;
}
