package com.gpl.tour.service.impl;

import com.gpl.common.exception.BusinessException;
import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.common.lifecycle.CheckpointStatus;
import com.gpl.common.lifecycle.Lifecycle;
import com.gpl.common.lifecycle.TourneeStatus;
import com.gpl.tour.model.Checkpoint;
import com.gpl.tour.model.Tour;
import com.gpl.tour.repository.CheckpointRepository;
import com.gpl.tour.repository.TourRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link TourServiceImpl}. No Spring context, no live Postgres.
 * The {@code @Transactional} annotation on the service is metadata-only here because
 * the bean is instantiated directly with {@link InjectMocks}, bypassing the AOP proxy.
 *
 * <p>Guards are exercised here rather than only in the env-gated integration test:
 * the transition rules are pure and need no database to be trustworthy.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TourServiceImpl — Flux 2 lifecycle")
class TourServiceImplTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private CheckpointRepository checkpointRepository;

    @InjectMocks
    private TourServiceImpl tourService;

    @Test
    @DisplayName("completes a REACHED checkpoint to COMPLETED without touching actualArrival")
    void completeCheckpoint_persistsCompletedAndLeavesArrivalAlone() {
        // Given — a REACHED checkpoint, whose arrival was already captured by reach()
        UUID id = UUID.randomUUID();
        Checkpoint loaded = new Checkpoint();
        loaded.setId(id.toString());
        loaded.updateStatus(CheckpointStatus.REACHED.name(), Lifecycle.labelOf(CheckpointStatus.REACHED));
        java.time.Instant arrival = java.time.Instant.now().minusSeconds(600);
        loaded.setActualArrival(arrival);

        when(checkpointRepository.findById(id.toString()))
                .thenReturn(Optional.of(loaded));
        when(checkpointRepository.save(any(Checkpoint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        tourService.completeCheckpoint(id.toString(), "alice");

        // Then
        ArgumentCaptor<Checkpoint> captor = ArgumentCaptor.forClass(Checkpoint.class);
        verify(checkpointRepository).save(captor.capture());
        Checkpoint saved = captor.getValue();

        assertEquals("COMPLETED", saved.getStatus());
        // The point of splitting reach from complete: finishing the delivery must not
        // restamp when the vehicle arrived. The old validateCheckpoint did exactly
        // that, destroying the gap the anomaly timeline measures.
        assertEquals(arrival, saved.getActualArrival(),
                "completing must not restamp actualArrival — that fact belongs to reach()");
    }

    @Test
    @DisplayName("refuses to complete a checkpoint that was never reached")
    void completeCheckpoint_fromPending_throws() {
        UUID id = UUID.randomUUID();
        Checkpoint loaded = new Checkpoint();
        loaded.setId(id.toString());
        loaded.updateStatus(CheckpointStatus.PENDING.name(), Lifecycle.labelOf(CheckpointStatus.PENDING));

        when(checkpointRepository.findById(id.toString())).thenReturn(Optional.of(loaded));

        BusinessException e = assertThrows(BusinessException.class,
                () -> tourService.completeCheckpoint(id.toString(), "alice"));
        assertTrue(e.getMessage().contains("PENDING") && e.getMessage().contains("COMPLETED"),
                "the 422 must name the attempted move: " + e.getMessage());
        verify(checkpointRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to complete an already COMPLETED checkpoint")
    void completeCheckpoint_isTerminal_throws() {
        UUID id = UUID.randomUUID();
        Checkpoint loaded = new Checkpoint();
        loaded.setId(id.toString());
        loaded.updateStatus(CheckpointStatus.COMPLETED.name(), Lifecycle.labelOf(CheckpointStatus.COMPLETED));

        when(checkpointRepository.findById(id.toString())).thenReturn(Optional.of(loaded));

        assertThrows(BusinessException.class,
                () -> tourService.completeCheckpoint(id.toString(), "alice"));
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when the checkpoint does not exist")
    void completeCheckpoint_unknownId_throws() {
        String missingId = UUID.randomUUID().toString();
        when(checkpointRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> tourService.completeCheckpoint(missingId, "alice"));
    }

    @Test
    @DisplayName("sets status REACHED and stamps actualArrival when the livreur reaches the checkpoint")
    void reachCheckpoint_setsReachedAndActualArrival() {
        // Given — a PENDING checkpoint loaded from the repository with no actualArrival yet
        UUID id = UUID.randomUUID();
        Checkpoint loaded = new Checkpoint();
        loaded.setId(id.toString());
        loaded.setTourId("tour-1");
        loaded.updateStatus(CheckpointStatus.PENDING.name(), Lifecycle.labelOf(CheckpointStatus.PENDING));

        when(checkpointRepository.findById(id.toString()))
                .thenReturn(Optional.of(loaded));
        // The tour is already CHECKPOINTACTIVE, so no promotion is attempted.
        Tour tour = new Tour();
        tour.setId("tour-1");
        tour.setExecutionMode("INTERNAL");
        tour.updateStatus(TourneeStatus.CHECKPOINTACTIVE.name(),
                Lifecycle.labelOf(TourneeStatus.CHECKPOINTACTIVE));
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        // Echo whatever was passed to save() so we can assert on the mutated entity
        when(checkpointRepository.save(any(Checkpoint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When — the livreur reaches the checkpoint
        tourService.reachCheckpoint(id.toString(), "alice");

        // Then — the persisted status must be REACHED (a valid checkpoint_status enum value
        // between PENDING and COMPLETED) and actualArrival must be stamped with the arrival time.
        ArgumentCaptor<Checkpoint> captor = ArgumentCaptor.forClass(Checkpoint.class);
        verify(checkpointRepository).save(captor.capture());
        Checkpoint saved = captor.getValue();

        assertEquals("REACHED", saved.getStatus(),
                "reachCheckpoint must persist REACHED (a value of the checkpoint_status enum)");
        assertNotNull(saved.getActualArrival(),
                "reachCheckpoint must stamp actualArrival so the downstream scan pipeline knows when the livreur arrived");
    }

    @Test
    @DisplayName("promotes the tour INPROGRESS -> CHECKPOINTACTIVE on the FIRST arrival")
    void reachCheckpoint_promotesTourOnFirstArrival() {
        UUID id = UUID.randomUUID();
        Checkpoint cp = new Checkpoint();
        cp.setId(id.toString());
        cp.setTourId("tour-1");
        cp.updateStatus(CheckpointStatus.PENDING.name(), Lifecycle.labelOf(CheckpointStatus.PENDING));
        when(checkpointRepository.findById(id.toString())).thenReturn(Optional.of(cp));

        Tour tour = new Tour();
        tour.setId("tour-1");
        tour.setExecutionMode("INTERNAL");
        tour.updateStatus(TourneeStatus.INPROGRESS.name(), Lifecycle.labelOf(TourneeStatus.INPROGRESS));
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(checkpointRepository.save(any(Checkpoint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tourService.reachCheckpoint(id.toString(), "alice");

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());
        assertEquals("CHECKPOINTACTIVE", tourCaptor.getValue().getStatus(),
                "the first arrival must promote the tour to CHECKPOINTACTIVE");
    }

    @Test
    @DisplayName("refuses to reach an already REACHED checkpoint")
    void reachCheckpoint_twice_throws() {
        UUID id = UUID.randomUUID();
        Checkpoint loaded = new Checkpoint();
        loaded.setId(id.toString());
        loaded.updateStatus(CheckpointStatus.REACHED.name(), Lifecycle.labelOf(CheckpointStatus.REACHED));
        when(checkpointRepository.findById(id.toString())).thenReturn(Optional.of(loaded));

        assertThrows(BusinessException.class,
                () -> tourService.reachCheckpoint(id.toString(), "alice"));
    }
}
