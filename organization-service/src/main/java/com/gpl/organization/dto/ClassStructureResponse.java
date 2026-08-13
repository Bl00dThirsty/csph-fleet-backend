package com.gpl.organization.dto;

import lombok.Data;

@Data
public class ClassStructureResponse {
    private String id;
    private String classificationId;
    private String description;
    private String hierarchyPath;
    private String parentClassStructureId;
    private String objectName;
    private int sortOrder;
    private boolean show;
    private boolean useClassInDesc;
    private boolean isTopLevel;
    private String orgId;
    private String siteId;
}
