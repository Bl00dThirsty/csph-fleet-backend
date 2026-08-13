package com.gpl.audit.service;

import com.gpl.audit.model.AuditSummary;
import com.gpl.audit.repository.AuditSummaryRepository;
import com.gpl.audit.repository.EntityModificationRepository;
import com.gpl.audit.repository.StatusHistoryRepository;
import com.gpl.common.event.AuditEvent;
import com.gpl.common.model.EntityModification;
import com.gpl.common.model.FieldChange;
import com.gpl.common.model.StatusHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditIngestionService {

    private final EntityModificationRepository entityModificationRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final AuditSummaryRepository auditSummaryRepository;

    @Transactional
    public void processAuditEvent(AuditEvent event) {
        log.info("Processing audit event for entity: {}/{}", event.getEntityType(), event.getEntityId());

        EntityModification mod = EntityModification.builder()
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .entityName(event.getEntityName())
                .action(event.getAction())
                .actionDescription(event.getActionDescription())
                .module(event.getModule())
                .description(event.getDescription())
                .changeby(event.getChangeby())
                .changebyDisplayName(event.getChangebyDisplayName())
                .actorOrgId(event.getActorOrgId())
                .actorSiteId(event.getActorSiteId())
                .changedate(event.getChangedate() != null ? event.getChangedate() : Instant.now())
                .deviceInfo(event.getDeviceInfo())
                .appVersion(event.getAppVersion())
                .ipAddress(event.getIpAddress())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .changes(new ArrayList<>())
                .build();

        if (event.getChanges() != null) {
            for (int i = 0; i < event.getChanges().size(); i++) {
                AuditEvent.FieldChangeEvent fc = event.getChanges().get(i);
                FieldChange change = FieldChange.builder()
                        .modification(mod)
                        .fieldName(fc.getFieldName())
                        .fieldLabel(fc.getFieldLabel())
                        .oldValue(fc.getOldValue())
                        .oldValueDescription(fc.getOldValueDescription())
                        .newValue(fc.getNewValue())
                        .newValueDescription(fc.getNewValueDescription())
                        .valueType(fc.getValueType() != null ? fc.getValueType() : "STRING")
                        .sortOrder(i)
                        .build();
                mod.getChanges().add(change);
            }
        }

        entityModificationRepository.save(mod);
        updateAuditSummary(mod);

        if ("STATUS_CHANGE".equalsIgnoreCase(event.getAction()) || "STC".equalsIgnoreCase(event.getAction())) {
            StatusHistory sh = StatusHistory.builder()
                    .entityType(event.getEntityType())
                    .entityId(event.getEntityId())
                    .entityName(event.getEntityName())
                    .changeby(event.getChangeby())
                    .changebyDisplayName(event.getChangebyDisplayName())
                    .changedate(event.getChangedate() != null ? event.getChangedate() : Instant.now())
                    .reason(event.getDescription())
                    .build();
            statusHistoryRepository.save(sh);
        }
    }

    private void updateAuditSummary(EntityModification mod) {
        Optional<AuditSummary> existingSummary = auditSummaryRepository.findByEntityTypeAndEntityId(mod.getEntityType(), mod.getEntityId());

        AuditSummary summary;
        if (existingSummary.isPresent()) {
            summary = existingSummary.get();
            summary.setTotalModifications(summary.getTotalModifications() + 1);
            summary.setLastModifiedAt(mod.getChangedate());
            summary.setLastModifiedBy(mod.getChangeby());
            summary.setLastModifiedByDisplayName(mod.getChangebyDisplayName());
            summary.setLastAction(mod.getAction());
            summary.setLastActionDescription(mod.getActionDescription());
            
            if (mod.getEntityName() != null) {
                summary.setEntityName(mod.getEntityName());
            }
        } else {
            summary = AuditSummary.builder()
                    .entityType(mod.getEntityType())
                    .entityId(mod.getEntityId())
                    .entityName(mod.getEntityName())
                    .totalModifications(1)
                    .lastModifiedAt(mod.getChangedate())
                    .lastModifiedBy(mod.getChangeby())
                    .lastModifiedByDisplayName(mod.getChangebyDisplayName())
                    .lastAction(mod.getAction())
                    .lastActionDescription(mod.getActionDescription())
                    .firstCreatedAt(mod.getChangedate())
                    .firstCreatedBy(mod.getChangeby())
                    .firstCreatedByDisplayName(mod.getChangebyDisplayName())
                    .build();
        }

        auditSummaryRepository.save(summary);
    }
}
