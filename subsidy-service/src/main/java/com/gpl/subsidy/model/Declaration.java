package com.gpl.subsidy.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "declarations")
public class Declaration extends AuditableEntity {
    private String marketerOrganizationId;
    private Instant periodStart;
    private Instant periodEnd;
    private double declaredVolume;
    private String submittedByPersonId;

    public String getMarketerOrganizationId() { return marketerOrganizationId; }
    public void setMarketerOrganizationId(String marketerOrganizationId) { this.marketerOrganizationId = marketerOrganizationId; }
    public Instant getPeriodStart() { return periodStart; }
    public void setPeriodStart(Instant periodStart) { this.periodStart = periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Instant periodEnd) { this.periodEnd = periodEnd; }
    public double getDeclaredVolume() { return declaredVolume; }
    public void setDeclaredVolume(double declaredVolume) { this.declaredVolume = declaredVolume; }
    public String getSubmittedByPersonId() { return submittedByPersonId; }
    public void setSubmittedByPersonId(String submittedByPersonId) { this.submittedByPersonId = submittedByPersonId; }
}
