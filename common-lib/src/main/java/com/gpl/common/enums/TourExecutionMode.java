package com.gpl.common.enums;

import com.gpl.common.exception.BusinessException;

/**
 * Mode d'exécution d'une tournée (Flux 2).
 *
 * <p>INTERNAL : gérée en propre par le marketeur, avec sa flotte.
 * EXTERNAL : sous-traitée à un transporteur — implique l'accusé de réception,
 * c'est-à-dire les états PENDINGTRANSPORTERACK puis ACKNOWLEDGED.</p>
 */
public enum TourExecutionMode {

    INTERNAL,
    EXTERNAL;

    /**
     * Résout un mode depuis un code client, sans distinction de casse.
     *
     * @throws BusinessException si le code ne correspond à aucun mode
     */
    public static TourExecutionMode fromCode(String code) {
        if (code != null) {
            for (TourExecutionMode mode : values()) {
                if (mode.name().equalsIgnoreCase(code.trim())) {
                    return mode;
                }
            }
        }
        throw new BusinessException(
                "Mode d'exécution inconnu : '" + code + "'. Valeurs acceptées : INTERNAL, EXTERNAL.");
    }
}
