package com.gpl.audit.service;

import com.gpl.audit.dto.AuditQueryParams;
import com.gpl.audit.dto.AuditSummaryResponse;
import com.gpl.audit.dto.EntityModificationResponse;
import com.gpl.audit.dto.FieldChangeResponse;
import com.gpl.audit.dto.StatusHistoryResponse;
import com.gpl.audit.repository.AuditSummaryRepository;
import com.gpl.audit.repository.EntityModificationRepository;
import com.gpl.audit.repository.StatusHistoryRepository;
import com.gpl.common.dto.PageResponse;
import com.gpl.common.model.EntityModification;
import com.gpl.common.model.StatusHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditQueryService {

    private final EntityModificationRepository entityModificationRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final AuditSummaryRepository auditSummaryRepository;

    public PageResponse<EntityModificationResponse> getModifications(AuditQueryParams params) {
        Pageable pageable = PageRequest.of(params.getPage(), params.getSize());
        Page<EntityModification> page;

        if (params.getEntityType() != null && params.getEntityId() != null) {
            if (params.getDateFrom() != null && params.getDateTo() != null) {
                page = entityModificationRepository.findByEntityTypeAndEntityIdAndChangedateBetweenOrderByChangedateDesc(
                        params.getEntityType(), params.getEntityId(), params.getDateFrom(), params.getDateTo(), pageable);
            } else {
                page = entityModificationRepository.findByEntityTypeAndEntityIdOrderByChangedateDesc(
                        params.getEntityType(), params.getEntityId(), pageable);
            }
        } else if (params.getActorPersonId() != null) {
            page = entityModificationRepository.findByChangebyOrderByChangedateDesc(params.getActorPersonId(), pageable);
        } else if (params.getDateFrom() != null && params.getDateTo() != null) {
            page = entityModificationRepository.findByChangedateBetweenOrderByChangedateDesc(params.getDateFrom(), params.getDateTo(), pageable);
        } else if (params.getEntityType() != null && params.getAction() != null) {
            page = entityModificationRepository.findByEntityTypeAndActionOrderByChangedateDesc(params.getEntityType(), params.getAction(), pageable);
        } else {
            page = entityModificationRepository.findAll(pageable);
        }

        return PageResponse.of(page.map(this::mapToModificationResponse));
    }

    public PageResponse<StatusHistoryResponse> getStatusHistory(AuditQueryParams params) {
        Pageable pageable = PageRequest.of(params.getPage(), params.getSize());
        Page<StatusHistory> page;

        if (params.getEntityType() != null && params.getEntityId() != null) {
            page = statusHistoryRepository.findByEntityTypeAndEntityIdOrderByChangedateDesc(
                    params.getEntityType(), params.getEntityId(), pageable);
        } else if (params.getActorPersonId() != null) {
            page = statusHistoryRepository.findByChangebyOrderByChangedateDesc(params.getActorPersonId(), pageable);
        } else if (params.getDateFrom() != null && params.getDateTo() != null) {
            page = statusHistoryRepository.findByChangedateBetweenOrderByChangedateDesc(params.getDateFrom(), params.getDateTo(), pageable);
        } else {
            page = statusHistoryRepository.findAll(pageable);
        }

        return PageResponse.of(page.map(this::mapToStatusHistoryResponse));
    }

    public AuditSummaryResponse getSummary(String entityType, String entityId) {
        return auditSummaryRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .map(summary -> AuditSummaryResponse.builder()
                        .id(summary.getId())
                        .entityType(summary.getEntityType())
                        .entityId(summary.getEntityId())
                        .entityName(summary.getEntityName())
                        .totalModifications(summary.getTotalModifications())
                        .lastModifiedAt(summary.getLastModifiedAt())
                        .lastModifiedBy(summary.getLastModifiedBy())
                        .lastModifiedByDisplayName(summary.getLastModifiedByDisplayName())
                        .lastAction(summary.getLastAction())
                        .lastActionDescription(summary.getLastActionDescription())
                        .firstCreatedAt(summary.getFirstCreatedAt())
                        .firstCreatedBy(summary.getFirstCreatedBy())
                        .firstCreatedByDisplayName(summary.getFirstCreatedByDisplayName())
                        .build())
                .orElse(null);
    }

    private EntityModificationResponse mapToModificationResponse(EntityModification mod) {
        List<FieldChangeResponse> changes = mod.getChanges() != null ? mod.getChanges().stream()
                .map(fc -> FieldChangeResponse.builder()
                        .fieldName(fc.getFieldName())
                        .fieldLabel(fc.getFieldLabel())
                        .oldValue(fc.getOldValue())
                        .oldValueDescription(fc.getOldValueDescription())
                        .newValue(fc.getNewValue())
                        .newValueDescription(fc.getNewValueDescription())
                        .valueType(fc.getValueType())
                        .build())
                .collect(Collectors.toList()) : null;

        return EntityModificationResponse.builder()
                .modificationId(mod.getId())
                .entityType(mod.getEntityType())
                .entityId(mod.getEntityId())
                .entityName(mod.getEntityName())
                .action(mod.getAction())
                .actionDescription(mod.getActionDescription())
                .module(mod.getModule())
                .description(mod.getDescription())
                .changeby(mod.getChangeby())
                .changebyDisplayName(mod.getChangebyDisplayName())
                .actorOrgId(mod.getActorOrgId())
                .actorSiteId(mod.getActorSiteId())
                .changedate(mod.getChangedate())
                .deviceInfo(mod.getDeviceInfo())
                .appVersion(mod.getAppVersion())
                .changes(changes)
                .build();
    }

    private StatusHistoryResponse mapToStatusHistoryResponse(StatusHistory sh) {
        return StatusHistoryResponse.builder()
                .id(sh.getId())
                .entityType(sh.getEntityType())
                .entityId(sh.getEntityId())
                .entityName(sh.getEntityName())
                .previousStatus(sh.getPreviousStatus())
                .previousStatusDescription(sh.getPreviousStatusDescription())
                .newStatus(sh.getNewStatus())
                .newStatusDescription(sh.getNewStatusDescription())
                .changeby(sh.getChangeby())
                .changebyDisplayName(sh.getChangebyDisplayName())
                .changedate(sh.getChangedate())
                .reason(sh.getReason())
                .isAutomatic(sh.getIsAutomatic())
                .build();
    }
}