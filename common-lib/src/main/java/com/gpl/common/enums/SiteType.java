package com.gpl.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types de sites physiques.
 * Inspiré du pattern Maximo Location type (OPERATING, HOLDING, etc.)
 */
@Getter
@RequiredArgsConstructor
public enum SiteType {

    DEPOT("DEP", "Dépôt de stockage"),
    FILLING_CENTER("FIL", "Centre emplisseur"),
    WAREHOUSE("WHS", "Entrepôt"),
    OFFICE("OFC", "Bureau / Siège"),
    CLIENT_SITE("CST", "Site client"),
    TRANSIT_POINT("TRP", "Point de transit");

    private final String code;
    private final String description;

    public static SiteType fromCode(String code) {
        for (SiteType type : values()) {
            if (type.code.equalsIgnoreCase(code)) return type;
        }
        throw new IllegalArgumentException("Unknown SiteType code: " + code);
    }
}
