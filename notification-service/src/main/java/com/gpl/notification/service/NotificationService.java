package com.gpl.notification.service;

import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.notification.dto.NotificationResponse;
import com.gpl.notification.dto.SendNotificationRequest;
import com.gpl.notification.model.NotificationLog;
import com.gpl.notification.model.NotificationTemplate;
import com.gpl.notification.repository.NotificationLogRepository;
import com.gpl.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationLogRepository logRepository;
    private final NotificationTemplateRepository templateRepository;
    private final EmailService emailService;

    @Transactional
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        NotificationTemplate template = templateRepository.findByCode(request.getTemplateCode())
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", "code", request.getTemplateCode()));

        String body = resolvePlaceholders(template.getBodyTemplate(), request.getVariables());
        String subject = resolvePlaceholders(template.getSubject(), request.getVariables());

        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setTemplateCode(template.getCode());
        notificationLog.setRecipientPersonId(request.getRecipientPersonId());
        notificationLog.setRecipientEmail(request.getRecipientEmail());
        notificationLog.setChannel(request.getChannel() != null ? request.getChannel() : template.getChannel());
        notificationLog.setSubject(subject);
        notificationLog.setBody(body);
        notificationLog.setRelatedEntityType(request.getRelatedEntityType());
        notificationLog.setRelatedEntityId(request.getRelatedEntityId());
        notificationLog.setStatus("PENDING");

        notificationLog = logRepository.save(notificationLog);

        try {
            if ("EMAIL".equalsIgnoreCase(notificationLog.getChannel()) && notificationLog.getRecipientEmail() != null) {
                emailService.sendEmail(notificationLog.getRecipientEmail(), subject, body);
            } else {
                log.warn("Channel {} not supported or missing email for {}", notificationLog.getChannel(), notificationLog.getRecipientPersonId());
            }

            notificationLog.setStatus("SENT");
            notificationLog.setSentAt(Instant.now());
        } catch (Exception e) {
            notificationLog.setStatus("FAILED");
            notificationLog.setErrorMessage(e.getMessage());
        }

        notificationLog = logRepository.save(notificationLog);

        return new NotificationResponse(
                notificationLog.getId(),
                notificationLog.getTemplateCode(),
                notificationLog.getRecipientPersonId(),
                notificationLog.getChannel(),
                notificationLog.getSubject(),
                notificationLog.getStatus(),
                notificationLog.getStatusDescription(),
                notificationLog.getSentAt()
        );
    }

    private String resolvePlaceholders(String template, Map<String, String> variables) {
        if (template == null) return null;
        if (variables == null || variables.isEmpty()) return template;

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
