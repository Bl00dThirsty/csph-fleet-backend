package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class AddGroupMemberRequest {
    @NotBlank private String personId;
}
