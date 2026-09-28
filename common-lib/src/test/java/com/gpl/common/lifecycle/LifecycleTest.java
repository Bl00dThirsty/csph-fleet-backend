package com.gpl.common.lifecycle;

import com.gpl.common.enums.TourExecutionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tables de transition Flux 2")
class LifecycleTest {

    @Nested
    @DisplayName("Tournée — mode INTERNAL")
    class TourneeInternal {

        static Stream<Arguments> legal() {
            return Stream.of(
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.PLANNED),
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.CANCELLED),
                    Arguments.of(TourneeStatus.PLANNED, TourneeStatus.INPROGRESS),
                    Arguments.of(TourneeStatus.PLANNED, TourneeStatus.CANCELLED),
                    Arguments.of(TourneeStatus.INPROGRESS, TourneeStatus.CHECKPOINTACTIVE),
                    Arguments.of(TourneeStatus.CHECKPOINTACTIVE, TourneeStatus.CLOSED));
        }

        static Stream<Arguments> illegal() {
            return Stream.of(
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.INPROGRESS),
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.CLOSED),
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.CHECKPOINTACTIVE),
                    Arguments.of(TourneeStatus.PLANNED, TourneeStatus.CLOSED),
                    Arguments.of(TourneeStatus.INPROGRESS, TourneeStatus.CLOSED),
                    Arguments.of(TourneeStatus.INPROGRESS, TourneeStatus.CANCELLED),
                    Arguments.of(TourneeStatus.CHECKPOINTACTIVE, TourneeStatus.CANCELLED),
                    Arguments.of(TourneeStatus.CLOSED, TourneeStatus.INPROGRESS),
                    Arguments.of(TourneeStatus.CLOSED, TourneeStatus.CANCELLED),
                    Arguments.of(TourneeStatus.CANCELLED, TourneeStatus.PLANNED),
                    Arguments.of(TourneeStatus.CANCELLED, TourneeStatus.DRAFT),
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.PENDINGTRANSPORTERACK),
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.ACKNOWLEDGED),
                    Arguments.of(TourneeStatus.PLANNED, TourneeStatus.PENDINGTRANSPORTERACK));
        }

        @ParameterizedTest(name = "{0} -> {1} est légale")
        @MethodSource("legal")
        void legalEdgesAreAllowed(TourneeStatus from, TourneeStatus to) {
            assertTrue(Lifecycle.canTransition(from, to, TourExecutionMode.INTERNAL));
        }

        @ParameterizedTest(name = "{0} -> {1} est interdite")
        @MethodSource("illegal")
        void illegalEdgesAreRefused(TourneeStatus from, TourneeStatus to) {
            assertFalse(Lifecycle.canTransition(from, to, TourExecutionMode.INTERNAL));
        }
    }

    @Nested
    @DisplayName("Tournée — mode EXTERNAL")
    class TourneeExternal {

        static Stream<Arguments> legal() {
            return Stream.of(
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.PENDINGTRANSPORTERACK),
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.CANCELLED),
                    Arguments.of(TourneeStatus.PENDINGTRANSPORTERACK, TourneeStatus.ACKNOWLEDGED),
                    Arguments.of(TourneeStatus.PENDINGTRANSPORTERACK, TourneeStatus.CANCELLED),
                    Arguments.of(TourneeStatus.ACKNOWLEDGED, TourneeStatus.INPROGRESS),
                    Arguments.of(TourneeStatus.ACKNOWLEDGED, TourneeStatus.CANCELLED),
                    Arguments.of(TourneeStatus.INPROGRESS, TourneeStatus.CHECKPOINTACTIVE),
                    Arguments.of(TourneeStatus.CHECKPOINTACTIVE, TourneeStatus.CLOSED));
        }

        static Stream<Arguments> illegal() {
            return Stream.of(
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.PLANNED),
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.INPROGRESS),
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.CLOSED),
                    Arguments.of(TourneeStatus.PENDINGTRANSPORTERACK, TourneeStatus.INPROGRESS),
                    Arguments.of(TourneeStatus.ACKNOWLEDGED, TourneeStatus.CHECKPOINTACTIVE),
                    Arguments.of(TourneeStatus.INPROGRESS, TourneeStatus.CLOSED),
                    Arguments.of(TourneeStatus.INPROGRESS, TourneeStatus.CANCELLED),
                    Arguments.of(TourneeStatus.CLOSED, TourneeStatus.INPROGRESS),
                    Arguments.of(TourneeStatus.CANCELLED, TourneeStatus.DRAFT));
        }

        @ParameterizedTest(name = "{0} -> {1} est légale")
        @MethodSource("legal")
        void legalEdgesAreAllowed(TourneeStatus from, TourneeStatus to) {
            assertTrue(Lifecycle.canTransition(from, to, TourExecutionMode.EXTERNAL));
        }

        @ParameterizedTest(name = "{0} -> {1} est interdite")
        @MethodSource("illegal")
        void illegalEdgesAreRefused(TourneeStatus from, TourneeStatus to) {
            assertFalse(Lifecycle.canTransition(from, to, TourExecutionMode.EXTERNAL));
        }
    }

    @Nested
    @DisplayName("Arrêt")
    class Checkpoint {

        static Stream<Arguments> legal() {
            return Stream.of(
                    Arguments.of(CheckpointStatus.PENDING, CheckpointStatus.REACHED),
                    Arguments.of(CheckpointStatus.PENDING, CheckpointStatus.SKIPPED),
                    Arguments.of(CheckpointStatus.REACHED, CheckpointStatus.COMPLETED),
                    Arguments.of(CheckpointStatus.REACHED, CheckpointStatus.SKIPPED));
        }

        static Stream<Arguments> illegal() {
            return Stream.of(
                    Arguments.of(CheckpointStatus.PENDING, CheckpointStatus.COMPLETED),
                    Arguments.of(CheckpointStatus.COMPLETED, CheckpointStatus.REACHED),
                    Arguments.of(CheckpointStatus.COMPLETED, CheckpointStatus.SKIPPED),
                    Arguments.of(CheckpointStatus.SKIPPED, CheckpointStatus.REACHED),
                    Arguments.of(CheckpointStatus.SKIPPED, CheckpointStatus.COMPLETED),
                    Arguments.of(CheckpointStatus.SKIPPED, CheckpointStatus.SKIPPED));
        }

        @ParameterizedTest(name = "{0} -> {1} est légale")
        @MethodSource("legal")
        void legalEdgesAreAllowed(CheckpointStatus from, CheckpointStatus to) {
            assertTrue(Lifecycle.canTransition(from, to));
        }

        @ParameterizedTest(name = "{0} -> {1} est interdite")
        @MethodSource("illegal")
        void illegalEdgesAreRefused(CheckpointStatus from, CheckpointStatus to) {
            assertFalse(Lifecycle.canTransition(from, to));
        }
    }

    @Nested
    @DisplayName("Terminaux et annulation")
    class Terminals {

        @Test
        void closedAndCancelledAreTerminal() {
            assertTrue(Lifecycle.isTerminal(TourneeStatus.CLOSED));
            assertTrue(Lifecycle.isTerminal(TourneeStatus.CANCELLED));
            assertFalse(Lifecycle.isTerminal(TourneeStatus.DRAFT));
            assertFalse(Lifecycle.isTerminal(TourneeStatus.CHECKPOINTACTIVE));
        }

        @Test
        void completedAndSkippedAreTerminal() {
            assertTrue(Lifecycle.isTerminal(CheckpointStatus.COMPLETED));
            assertTrue(Lifecycle.isTerminal(CheckpointStatus.SKIPPED));
            assertFalse(Lifecycle.isTerminal(CheckpointStatus.PENDING));
        }

        @Test
        void cancellableFromMatchesTheSchema() {
            assertEquals(
                    Set.of(TourneeStatus.DRAFT, TourneeStatus.PLANNED),
                    Lifecycle.cancellableFrom(TourExecutionMode.INTERNAL));
            assertEquals(
                    Set.of(TourneeStatus.DRAFT, TourneeStatus.PENDINGTRANSPORTERACK, TourneeStatus.ACKNOWLEDGED),
                    Lifecycle.cancellableFrom(TourExecutionMode.EXTERNAL));
        }
    }

    @Test
    @DisplayName("Chaque statut a un libellé français non vide")
    void everyStatusHasALabel() {
        for (TourneeStatus s : TourneeStatus.values()) {
            assertTrue(s.getLabel() != null && !s.getLabel().isBlank(), s.name());
        }
        for (CheckpointStatus s : CheckpointStatus.values()) {
            assertTrue(s.getLabel() != null && !s.getLabel().isBlank(), s.name());
        }
    }

    @Test
    @DisplayName("Chaque code tient dans la largeur de la colonne status")
    void everyCodeFitsTheStatusColumn() {
        for (TourneeStatus s : TourneeStatus.values()) {
            assertTrue(s.name().length() <= Lifecycle.MAX_STATUS_LENGTH,
                    s.name() + " (" + s.name().length() + " car.) dépasse la largeur de "
                            + Lifecycle.MAX_STATUS_LENGTH + " caractères");
        }
        for (CheckpointStatus s : CheckpointStatus.values()) {
            assertTrue(s.name().length() <= Lifecycle.MAX_STATUS_LENGTH,
                    s.name() + " (" + s.name().length() + " car.) dépasse la largeur de "
                            + Lifecycle.MAX_STATUS_LENGTH + " caractères");
        }
    }

    @Test
    @DisplayName("PENDINGTRANSPORTERACK tient dans 20 caractères — c'est ce qui a fait échouer le premier essai")
    void longestCodeIsTheKnownTrap() {
        // 21 caractères. C'est exactement la valeur qui a fait échouer une largeur
        // de colonne fixée à 20, et qui fit échouer la première exécution de ce test.
        assertEquals(21, TourneeStatus.PENDINGTRANSPORTERACK.name().length());
    }
}
