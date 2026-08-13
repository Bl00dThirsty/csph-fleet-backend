package com.gpl.user.dto;
import lombok.Data;
@Data
public class EmailDto {
    private Integer emailId;
    private String emailAddress;
    private String type;
    private String typeDescription;
    private boolean isPrimary;
}
