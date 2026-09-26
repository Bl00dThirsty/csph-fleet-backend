package com.gpl.auth.controller;

import com.gpl.auth.dto.ChangePasswordRequest;
import com.gpl.auth.dto.LoginRequest;
import com.gpl.auth.dto.LoginResponse;
import com.gpl.auth.dto.RegisterAuthUserRequest;
import com.gpl.auth.dto.TokenRefreshRequest;
import com.gpl.auth.dto.TokenRefreshResponse;
import com.gpl.common.dto.ApiResponse;
import com.gpl.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterAuthUserRequest request) {
        ApiResponse<String> response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("X-User-PersonId") String personId) {
        ApiResponse<Void> response = authService.logout(personId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("X-User-PersonId") String personId,
            @Valid @RequestBody ChangePasswordRequest request) {
        ApiResponse<Void> response = authService.changePassword(personId, request);
        return ResponseEntity.ok(response);
    }
}
