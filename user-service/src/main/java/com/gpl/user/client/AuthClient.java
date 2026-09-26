package com.gpl.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for communicating with the auth-service.
 * Used to provision authentication credentials when creating users
 * who need login access (e.g., drivers).
 */
@FeignClient(name = "auth-service", path = "/api/v1/auth")
public interface AuthClient {

    /**
     * Registers a new authentication user in the auth-service.
     *
     * @param request map containing personId, username, email, password, organizationId, orgId
     * @return the auth-service response
     */
    @PostMapping("/register")
    Map<String, Object> register(@RequestBody Map<String, String> request);
}
