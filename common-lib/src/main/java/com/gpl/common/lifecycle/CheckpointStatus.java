package com.gpl.common.lifecycle;

import com.gpl.common.exception.BusinessException;

/**
 * Statuts d'un arrêt de tournée.
 *
 * <p>REACHED (arrivée capturée, actual_arrival renseigné) et COMPLETED
 * (livraison terminée) restent deux faits distincts : la timeline d'anomalies
 * en dépend. Les confondre perd l'heure d'arrivée, qui est la preuve.</p>
 *
 * <p>Valeurs issues de {@code csph_gpl_schema_v6_2.sql:52}
 * (CREATE TYPE checkpoint_status).</p>
 */
public enum CheckpointStatus {

    PENDING("En attente de visite"),
    REACHED("Arrivé sur site"),
    COMPLETED("Livraison terminée"),
    SKIPPED("Arrêt sauté");

    private final String label;

    CheckpointStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Résout un statut depuis le code stocké en colonne {@code status}.
     *
     * @throws BusinessException si le code ne correspond à aucun statut
     * @see TourneeStatus#fromCode(String) pour la justification du 422
     */
    public static CheckpointStatus fromCode(String code) {
        if (code != null) {
            String trimmed = code.trim();
            for (CheckpointStatus status : values()) {
                if (status.name().equalsIgnoreCase(trimmed)) {
                    return status;
                }
            }
        }
        throw new BusinessException(
                "Statut d'arrêt inconnu : '" + code
                        + "'. Valeurs acceptées : PENDING, REACHED, COMPLETED, SKIPPED.");
    }
}
