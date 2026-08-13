package com.gpl.cylinder.model;

import com.gpl.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rfid_tags")
@Getter
@Setter
@NoArgsConstructor
public class RfidTag extends AuditableEntity {
    private String tagUid;
    private String bottleSerial;
    private String currentSiteId;
    private String currentClientSiteId;
}
