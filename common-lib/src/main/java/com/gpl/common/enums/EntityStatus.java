package com.gpl.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut générique des entités.
 * Pattern Maximo : chaque enum porte un code machine ET une description lisible.
 */
@Getter
@RequiredArgsConstructor
public enum EntityStatus {

    ACTIVE("ACTIVE", "Actif"),
    INACTIVE("INACTIVE", "Inactif"),
    SUSPENDED("SUSPENDED", "Suspendu"),
    PENDING_APPROVAL("PENDAPPR", "En attente d'approbation"),
    ARCHIVED("ARCHIVED", "Archivé"),
    OPERATING("OPERATING", "En service"),
    DECOMMISSIONED("DECOMMISSIONED", "Déclassé");

    private final String code;
    private final String description;

    public static EntityStatus fromCode(String code) {
        for (EntityStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) return status;
        }
        throw new IllegalArgumentException("Unknown EntityStatus code: " + code);
    }
}
