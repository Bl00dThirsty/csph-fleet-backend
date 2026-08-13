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
    private Integer deviceClass;
    private String deviceClassDescription;
    private String wfMailElection;
    private String transEmailElection;
}
