package com.gpl.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tier hiérarchique des organisations.
 * Contrôle la visibilité des données et les permissions systémiques.
 *
 * TIER_1 (CSPH) voit tout le système.
 * TIER_2 (Dépôts SNH/SCDP) voit ses approvisionnements.
 * TIER_3 (Marqueteurs/Transporteurs) voit ses propres opérations.
 * TIER_4 (Clients) voit uniquement ses livraisons.
 * TIER_SUPPORT (Intégrateur/Plateforme) voit selon ses permissions.
 */
@Getter
@RequiredArgsConstructor
public enum OrganizationTier {

    TIER_1_GOVERNANCE(1, "T1", "Gouvernance", true, false),
    TIER_2_INFRASTRUCTURE(2, "T2", "Infrastructure", false, true),
    TIER_3_OPERATIONS(3, "T3", "Opérations", false, true),
    TIER_4_CONSUMPTION(4, "T4", "Consommation", false, false),
    TIER_SUPPORT(0, "TS", "Support", true, true);

    private final int level;
    private final String code;
    private final String description;
    private final boolean canSeeAllData;
    private final boolean canManageUsers;

    public static OrganizationTier fromCode(String code) {
        for (OrganizationTier tier : values()) {
            if (tier.code.equalsIgnoreCase(code)) return tier;
        }
        throw new IllegalArgumentException("Unknown OrganizationTier code: " + code);
    }
}
