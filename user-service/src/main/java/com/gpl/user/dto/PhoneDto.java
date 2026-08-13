package com.gpl.user.dto;
import lombok.Data;
@Data
public class PhoneDto {
    private Integer phoneId;
    private String phoneNum;
    private String type;
    private String typeDescription;
    private boolean isPrimary;
}
