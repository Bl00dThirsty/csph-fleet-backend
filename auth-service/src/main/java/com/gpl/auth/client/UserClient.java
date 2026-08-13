package com.gpl.auth.client;
import com.gpl.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;
import java.util.Set;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/api/v1/persons/by-person-id/{personId}")
    ApiResponse<Map<String, Object>> getPersonByPersonId(@PathVariable("personId") String personId);

    @GetMapping("/api/v1/persons/by-person-id/{personId}/permissions")
    ApiResponse<Set<String>> getPermissionsByPersonId(@PathVariable("personId") String personId);
}
