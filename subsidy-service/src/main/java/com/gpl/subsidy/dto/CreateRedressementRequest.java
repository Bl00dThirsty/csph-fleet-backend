package com.gpl.subsidy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRedressementRequest {
    @NotBlank(message = "reconciliationId is required")
    private String reconciliationId;
    private Double amount;
    @NotNull(message = "dueDate is required")
    private Instant dueDate;
    private String transactionRef;
}
