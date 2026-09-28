package com.gpl.common.lifecycle;

import com.gpl.common.exception.BusinessException;

/**
 * Statuts d'une tournée de livraison (Flux 2).
 *
 * <p>Le nom de la constante EST le code persisté en base ; le libellé est
 * l'information affichée. C'est le pattern dual Maximo
 * (status + statusDescription), ici porté par le type lui-même : impossible
 * qu'un code et son libellé se désynchronisent.</p>
 *
 * <p>Valeurs issues de {@code csph_gpl_schema_v6_2.sql:48-51}
 * (CREATE TYPE tournee_status).</p>
 */
public enum TourneeStatus {

    DRAFT("Brouillon"),
    PLANNED("Planifiée"),
    PENDINGTRANSPORTERACK("En attente d'accusé du transporteur"),
    ACKNOWLEDGED("Accusée par le transporteur"),
    INPROGRESS("En cours d'exécution"),
    CHECKPOINTACTIVE("Arrêts en cours"),
    CLOSED("Clôturée"),
    CANCELLED("Annulée");

    private final String label;

    TourneeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Résout un statut depuis le code stocké en colonne {@code status}.
     *
     * <p>La colonne est un {@code varchar} : elle ne peut pas rejeter un code
     * inconnu, contrairement à un ENUM Postgres. Cette méthode est donc le seul
     * endroit où un code hors domaine doit échouer — et elle échoue en 422
     * (violation de règle métier), pas en 400, parce qu'un code hors domaine
     *signale un état incohérent en base, pas une requête mal formée.</p>
     *
     * @throws BusinessException si le code ne correspond à aucun statut
     */
    public static TourneeStatus fromCode(String code) {
        if (code != null) {
            String trimmed = code.trim();
            for (TourneeStatus status : values()) {
                if (status.name().equalsIgnoreCase(trimmed)) {
                    return status;
                }
            }
        }
        throw new BusinessException(
                "Statut de tournée inconnu : '" + code + "'. Valeurs acceptées : DRAFT, PLANNED, "
                        + "PENDINGTRANSPORTERACK, ACKNOWLEDGED, INPROGRESS, CHECKPOINTACTIVE, CLOSED, CANCELLED.");
    }
}
