package com.gpl.auth.controller;

import com.gpl.auth.dto.MeResponse;
import com.gpl.auth.dto.UpdateMeRequest;
import com.gpl.auth.model.AuthUser;
import com.gpl.auth.repository.AuthUserRepository;
import com.gpl.common.dto.ApiResponse;
import com.gpl.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final AuthUserRepository authUserRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<MeResponse>> me(
            @RequestHeader(value = "X-User-PersonId", required = false) String personId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        AuthUser user = requireCurrentUser(personId);
        return ResponseEntity.ok(ApiResponse.success(buildMeResponse(user, roles)));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<String>>> permissions(
            @RequestHeader(value = "X-User-PersonId", required = false) String personId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissions) {
        requireCurrentUser(personId);
        return ResponseEntity.ok(ApiResponse.success(splitHeaderValue(permissions)));
    }

    @PatchMapping
    @Transactional
    public ResponseEntity<ApiResponse<MeResponse>> updateMe(
            @RequestHeader(value = "X-User-PersonId", required = false) String personId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @Valid @RequestBody UpdateMeRequest request) {
        AuthUser user = requireCurrentUser(personId);

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        user.setChangeby(personId);
        user.setChangedate(Instant.now());
        authUserRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(buildMeResponse(user, roles)));
    }

    private AuthUser requireCurrentUser(String personId) {
        if (personId == null || personId.isBlank()) {
            throw new UnauthorizedException("Utilisateur non authentifié");
        }
        return authUserRepository.findByPersonId(personId)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur introuvable ou identifiants incorrects"));
    }

    private MeResponse buildMeResponse(AuthUser user, String roles) {
        return MeResponse.builder()
                .id(user.getId())
                .personId(user.getPersonId())
                .username(user.getUsername())
                .email(user.getEmail())
                .organizationId(user.getOrganizationId())
                .orgId(user.getOrgId())
                .status(user.getStatus())
                .statusDescription(user.getStatusDescription())
                .locked(user.isLocked())
                .mustChangePassword(user.isMustChangePassword())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .roles(splitHeaderValue(roles))
                .build();
    }

    private List<String> splitHeaderValue(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(headerValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
