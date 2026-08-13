package com.gpl.cylinder.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRfidTagRequest {
    private String tagUid;
    private String bottleSerial;
    private String currentSiteId;
    private String currentClientSiteId;
    private String status;
    private String statusDescription;
}
