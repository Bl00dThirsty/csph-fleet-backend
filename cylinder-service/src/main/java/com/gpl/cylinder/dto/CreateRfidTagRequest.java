package com.gpl.cylinder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRfidTagRequest {
    @NotBlank(message = "Le tag UID est obligatoire")
    private String tagUid;
    private String bottleSerial;
    private String currentSiteId;
    private String currentClientSiteId;
    private String status;
    private String statusDescription;
}
