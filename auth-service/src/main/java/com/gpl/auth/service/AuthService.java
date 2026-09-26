package com.gpl.auth.service;

import com.gpl.common.exception.UnauthorizedException;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.auth.dto.ChangePasswordRequest;
import com.gpl.auth.dto.LoginRequest;
import com.gpl.auth.dto.LoginResponse;
import com.gpl.auth.dto.RegisterAuthUserRequest;
import com.gpl.auth.dto.TokenRefreshRequest;
import com.gpl.auth.dto.TokenRefreshResponse;
import com.gpl.common.dto.ApiResponse;
import com.gpl.auth.model.AuthUser;
import com.gpl.auth.model.RefreshToken;
import com.gpl.auth.repository.AuthUserRepository;
import com.gpl.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import com.gpl.auth.client.UserClient;
import com.gpl.auth.client.AuditClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserClient userClient;
    private final AuditClient auditClient;

    @Value("${gpl.jwt.access-token-expiry}")
    private long jwtExpiration;

    @Value("${gpl.jwt.refresh-token-expiry}")
    private long jwtRefreshExpiration;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String identifier = request.getUsername() != null ? request.getUsername().trim() : "";
        AuthUser user = authUserRepository.findByUsername(identifier)
                .or(() -> authUserRepository.findByEmail(identifier))
                .orElse(null);

        if (user == null) {
            publishLoginFailed("UNKNOWN", request.getUsername(), "Utilisateur introuvable", request);
            throw new UnauthorizedException("Utilisateur introuvable ou identifiants incorrects");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            publishLoginFailed(user.getPersonId(), user.getId().toString(), "Compte utilisateur inactif", request);
            throw new UnauthorizedException("Compte utilisateur inactif");
        }

        if (user.isLocked()) {
            publishLoginFailed(user.getPersonId(), user.getId().toString(), "Compte verrouillé", request);
            throw new UnauthorizedException("Compte verrouillé après plusieurs échecs");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) {
                user.setLocked(true);
            }
            authUserRepository.save(user);
            publishLoginFailed(user.getPersonId(), user.getId().toString(), "Mot de passe incorrect", request);
            throw new UnauthorizedException("Mot de passe incorrect");
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(request.getIpAddress());
        authUserRepository.save(user);

        try {
            auditClient.ingestAudit(Map.of(
                    "module", "AUTH",
                    "changeby", user.getPersonId(),
                    "entityType", "AUTH_USER",
                    "entityId", user.getId().toString(),
                    "action", "LOGIN",
                    "description", "User logged in successfully",
                    "ipAddress", request.getIpAddress() != null ? request.getIpAddress() : "UNKNOWN",
                    "deviceInfo", request.getDeviceInfo() != null ? request.getDeviceInfo() : "UNKNOWN"
            ));
        } catch (Exception e) {
            // Ignore audit failure to not block login
        }

        List<String> roles = new ArrayList<>();
        try {
            var response = userClient.getPersonByPersonId(user.getPersonId());
            if (response != null && response.getData() != null) {
                var data = response.getData();
                if (data.containsKey("roles")) {
                    List<Map<String, Object>> rolesList = (List<Map<String, Object>>) data.get("roles");
                    for (Map<String, Object> roleObj : rolesList) {
                        if (roleObj.containsKey("roleCode") && roleObj.get("roleCode") != null) {
                            roles.add((String) roleObj.get("roleCode"));
                        } else if (roleObj.containsKey("roleId") && roleObj.get("roleId") != null) {
                            roles.add((String) roleObj.get("roleId"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore if user-service is unavailable, fallback to empty roles
        }

        List<String> permissionsList = new ArrayList<>();
        try {
            var permResponse = userClient.getPermissionsByPersonId(user.getPersonId());
            if (permResponse != null && permResponse.getData() != null) {
                permissionsList.addAll(permResponse.getData());
            }
        } catch (Exception e) {
            // Ignore if user-service is unavailable
        }

        String accessToken = jwtService.generateAccessToken(
                user.getPersonId(),
                user.getUsername(),
                user.getOrgId(),
                roles,
                permissionsList
        );

        String refreshTokenString = jwtService.generateRefreshToken();
        
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .personId(user.getPersonId())
                .username(user.getUsername())
                .expiresAt(Instant.now().plusMillis(jwtRefreshExpiration))
                .deviceInfo(request.getDeviceInfo())
                .ipAddress(request.getIpAddress())
                .build();
                
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .expiresIn(jwtExpiration / 1000)
                .personId(user.getPersonId())
                .username(user.getUsername())
                .displayName(user.getUsername()) // Defaulting to username
                .organizationId(user.getOrganizationId())
                .orgId(user.getOrgId())
                .roles(roles)
                .build();
    }

    private void publishLoginFailed(String personId, String entityId, String reason, LoginRequest request) {
        try {
            auditClient.ingestAudit(Map.of(
                    "module", "AUTH",
                    "changeby", personId != null ? personId : "UNKNOWN",
                    "entityType", "AUTH_USER",
                    "entityId", entityId,
                    "action", "LOGIN_FAILED",
                    "description", reason,
                    "ipAddress", request.getIpAddress() != null ? request.getIpAddress() : "UNKNOWN",
                    "deviceInfo", request.getDeviceInfo() != null ? request.getDeviceInfo() : "UNKNOWN"
            ));
        } catch (Exception e) {
            // Ignore audit failure to not block login
        }
    }

    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        RefreshToken existingToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (existingToken.isRevoked() || existingToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        AuthUser user = authUserRepository.findByPersonId(existingToken.getPersonId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> roles = new ArrayList<>();
        List<String> permissionsList = new ArrayList<>();
        try {
            var response = userClient.getPersonByPersonId(user.getPersonId());
            if (response != null && response.getData() != null) {
                var data = response.getData();
                if (data.containsKey("roles")) {
                    List<Map<String, Object>> rolesList = (List<Map<String, Object>>) data.get("roles");
                    for (Map<String, Object> roleObj : rolesList) {
                        if (roleObj.containsKey("roleCode")) {
                            roles.add((String) roleObj.get("roleCode"));
                        }
                    }
                }
            }
            var permResponse = userClient.getPermissionsByPersonId(user.getPersonId());
            if (permResponse != null && permResponse.getData() != null) {
                permissionsList.addAll(permResponse.getData());
            }
        } catch (Exception e) {
            // fallback to empty
        }

        String accessToken = jwtService.generateAccessToken(
                user.getPersonId(),
                user.getUsername(),
                user.getOrgId(),
                roles,
                permissionsList
        );

        String newRefreshTokenString = jwtService.generateRefreshToken();
        
        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(newRefreshTokenString)
                .personId(user.getPersonId())
                .username(user.getUsername())
                .expiresAt(Instant.now().plusMillis(jwtRefreshExpiration))
                .deviceInfo(existingToken.getDeviceInfo())
                .ipAddress(existingToken.getIpAddress())
                .build();

        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);
        refreshTokenRepository.save(newRefreshToken);

        return TokenRefreshResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshTokenString)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .build();
    }

    @Transactional
    public ApiResponse<String> register(RegisterAuthUserRequest request) {
        if (authUserRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (request.getEmail() != null && authUserRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        String pId = request.getPersonId() != null ? request.getPersonId() : UUID.randomUUID().toString();

        AuthUser user = AuthUser.builder()
                .personId(pId)
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .organizationId(request.getOrganizationId())
                .orgId(request.getOrgId())
                .build();
        user.setCreatedBy("SYSTEM");

        authUserRepository.save(user);

        return ApiResponse.ok(user.getPersonId(), "User registered successfully");
    }

    @Transactional
    public ApiResponse<Void> changePassword(String personId, ChangePasswordRequest request) {
        AuthUser user = authUserRepository.findByPersonId(personId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid old password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setMustChangePassword(false);
        authUserRepository.save(user);

        return ApiResponse.ok(null, "Password changed successfully");
    }

    @Transactional
    public ApiResponse<Void> logout(String personId) {
        refreshTokenRepository.deleteByPersonId(personId);
        return ApiResponse.ok(null, "Logged out successfully");
    }
}
