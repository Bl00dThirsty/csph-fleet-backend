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
