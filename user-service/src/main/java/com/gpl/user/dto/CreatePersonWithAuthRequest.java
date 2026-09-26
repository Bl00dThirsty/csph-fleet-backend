package com.gpl.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extended person creation request that includes authentication credentials.
 * Used when creating users who need to log in to the system (e.g., drivers).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreatePersonWithAuthRequest extends CreatePersonRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    private String roleName;
}
