package com.gpl.cylinder.dto;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfidTagResponse {
    private String id;
    private Long rowStamp;
    private String tagUid;
    private String bottleSerial;
    private String currentSiteId;
    private String currentClientSiteId;
    private String status;
    private String statusDescription;
    private Instant statusDate;
    private Instant createdAt;
    private String createdBy;
    private Instant changedate;
    private String changeby;
}
