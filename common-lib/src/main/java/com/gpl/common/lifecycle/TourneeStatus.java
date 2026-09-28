package com.gpl.common.lifecycle;

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
}
