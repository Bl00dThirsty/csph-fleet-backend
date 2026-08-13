package com.gpl.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types d'organisation dans la chaîne GPL.
 * Chaque type correspond à un rôle métier identifié dans la Matrice des Acteurs.
 */
@Getter
@RequiredArgsConstructor
public enum OrganizationType {

    REGULATOR("REG", "Régulateur"),
    DEPOT("DEP", "Dépôt / Centre emplisseur"),
    MARKETER("MKT", "Marqueteur / Distributeur"),
    TRANSPORTER("TRP", "Transporteur"),
    CLIENT("CLT", "Client"),
    INTEGRATOR("INT", "Intégrateur technique"),
    PLATFORM_OPERATOR("PLT", "Opérateur de plateforme");

    private final String code;
    private final String description;

    public static OrganizationType fromCode(String code) {
        for (OrganizationType type : values()) {
            if (type.code.equalsIgnoreCase(code)) return type;
        }
        throw new IllegalArgumentException("Unknown OrganizationType code: " + code);
    }
}
