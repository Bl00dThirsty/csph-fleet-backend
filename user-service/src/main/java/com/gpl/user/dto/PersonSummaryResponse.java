package com.gpl.user.dto;
import lombok.Data;
@Data
public class PersonSummaryResponse {
    private String id;
    private String personId;
    private String displayName;
    private String title;
    private String jobCode;
    private String orgId;
    private String siteId;
    private String status;
    private String statusDescription;
    private boolean isActive;
}
