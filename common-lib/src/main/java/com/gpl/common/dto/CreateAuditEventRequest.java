package com.gpl.common.dto;

import java.util.Map;

public class CreateAuditEventRequest {
    private String eventType;
    private String entityType;
    private String entityId;
    private String action;
    private String actorId;
    private Map<String, Object> details;

    public String getEventType() {
        return eventType;
    }
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    public String getEntityType() {
        return entityType;
    }
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    public String getEntityId() {
        return entityId;
    }
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public String getActorId() {
        return actorId;
    }
    public void setActorId(String actorId) {
        this.actorId = actorId;
    }
    public Map<String, Object> getDetails() {
        return details;
    }
    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
