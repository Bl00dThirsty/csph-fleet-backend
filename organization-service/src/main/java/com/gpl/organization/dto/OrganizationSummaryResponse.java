package com.gpl.organization.dto;

import lombok.Data;

@Data
public class OrganizationSummaryResponse {
    private String id;
    private String code;
    private String name;
    private String type;
    private String typeDescription;
    private String tier;
    private String tierDescription;
    private String status;
    private String statusDescription;
    private boolean isActive;
    private String hierarchyPath;
    private int hierarchyLevel;
    private boolean hasChildren;
}
