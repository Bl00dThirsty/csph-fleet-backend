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
