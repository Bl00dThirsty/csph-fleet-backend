package com.gpl.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO d'entrée pour la soumission en lot d'événements de scan RFID.
 *
 * <p>Utilisé lors de la resynchronisation offline d'un PDA : tous les scans
 * accumulés hors connexion sont poussés en une seule requête HTTP.</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   28.09.2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkScanEventsDto {

    /** Liste des scans à insérer dans la même transaction. */
    private List<CreateScanEventDto> items;
}