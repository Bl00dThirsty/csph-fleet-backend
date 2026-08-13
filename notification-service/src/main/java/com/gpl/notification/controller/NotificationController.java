package com.gpl.notification.controller;

import com.gpl.common.dto.ApiResponse;
import com.gpl.notification.dto.NotificationResponse;
import com.gpl.notification.dto.SendNotificationRequest;
import com.gpl.notification.service.NotificationService;
import com.gpl.common.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @RequiresPermission("NOTIFICATION_SEND")
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
