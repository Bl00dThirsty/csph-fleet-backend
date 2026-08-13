package com.gpl.organization.dto;

import com.gpl.common.dto.ModificationSubObject;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrganizationResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private String type;
    private String typeDescription;
    private String tier;
    private String tierDescription;
    private String classStructureId;
    private String hierarchyPath;
    private String parentOrganizationId;
    private int hierarchyLevel;
    private String logoUrl;
    private String taxId;
    private String registrationNumber;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private String currency;
    private String language;
    private String timezone;
    private boolean isActive;
    private boolean isLocked;
    private boolean isHeadquarters;
    private boolean hasChildren;
    private boolean isSystemOrg;
    private String status;
    private String modificationsRef;
    private List<ModificationSubObject> recentModifications = new ArrayList<>();
}
