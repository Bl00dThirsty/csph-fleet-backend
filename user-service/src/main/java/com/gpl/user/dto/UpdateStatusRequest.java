package com.gpl.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class UpdateStatusRequest {
    @NotBlank private String newStatus;
    private String reason;
}
