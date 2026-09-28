package com.gpl.tour.service.impl;

import com.gpl.common.exception.ResourceNotFoundException;
import com.gpl.tour.model.Checkpoint;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link TourServiceImpl}. No Spring context, no live Postgres.
 * The {@code @Transactional} annotation on the service is metadata-only here because
 * the bean is instantiated directly with {@link InjectMocks}, bypassing the AOP proxy.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TourServiceImpl.validateCheckpoint")
class TourServiceImplTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private CheckpointRepository checkpointRepository;

    @InjectMocks
    private TourServiceImpl tourService;

    @Test
    @DisplayName("persists status COMPLETED (a valid checkpoint_status enum value) and not VALIDATED")
    void validateCheckpoint_persistsCompletedNotValidated() {
        // Given — a PENDING checkpoint loaded from the repository
        UUID id = UUID.randomUUID();
        Checkpoint loaded = new Checkpoint();
        loaded.setId(id.toString());
        loaded.setStatus("PENDING");

        when(checkpointRepository.findById(id.toString()))
                .thenReturn(Optional.of(loaded));
        // Echo whatever was passed to save() so we can assert on the mutated entity
        when(checkpointRepository.save(any(Checkpoint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When — the operator validates the checkpoint
        tourService.validateCheckpoint(id.toString(), "alice");

        // Then — the persisted status must be COMPLETED (a real enum value),
        // not the bogus VALIDATED that would fail the DB check constraint.
        ArgumentCaptor<Checkpoint> captor = ArgumentCaptor.forClass(Checkpoint.class);
        verify(checkpointRepository).save(captor.capture());
        Checkpoint saved = captor.getValue();

        assertEquals("COMPLETED", saved.getStatus(),
                "validateCheckpoint must persist COMPLETED (a value of the checkpoint_status enum)");
        assertNotEquals("VALIDATED", saved.getStatus(),
                "validateCheckpoint must NOT persist VALIDATED — it is not part of the checkpoint_status enum");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when the checkpoint does not exist")
    void validateCheckpoint_unknownId_throws() {
        String missingId = UUID.randomUUID().toString();
        when(checkpointRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> tourService.validateCheckpoint(missingId, "alice"));
    }
}
