package com.gpl.tour.service.impl;

import com.gpl.common.enums.ScanDirection;
import com.gpl.common.exception.BusinessException;
import com.gpl.tour.dto.CreateScanEventDto;
import com.gpl.tour.dto.ScanEventResponseDto;
import com.gpl.tour.model.ScanEvent;
import com.gpl.tour.repository.ScanEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link ScanEventServiceImpl}. No Spring context, no live Postgres.
 *
 * <p>The {@code @Transactional} annotation on the service is metadata-only here because
 * the bean is instantiated directly with {@link InjectMocks}, bypassing the AOP proxy.</p>
 *
 * @author  GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since   28.09.2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScanEventServiceImpl")
class ScanEventServiceImplTest {

    @Mock
    private ScanEventRepository scanEventRepository;

    @InjectMocks
    private ScanEventServiceImpl scanEventService;

    @Test
    @DisplayName("create() persists a row whose checkpointId, livreurUserId and direction match the DTO")
    void create_persistsRowWithCheckpointAndLivreur() {
        // Given — a valid IN scan payload and a repository that echoes save() calls
        UUID checkpointId = UUID.randomUUID();
        UUID livreurId = UUID.randomUUID();
        CreateScanEventDto dto = CreateScanEventDto.builder()
                .checkpointId(checkpointId)
                .livreurUserId(livreurId)
                .direction("IN")
                .geoLng(11.5)
                .geoLat(3.8)
                .photoUrl("https://photos.example/abc.jpg")
                .pdaSyncId("PDA-001")
                .build();

        when(scanEventRepository.save(any(ScanEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When — the livreur triggers a single scan
        ScanEventResponseDto response = scanEventService.create(dto, livreurId.toString());

        // Then — a row is persisted and the response echoes the key fields
        ArgumentCaptor<ScanEvent> captor = ArgumentCaptor.forClass(ScanEvent.class);
        verify(scanEventRepository).save(captor.capture());
        ScanEvent persisted = captor.getValue();

        assertEquals(checkpointId, persisted.getCheckpointId(),
                "persisted checkpointId must match the DTO");
        assertEquals(livreurId, persisted.getLivreurUserId(),
                "persisted livreurUserId must match the DTO");
        assertEquals(ScanDirection.IN, persisted.getDirection(),
                "persisted direction must be IN");
        assertNotNull(persisted.getId(),
                "persisted entity must have a generated UUID");
        assertEquals(11.5, persisted.getGeoLng(),
                "persisted geoLng must match the DTO");
        assertEquals(3.8, persisted.getGeoLat(),
                "persisted geoLat must match the DTO");

        assertEquals(checkpointId, response.getCheckpointId(),
                "response checkpointId must match the DTO");
        assertEquals(livreurId, response.getLivreurUserId(),
                "response livreurUserId must match the DTO");
        assertEquals("IN", response.getDirection(),
                "response direction must be IN");
    }

    @Test
    @DisplayName("create() rejects a DTO that supplies both direction AND meterReading (XOR violation)")
    void create_rejectsBothDirectionAndMeter() {
        // Given — a payload with BOTH direction and meterReading (forbidden XOR)
        CreateScanEventDto dto = CreateScanEventDto.builder()
                .checkpointId(UUID.randomUUID())
                .livreurUserId(UUID.randomUUID())
                .direction("IN")
                .meterReading(125.0)
                .geoLng(11.5)
                .geoLat(3.8)
                .build();

        // When + Then — exactly one of (direction, meterReading) must be supplied
        assertThrows(BusinessException.class,
                () -> scanEventService.create(dto, "alice"),
                "create() must reject a scan that supplies both direction and meterReading");
    }

    @Test
    @DisplayName("create() rejects a DTO whose direction is neither IN nor OUT")
    void create_rejectsInvalidDirection() {
        // Given — a payload with an invalid direction value
        CreateScanEventDto dto = CreateScanEventDto.builder()
                .checkpointId(UUID.randomUUID())
                .livreurUserId(UUID.randomUUID())
                .direction("SIDEWAYS")
                .geoLng(11.5)
                .geoLat(3.8)
                .build();

        // When + Then — only IN/OUT are allowed directions
        assertThrows(BusinessException.class,
                () -> scanEventService.create(dto, "alice"),
                "create() must reject a scan whose direction is not IN/OUT");
    }

    @Test
    @DisplayName("createBulk() calls create() once per item, preserving order")
    void createBulk_callsCreateForEachItem() {
        // Given — a 3-item bulk payload and a repository that echoes save() calls
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        UUID c3 = UUID.randomUUID();
        UUID livreurId = UUID.randomUUID();
        CreateScanEventDto i1 = CreateScanEventDto.builder()
                .checkpointId(c1).livreurUserId(livreurId).direction("IN")
                .geoLng(1.0).geoLat(2.0).build();
        CreateScanEventDto i2 = CreateScanEventDto.builder()
                .checkpointId(c2).livreurUserId(livreurId).direction("OUT")
                .geoLng(3.0).geoLat(4.0).build();
        CreateScanEventDto i3 = CreateScanEventDto.builder()
                .checkpointId(c3).livreurUserId(livreurId).direction("IN")
                .geoLng(5.0).geoLat(6.0).build();

        when(scanEventRepository.save(any(ScanEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When — bulk ingestion
        java.util.List<ScanEventResponseDto> responses =
                scanEventService.createBulk(java.util.List.of(i1, i2, i3), livreurId.toString());

        // Then — exactly 3 calls to save() (one per item)
        verify(scanEventRepository, times(3)).save(any(ScanEvent.class));
        assertEquals(3, responses.size(), "createBulk() must return one response per item");
        assertEquals(c1, responses.get(0).getCheckpointId());
        assertEquals(c2, responses.get(1).getCheckpointId());
        assertEquals(c3, responses.get(2).getCheckpointId());
    }

    @Test
    @DisplayName("create() with an already-stored pdaSyncId returns the existing row without inserting")
    void create_samePdaSyncIdTwice_doesNotDuplicate() {
        // Given — a re-uploaded read (same PDA nonce) already in store
        UUID checkpointId = UUID.randomUUID();
        UUID livreurId = UUID.randomUUID();
        ScanEvent stored = ScanEvent.builder()
                .id(UUID.randomUUID())
                .timestamp(java.time.Instant.now())
                .checkpointId(checkpointId)
                .livreurUserId(livreurId)
                .direction(ScanDirection.OUT)
                .geoLng(11.5).geoLat(3.8)
                .pdaSyncId("PDA-RETRY-1")
                .build();
        when(scanEventRepository.findByPdaSyncId("PDA-RETRY-1"))
                .thenReturn(java.util.Optional.of(stored));

        CreateScanEventDto retry = CreateScanEventDto.builder()
                .checkpointId(checkpointId).livreurUserId(livreurId).direction("OUT")
                .geoLng(11.5).geoLat(3.8).pdaSyncId("PDA-RETRY-1").build();

        // When — the PDA retries the unconfirmed upload
        ScanEventResponseDto response = scanEventService.create(retry, livreurId.toString());

        // Then — no second row, the stored one is echoed back
        verify(scanEventRepository, never()).save(any(ScanEvent.class));
        assertEquals(stored.getId(), response.getId(),
                "retry must return the already-stored event, not a twin");
    }
}