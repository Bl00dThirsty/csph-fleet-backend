package com.gpl.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeResponse {

    private String id;
    private String personId;
    private String username;
    private String email;
    private String organizationId;
    private String orgId;
    private String status;
    private String statusDescription;
    private boolean locked;
    private boolean mustChangePassword;
    private boolean twoFactorEnabled;
    private Instant lastLoginAt;
    private List<String> roles;
}
