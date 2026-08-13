package com.gpl.tour.dto;

import lombok.Data;

@Data
public class UpdatePickupRequestDto {
    private Double approvedQuantity;
    private String status;
    private String statusDescription;
}
