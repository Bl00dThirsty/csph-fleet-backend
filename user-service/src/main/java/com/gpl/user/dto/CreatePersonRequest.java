package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
@Data
public class CreatePersonRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    private String email;
    private String organizationId;
    private String orgId;
    private String primarySiteId;
    private String title;
    private String jobCode;
    private String primaryPhone;
    private String city;
    private String language;
    private String supervisorId;
    private Integer deviceClass;
    private List<PhoneDto> phones;
    private List<EmailDto> emails;
}
