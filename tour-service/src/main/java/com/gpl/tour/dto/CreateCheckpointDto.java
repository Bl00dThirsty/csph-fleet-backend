package com.gpl.tour.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.Instant;

/**
 * DTO pour la création d'un arrêt de tournée (checkpoint).
 *
 * <p>Le champ {@code tourId} est injecté automatiquement depuis la variable de chemin URL
 * par le contrôleur ({@code @PathVariable}), il ne doit pas être fourni dans le body
 * et ne porte donc aucune contrainte de validation {@code @NotBlank}.</p>
 *
 * <p>Contrainte métier : {@code siteId} et {@code clientSiteId} sont mutuellement exclusifs.
 * Un et un seul doit être renseigné. Cette contrainte est validée dans le service.</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   04.08.2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCheckpointDto {

    /**
     * Identifiant de la tournée parente.
     * Injecté depuis le path variable — ne pas renseigner dans le body.
     */
    private String tourId;

    /**
     * Identifiant du site opérationnel (dépôt, entrepôt).
     * Exclusif avec {@code clientSiteId}.
     */
    private String siteId;

    /**
     * Identifiant du site client (emplisseur, revendeur, industriel).
     * Exclusif avec {@code siteId}.
     */
    private String clientSiteId;

    /**
     * Numéro d'ordre de cet arrêt dans le circuit (commence à 1).
     * Doit être ≥ 1.
     */
    @Builder.Default
    @Min(value = 1, message = "La séquence doit être supérieure ou égale à 1")
    private int sequence = 1;

    /** Heure de passage prévue (ISO-8601, ex: 2026-08-12T08:00:00Z). */
    private Instant expectedArrival;

    /** Motif de saut — à renseigner uniquement si l'arrêt est annulé. */
    private String skipReason;
}
