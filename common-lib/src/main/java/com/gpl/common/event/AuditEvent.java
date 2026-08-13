package com.gpl.common.event;

import com.gpl.common.enums.AuditAction;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Événement d'audit publié sur le message broker par tous les microservices.
 * Le service d'audit consomme ces événements et crée les EntityModification
 * et FieldChange correspondants.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    /** Type de l'entité modifiée. */
    private String entityType;

    /** ID de l'entité modifiée. */
    private String entityId;

    /** Nom lisible de l'entité. */
    private String entityName;

    /** Action effectuée. */
    private String action;
    private String actionDescription;

    /** Module applicatif. */
    private String module;

    /** Description humaine. */
    private String description;

    /** PersonId de l'auteur. */
    private String changeby;
    private String changebyDisplayName;
    private String actorOrgId;
    private String actorSiteId;

    /** Timestamp. */
    private Instant changedate;

    /** Détails des champs modifiés : fieldName → {oldValue, newValue}. */
    private List<FieldChangeEvent> changes;

    /** Contexte technique. */
    private String deviceInfo;
    private String appVersion;
    private String ipAddress;
    private Double latitude;
    private Double longitude;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldChangeEvent {
        private String fieldName;
        private String fieldLabel;
        private String oldValue;
        private String oldValueDescription;
        private String newValue;
        private String newValueDescription;
        private String valueType;
    }
}
