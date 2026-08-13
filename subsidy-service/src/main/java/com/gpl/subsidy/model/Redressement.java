package com.gpl.subsidy.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "redressements")
public class Redressement extends AuditableEntity {
    private String reconciliationId;
    private double amount;
    private Instant issuedAt;
    private Instant dueDate;
    private Instant paidAt;
    private String transactionRef;

    public String getReconciliationId() { return reconciliationId; }
    public void setReconciliationId(String reconciliationId) { this.reconciliationId = reconciliationId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
}
