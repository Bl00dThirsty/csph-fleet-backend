package com.gpl.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types de relations commerciales inter-organisations.
 */
@Getter
@RequiredArgsConstructor
public enum RelationshipType {

    MANDATE("MANDATE", "Mandatement (transport)"),
    CONTRACT("CONTRACT", "Contrat commercial (client)"),
    SUPPLY("SUPPLY", "Approvisionnement (dépôt → marqueteur)"),
    SUBSIDIARY("SUBSIDIARY", "Filiale"),
    FRANCHISE("FRANCHISE", "Franchise"),
    SUPERVISION("SUPERVISION", "Supervision réglementaire");

    private final String code;
    private final String description;

    public static RelationshipType fromCode(String code) {
        for (RelationshipType type : values()) {
            if (type.code.equalsIgnoreCase(code)) return type;
        }
        throw new IllegalArgumentException("Unknown RelationshipType code: " + code);
    }
}
