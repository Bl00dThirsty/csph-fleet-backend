package com.gpl.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldChangeResponse {
    private String fieldName;
    private String fieldLabel;
    private String oldValue;
    private String oldValueDescription;
    private String newValue;
    private String newValueDescription;
    private String valueType;
}
