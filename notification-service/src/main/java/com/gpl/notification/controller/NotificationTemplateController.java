package com.gpl.notification.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.notification.dto.CreateTemplateRequest;
import com.gpl.notification.dto.NotificationTemplateResponse;
import com.gpl.notification.service.TemplateService;
import com.gpl.common.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notification-templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final TemplateService templateService;

    @RequiresPermission("TEMPLATE_CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        NotificationTemplateResponse response = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Template created successfully"));
    }

    @RequiresPermission("TEMPLATE_VIEW")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationTemplateResponse>>> getAllTemplates() {
        return ResponseEntity.ok(ApiResponse.success(templateService.getAllTemplates()));
    }

    @RequiresPermission("TEMPLATE_VIEW")
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> getTemplateByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getTemplateByCode(code)));
    }
}
