import os

base_dir = r"c:\Users\User\Downloads\gpl-rfid-livraisons\backend\organization-service\src\main\java\com\gpl\organization"

def write_file(sub_dir, name, content):
    d = os.path.join(base_dir, sub_dir)
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, name), "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

write_file("dto", "CreateOrganizationRequest.java", """
package com.gpl.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrganizationRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String type;
    @NotBlank
    private String tier;
    private String classStructureId;
    private String parentOrganizationId;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private boolean isHeadquarters;
}
""")

write_file("dto", "UpdateOrganizationRequest.java", """
package com.gpl.organization.dto;

import lombok.Data;

@Data
public class UpdateOrganizationRequest {
    private String name;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private String logoUrl;
    private String taxId;
    private String registrationNumber;
    private String currency;
    private String language;
    private String timezone;
    private Boolean isHeadquarters;
}
""")

write_file("dto", "OrganizationResponse.java", """
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
""")

write_file("dto", "OrganizationSummaryResponse.java", """
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
""")

write_file("dto", "CreateSiteRequest.java", """
package com.gpl.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSiteRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String siteId;
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String organizationId;
    @NotBlank
    private String type;
    private String addressLine1;
    private String city;
    private String region;
    private Double latitude;
    private Double longitude;
    private Integer geofenceRadiusMeters;
    private Double storageCapacityTons;
    private Integer maxVehicleBays;
}
""")

write_file("dto", "UpdateSiteRequest.java", """
package com.gpl.organization.dto;

import lombok.Data;

@Data
public class UpdateSiteRequest {
    private String name;
    private String description;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String region;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private Integer geofenceRadiusMeters;
    private String operatingHoursStart;
    private String operatingHoursEnd;
}
""")

write_file("dto", "SiteResponse.java", """
package com.gpl.organization.dto;

import com.gpl.common.dto.ModificationSubObject;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class SiteResponse {
    private String id;
    private String code;
    private String siteId;
    private String name;
    private String description;
    private String organizationId;
    private String orgId;
    private String type;
    private String typeDescription;
    private String classStructureId;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String region;
    private String country;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private int geofenceRadiusMeters;
    private boolean isDefault;
    private boolean isActive;
    private boolean isLocked;
    private boolean disabled;
    private boolean isOperational;
    private boolean hasStorageCapacity;
    private boolean isRepairFacility;
    private Double storageCapacityTons;
    private Integer maxVehicleBays;
    private String operatingHoursStart;
    private String operatingHoursEnd;
    private String status;
    private String modificationsRef;
    private List<ModificationSubObject> recentModifications = new ArrayList<>();
}
""")

write_file("dto", "SiteSummaryResponse.java", """
package com.gpl.organization.dto;

import lombok.Data;

@Data
public class SiteSummaryResponse {
    private String id;
    private String code;
    private String siteId;
    private String name;
    private String type;
    private String typeDescription;
    private String orgId;
    private String city;
    private String status;
    private String statusDescription;
    private boolean isOperational;
    private Double latitude;
    private Double longitude;
}
""")

write_file("dto", "ClassStructureResponse.java", """
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
""")

write_file("dto", "CreateRelationshipRequest.java", """
package com.gpl.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.Instant;

@Data
public class CreateRelationshipRequest {
    @NotBlank
    private String sourceOrganizationId;
    @NotBlank
    private String targetOrganizationId;
    @NotBlank
    private String type;
    private String contractReference;
    private Instant validFrom;
    private Instant validUntil;
    private boolean isExclusive;
    private String description;
}
""")

write_file("dto", "RelationshipResponse.java", """
package com.gpl.organization.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class RelationshipResponse {
    private String id;
    private String sourceOrganizationId;
    private String targetOrganizationId;
    private String sourceOrganizationName;
    private String targetOrganizationName;
    private String type;
    private String typeDescription;
    private String contractReference;
    private Instant validFrom;
    private Instant validUntil;
    private boolean isActive;
    private boolean isExclusive;
    private String description;
    private String status;
}
""")

write_file("dto", "UpdateStatusRequest.java", """
package com.gpl.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotBlank
    private String newStatus;
    private String reason;
}
""")

print("DTOs generated!")
