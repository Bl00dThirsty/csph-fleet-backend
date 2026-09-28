package com.gpl.user.dto;
import lombok.Data;
@Data
public class UpdatePersonRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String title;
    private String jobCode;
    private String jobCodeDescription;
    private String primaryPhone;
    private String addressLine1;
    private String city;
    private String avatarUrl;
    private String supervisorId;
    /**
     * Staff transfer: moving a person to another org/site. Absent from the
     * original DTO, which made seeded rows (e.g. a driver created under a
     * transporter) impossible to re-home to their real org via the API.
     */
    private String organizationId;
    private String orgId;
    private String primarySiteId;
    private String siteId;
    private Integer deviceClass;    private String deviceClassDescription;
    private String wfMailElection;
    private String transEmailElection;
}
