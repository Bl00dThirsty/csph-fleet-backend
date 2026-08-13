package com.gpl.subsidy.dto;

import com.gpl.subsidy.model.Redressement;
import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedressementResponse {
    private String id;
    private String reconciliationId;
    private double amount;
    private String status;
    private String statusDescription;
    private Instant issuedAt;
    private Instant dueDate;
    private Instant paidAt;
    private String transactionRef;
    private Instant createdAt;
    private String createdBy;
    private Instant changedate;
    private String changeby;

    public static RedressementResponse fromEntity(Redressement entity) {
        if (entity == null) return null;
        return RedressementResponse.builder()
                .id(entity.getId())
                .reconciliationId(entity.getReconciliationId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .statusDescription(entity.getStatusDescription())
                .issuedAt(entity.getIssuedAt())
                .dueDate(entity.getDueDate())
                .paidAt(entity.getPaidAt())
                .transactionRef(entity.getTransactionRef())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .changedate(entity.getChangedate())
                .changeby(entity.getChangeby())
                .build();
    }
}
