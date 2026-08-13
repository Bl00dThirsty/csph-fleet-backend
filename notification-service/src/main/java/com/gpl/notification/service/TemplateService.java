package com.gpl.notification.service;

import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.notification.dto.CreateTemplateRequest;
import com.gpl.notification.dto.NotificationTemplateResponse;
import com.gpl.notification.model.NotificationTemplate;
import com.gpl.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final NotificationTemplateRepository repository;

    @Transactional
    public NotificationTemplateResponse createTemplate(CreateTemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate();
        template.setCode(request.getCode());
        template.setName(request.getName());
        template.setSubject(request.getSubject());
        template.setBodyTemplate(request.getBodyTemplate());
        template.setModule(request.getModule());
        template.setChannel(request.getChannel());
        template.setLanguage(request.getLanguage());
        template.setIsActive(true);
        template.setSortOrder(0);

        NotificationTemplate saved = repository.save(template);
        return mapToResponse(saved);
    }

    public List<NotificationTemplateResponse> getAllTemplates() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public NotificationTemplateResponse getTemplateByCode(String code) {
        return repository.findByCode(code)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", "code", code));
    }

    private NotificationTemplateResponse mapToResponse(NotificationTemplate template) {
        return new NotificationTemplateResponse(
                template.getId(),
                template.getCode(),
                template.getName(),
                template.getSubject(),
                template.getBodyTemplate(),
                template.getModule(),
                template.getChannel(),
                template.getLanguage(),
                template.getIsActive(),
                template.getSortOrder()
        );
    }
}
