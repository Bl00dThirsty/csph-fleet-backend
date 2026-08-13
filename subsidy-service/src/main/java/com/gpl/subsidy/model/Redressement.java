package com.gpl.subsidy.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "redressements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Redressement extends AuditableEntity {

    @Column(name = "reconciliation_id", nullable = false)
    private String reconciliationId;

    @Column(name = "amount")
    private double amount;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "transaction_ref")
    private String transactionRef;
}
