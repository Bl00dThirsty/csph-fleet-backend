package com.gpl.tour.service;

import com.gpl.tour.dto.CreateScanEventDto;
import com.gpl.tour.dto.ScanEventResponseDto;

import java.util.List;

/**
 * Service métier pour l'enregistrement des événements de scan RFID
 * sur les arrêts de tournée (checkpoints).
 *
 * <p>Les scans alimentent la table {@code scan_events} (hypertable TimescaleDB).
 * Chaque scan représente soit une entrée / sortie RFID, soit une mesure
 * volumétrique VRAC — les deux modes s'excluent mutuellement.</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   28.09.2026
 */
public interface ScanEventService {

    /**
     * Enregistre un événement de scan RFID.
     *
     * <p>Valide qu'exactement un de {@code direction} ou {@code meterReading}
     * est fourni, que {@code direction} vaut {@code "IN"} ou {@code "OUT"},
     * et que {@code meterReading ≥ 0}.</p>
     *
     * @param dto       payload de création
     * @param createdBy identifiant (UUID) du créateur de la ligne (utilisateur courant)
     * @return DTO de réponse enrichi (id, geoLng, geoLat, createdAt, conflictStatus)
     * @throws com.gpl.common.exception.BusinessException si les règles métier sont violées
     */
    ScanEventResponseDto create(CreateScanEventDto dto, String createdBy);

    /**
     * Enregistre en lot une liste d'événements de scan RFID.
     *
     * <p>Utilisé par le PDA lors de la resynchronisation offline. Chaque item
     * est validé indépendamment — la première violation fait échouer toute la
     * transaction (rollback complet).</p>
     *
     * @param dtos      liste de payloads de création
     * @param createdBy identifiant (UUID) du créateur
     * @return liste des DTOs persistés dans le même ordre que {@code dtos}
     */
    List<ScanEventResponseDto> createBulk(List<CreateScanEventDto> dtos, String createdBy);
}