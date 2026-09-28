package com.gpl.tour.service.impl;

import com.gpl.common.enums.ScanDirection;
import com.gpl.common.exception.BusinessException;
import com.gpl.tour.dto.CreateScanEventDto;
import com.gpl.tour.dto.ScanEventResponseDto;
import com.gpl.tour.model.ScanEvent;
import com.gpl.tour.repository.ScanEventRepository;
import com.gpl.tour.service.ScanEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implémentation de {@link ScanEventService}.
 *
 * <p>Persistance d'événements de scan RFID sur les arrêts de tournée.</p>
 *
 * <p>Règles métier appliquées :</p>
 * <ul>
 *   <li>Exactement un de {@code direction} ou {@code meterReading} doit être renseigné (XOR).</li>
 *   <li>{@code direction} doit valoir {@code IN} ou {@code OUT} (cf. enum SQL {@code scan_direction}).</li>
 *   <li>{@code meterReading}, s'il est fourni, doit être ≥ 0 (cf. CHECK constraint en base).</li>
 * </ul>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   28.09.2026
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ScanEventServiceImpl implements ScanEventService {

    private final ScanEventRepository scanEventRepository;

    @Override
    public ScanEventResponseDto create(CreateScanEventDto dto, String createdBy) {
        log.info("Creating ScanEvent — checkpoint={}, livreur={}, direction={}, meterReading={}",
                dto.getCheckpointId(), dto.getLivreurUserId(), dto.getDirection(), dto.getMeterReading());

        validatePayload(dto);

        // Idempotency: a PDA re-sends the same read (same pdaSyncId) while the
        // server has not confirmed it. Returning the stored row keeps one
        // physical tag at exactly one event no matter how often it is
        // re-uploaded. The UNIQUE constraint on pda_sync_id is the backstop
        // against raced duplicates.
        if (dto.getPdaSyncId() != null && !dto.getPdaSyncId().isBlank()) {
            var existing = scanEventRepository.findByPdaSyncId(dto.getPdaSyncId());
            if (existing.isPresent()) {
                log.info("ScanEvent already stored for pdaSyncId={} — returning existing id={}",
                        dto.getPdaSyncId(), existing.get().getId());
                return toResponseDto(existing.get());
            }
        }

        ScanEvent entity = ScanEvent.builder()
                .id(UUID.randomUUID())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now())
                .checkpointId(dto.getCheckpointId())
                .livreurUserId(dto.getLivreurUserId())
                .rfidTagId(dto.getRfidTagId())
                .direction(parseDirection(dto.getDirection()))
                .geoLng(requireCoordinate(dto.getGeoLng(), "geoLng"))
                .geoLat(requireCoordinate(dto.getGeoLat(), "geoLat"))
                .meterReading(dto.getMeterReading())
                .photoUrl(dto.getPhotoUrl())
                .pdaSyncId(dto.getPdaSyncId())
                .createdAt(Instant.now())
                .createdBy(parseUuid(createdBy))
                .build();

        ScanEvent saved = scanEventRepository.save(entity);
        log.debug("ScanEvent persisted with id={}, timestamp={}", saved.getId(), saved.getTimestamp());

        return toResponseDto(saved);
    }

    @Override
    public List<ScanEventResponseDto> createBulk(List<CreateScanEventDto> dtos, String createdBy) {
        log.info("Bulk-creating {} ScanEvents for user={}", dtos != null ? dtos.size() : 0, createdBy);
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        List<ScanEventResponseDto> responses = new ArrayList<>(dtos.size());
        for (CreateScanEventDto dto : dtos) {
            responses.add(create(dto, createdBy));
        }
        return responses;
    }

    /* ── Validation helpers ─────────────────────────────────────────────── */

    private void validatePayload(CreateScanEventDto dto) {
        boolean hasDirection = dto.getDirection() != null && !dto.getDirection().isBlank();
        boolean hasMeter = dto.getMeterReading() != null;

        // XOR — exactly one of (direction, meterReading) must be supplied
        if (hasDirection == hasMeter) {
            throw new BusinessException(
                    "Un événement de scan doit préciser exactement un mode parmi 'direction' (RFID IN/OUT) ou 'meterReading' (VRAC volumétrique). Les deux sont renseignés ou aucun des deux."
            );
        }
        if (hasDirection) {
            try {
                ScanDirection.valueOf(dto.getDirection());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(
                        "La direction '" + dto.getDirection() + "' est invalide. Valeurs acceptées : IN, OUT."
                );
            }
        }
        if (hasMeter && dto.getMeterReading() < 0) {
            throw new BusinessException("L'index compteur (meterReading) doit être ≥ 0.");
        }
    }

    /* ── Mapping helpers ────────────────────────────────────────────────── */

    private Double requireCoordinate(Double value, String name) {
        if (value == null) {
            throw new BusinessException("Les coordonnées GPS (geoLng, geoLat) sont obligatoires pour un scan.");
        }
        return value;
    }

    private ScanDirection parseDirection(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ScanDirection.valueOf(raw);
    }

    /**
     * Best-effort UUID parsing — the {@code createdBy} header is the X-User-Username
     * (an opaque identifier); when the controller hands us a non-UUID string we
     * persist {@code null} and the audit row stays anonymous.
     */
    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ScanEventResponseDto toResponseDto(ScanEvent e) {
        if (e == null) {
            return null;
        }
        return ScanEventResponseDto.builder()
                .id(e.getId())
                .checkpointId(e.getCheckpointId())
                .livreurUserId(e.getLivreurUserId())
                .rfidTagId(e.getRfidTagId())
                .direction(e.getDirection() != null ? e.getDirection().name() : null)
                .geoLng(e.getGeoLng())
                .geoLat(e.getGeoLat())
                .meterReading(e.getMeterReading())
                .photoUrl(e.getPhotoUrl())
                .pdaSyncId(e.getPdaSyncId())
                .timestamp(e.getTimestamp())
                .createdAt(e.getCreatedAt())
                .conflictStatus(e.getConflictStatus())
                .build();
    }
}