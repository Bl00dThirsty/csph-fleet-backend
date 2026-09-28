package com.gpl.tour.integration;

import com.gpl.tour.dto.CheckpointResponseDto;
import com.gpl.tour.dto.CreateCheckpointDto;
import com.gpl.tour.dto.CreateScanEventDto;
import com.gpl.tour.dto.CreateTourDto;
import com.gpl.tour.dto.TourResponseDto;
import com.gpl.tour.model.Checkpoint;
import com.gpl.tour.model.ScanEvent;
import com.gpl.tour.model.ScanEventId;
import com.gpl.tour.model.Tour;
import com.gpl.tour.repository.CheckpointRepository;
import com.gpl.tour.repository.ScanEventRepository;
import com.gpl.tour.repository.TourRepository;
import com.gpl.tour.service.ScanEventService;
import com.gpl.tour.service.TourService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration test for the full INTERNAL tour lifecycle.
 *
 * <p>Walks the canonical happy path against the real Spring context and the
 * running Postgres database ({@code gpl_tour_db}):</p>
 *
 * <ol>
 *   <li>create (DRAFT) → set PLANNED via direct repository setStatus (UpdateTourDto
 *       does not expose {@code status} so the public update path can't transition it)</li>
 *   <li>start → INPROGRESS</li>
 *   <li>per-checkpoint: load → reach → scan (VRAC meter reading) → validate (COMPLETED)</li>
 *   <li>close → CLOSED with quantities stamped</li>
 *   <li>assert: tour CLOSED, closedAt set, deliveredQuantity matches,
 *       3 checkpoints all COMPLETED, ≥3 scan_events rows</li>
 * </ol>
 *
 * <p>The checkpoint finder is {@code findByTourIdOrderBySequenceAsc} — the JPA
 * property name {@code tourId} maps to the {@code tournee_id} column per v6_2.</p>
 *
 * <p>Data is deterministic and scoped: one client_site is reused across all 3
 * checkpoints with distinct {@code sequence} values, fulfilling the
 * {@code UNIQUE (tournee_id, sequence)} constraint while exercising the
 * multi-stop flow.</p>
 *
 * <p>Cleanup runs in {@link AfterEach} via {@code tourRepository.deleteById}
 * (cascade removes checkpoints). Any orphaned scan_events whose checkpoint
 * was deleted before the cascade are also removed.</p>
 *
 * @author GPL-RFID Team | Digit-Tech-Innov Solutions and Services
 * @version 1.0
 * @since 28.09.2026
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "TOUR_IT_DB_URL", matches = ".+")
@DisplayName("TourLifecycleIT — full INTERNAL round-trip DRAFT → PLANNED → INPROGRESS → CLOSED")
class TourLifecycleIT {

    /* Seeded identifiers (see V1__seed_test_data.sql). Each service owns its own DB,
     * so cross-service FKs aren't enforced in gpl_tour_db — any well-formed UUID works. */
    private static final String MARKETEUR_ORG_ID = "22222222-2222-2222-2222-222222222222"; // TOTAL Cameroun
    private static final String CLIENT_SITE_ID    = "c1111111-1111-1111-1111-111111111111";

    @Autowired private TourService tourService;
    @Autowired private ScanEventService scanEventService;
    @Autowired private TourRepository tourRepository;
    @Autowired private CheckpointRepository checkpointRepository;
    @Autowired private ScanEventRepository scanEventRepository;

    private String createdTourId;

    @Test
    @DisplayName("fullInternalLifecycle_reachesClosedWithCompletedCheckpointsAndScans")
    void fullInternalLifecycle_reachesClosedWithCompletedCheckpointsAndScans() {
        /* ── Step a — create the INTERNAL tour (status defaults to DRAFT/ACTIVE) ── */
        final String livreurUserId = "b1111111-1111-1111-1111-111111111111";
        final String vehicleId     = "a1111111-1111-1111-1111-111111111111";
        final String driverId      = "d1111111-1111-1111-1111-111111111111";
        final String driverPersonId = "e1111111-1111-1111-1111-111111111111";

        CreateTourDto createDto = CreateTourDto.builder()
                .tourCode("T-LIFECYCLE-IT-" + UUID.randomUUID().toString().substring(0, 8))
                .marketerOrganizationId(MARKETEUR_ORG_ID)
                .executionMode("INTERNAL")
                .type("VRAC")
                .requestedQuantity(5.0)
                .vehicleId(vehicleId)
                .driverId(driverId)
                .driverPersonId(driverPersonId)
                .build();

        TourResponseDto created = tourService.create(createDto, "tester");
        createdTourId = created.getId();
        assertNotNull(createdTourId, "create() must return a non-null tour id");

        /* ── Step b — add 3 checkpoints (same client_site, distinct sequences) ── */
        for (int seq = 1; seq <= 3; seq++) {
            CreateCheckpointDto cpDto = CreateCheckpointDto.builder()
                    .sequence(seq)
                    .clientSiteId(CLIENT_SITE_ID)
                    .expectedArrival(Instant.now().plusSeconds(seq * 600L))
                    .build();
            CheckpointResponseDto cp = tourService.addCheckpoint(createdTourId, cpDto, "tester");
            assertNotNull(cp.getId(), "checkpoint #" + seq + " must be persisted with an id");
            assertEquals("PENDING", cp.getStatus(),
                    "new checkpoint must start in PENDING (a real checkpoint_status enum value)");
        }

        /* ── Step c — transition DRAFT → PLANNED via direct repository setStatus
         *    UpdateTourDto doesn't expose 'status', so the public update endpoint
         *    can't do it — we set it directly on the entity (mirrors what a real
         *    planner UI would do once the v6_2 enum-driven workflow ships).       ── */
        Tour tour = tourRepository.findById(createdTourId)
                .orElseThrow(() -> new AssertionError("tour vanished after create"));
        tour.setStatus("PLANNED");
        tour.setStatusDescription("Tournée planifiée et prête à démarrer");
        tour.setStatusDate(Instant.now());
        tour.setChangeby("tester");
        tourRepository.saveAndFlush(tour);

        /* ── Step d — start the tour (PLANNED → STARTED/INPROGRESS) ── */
        TourResponseDto started = tourService.startTour(createdTourId, "livreur");
        assertEquals("INPROGRESS", started.getStatus(),
                "startTour must persist INPROGRESS (a valid tournee_status enum value)");
        assertNotNull(started.getStartedAt(),
                "startTour must stamp startedAt so the audit trail records the real departure");

        /* ── Step e — for each checkpoint: reach → scan → validate ── */
        List<Checkpoint> checkpoints =
                checkpointRepository.findByTourIdOrderBySequenceAsc(createdTourId);
        assertEquals(3, checkpoints.size(),
                "3 checkpoints must be persisted in sequence order");

        UUID livreurUuid = UUID.fromString(livreurUserId);
        for (Checkpoint cp : checkpoints) {
            /* reach — PENDING → REACHED */
            tourService.reachCheckpoint(cp.getId(), "livreur");
            Checkpoint reloaded = checkpointRepository.findById(cp.getId())
                    .orElseThrow(() -> new AssertionError("checkpoint vanished after reach"));
            assertEquals("REACHED", reloaded.getStatus(),
                    "reachCheckpoint must persist REACHED");
            assertNotNull(reloaded.getActualArrival(),
                    "reachCheckpoint must stamp actualArrival");

            /* scan — VRAC volumetric meter reading at a valid Douala-area geoPoint */
            CreateScanEventDto scanDto = CreateScanEventDto.builder()
                    .checkpointId(UUID.fromString(cp.getId()))
                    .livreurUserId(livreurUuid)
                    .meterReading(1.5)
                    .geoLng(9.7)
                    .geoLat(4.05)
                    .pdaSyncId("PDA-IT-" + cp.getSequence())
                    .build();
            scanEventService.create(scanDto, livreurUserId);

            /* validate — REACHED → COMPLETED */
            tourService.validateCheckpoint(cp.getId(), "livreur");
            Checkpoint validated = checkpointRepository.findById(cp.getId())
                    .orElseThrow(() -> new AssertionError("checkpoint vanished after validate"));
            assertEquals("COMPLETED", validated.getStatus(),
                    "validateCheckpoint must persist COMPLETED (a real checkpoint_status enum value)");
        }

        /* ── Step f — close the tour ── */
        TourResponseDto closed = tourService.closeTour(createdTourId, 5.0, 4.5, "livreur");
        assertEquals("CLOSED", closed.getStatus(),
                "closeTour must persist CLOSED (a valid tournee_status enum value)");

        /* ── Step g — final tour state ── */
        Tour finalTour = tourRepository.findById(createdTourId)
                .orElseThrow(() -> new AssertionError("tour vanished after close"));
        assertEquals("CLOSED", finalTour.getStatus(), "tour must be CLOSED");
        assertNotNull(finalTour.getClosedAt(), "closedAt must be stamped");
        assertEquals(4.5, finalTour.getDeliveredQuantity(),
                "deliveredQuantity must equal what was passed to closeTour");

        /* ── Step h — checkpoint fan-out ── */
        List<Checkpoint> finalCheckpoints =
                checkpointRepository.findByTourIdOrderBySequenceAsc(createdTourId);
        assertEquals(3, finalCheckpoints.size(),
                "all 3 checkpoints must still be associated with the tour");
        for (Checkpoint cp : finalCheckpoints) {
            assertEquals("COMPLETED", cp.getStatus(),
                    "checkpoint seq=" + cp.getSequence() + " must be COMPLETED");
        }

        /* ── Step i — scan-event fan-out ── */
        for (Checkpoint cp : finalCheckpoints) {
            List<ScanEvent> scans = scanEventRepository
                    .findByCheckpointIdOrderByTimestampAsc(UUID.fromString(cp.getId()));
            assertFalse(scans.isEmpty(),
                    "checkpoint seq=" + cp.getSequence() + " must have ≥1 scan_event row");
            assertTrue(scans.size() >= 1,
                    "checkpoint seq=" + cp.getSequence() + " must have ≥1 scan_event row (got "
                            + scans.size() + ")");
        }
    }

    @AfterEach
    void cleanup() {
        if (createdTourId == null) {
            return;
        }
        // Cascade removes the checkpoints; scan_events reference checkpoint_id (no FK
        // in the running schema), so we must scrub them explicitly to keep the test
        // idempotent across re-runs.
        try {
            List<Checkpoint> cps = checkpointRepository.findByTourIdOrderBySequenceAsc(createdTourId);
            for (Checkpoint cp : cps) {
                scanEventRepository
                        .findByCheckpointIdOrderByTimestampAsc(UUID.fromString(cp.getId()))
                        .forEach(se -> scanEventRepository.deleteById(new ScanEventId(se.getId(), se.getTimestamp())));
            }
            tourRepository.findById(createdTourId).ifPresent(tourRepository::delete);
        } catch (Exception ignored) {
            // best-effort cleanup; failure here should not mask a passing test
        }
        createdTourId = null;
    }
}
