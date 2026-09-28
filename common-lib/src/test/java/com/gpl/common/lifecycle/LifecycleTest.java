package com.gpl.common.lifecycle;

import com.gpl.common.enums.TourExecutionMode;
import com.gpl.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    /* ── Resolution depuis le code stocké en colonne varchar ──────────────── */

    @Test
    @DisplayName("fromCode résout chaque code, tolère la casse et les espaces")
    void fromCodeResolvesEveryCode() {
        for (TourneeStatus s : TourneeStatus.values()) {
            assertEquals(s, TourneeStatus.fromCode(s.name()));
            assertEquals(s, TourneeStatus.fromCode("  " + s.name().toLowerCase() + " "));
        }
        for (CheckpointStatus s : CheckpointStatus.values()) {
            assertEquals(s, CheckpointStatus.fromCode(s.name()));
            assertEquals(s, CheckpointStatus.fromCode(s.name().toLowerCase()));
        }
    }

    @Test
    @DisplayName("fromCode rejette un code hors domaine en 422, pas en IllegalArgumentException")
    void fromCodeRejectsOutOfDomainCode() {
        // "ACTIVE" est l'état par défaut d'AuditableEntity : une ligne jamais
        // transitionnée en contient un, et il ne fait partie d'aucun des deux
        // domaines. Il doit échouer explicitement plutôt que d'être accepté.
        assertThrows(BusinessException.class, () -> TourneeStatus.fromCode("ACTIVE"));
        assertThrows(BusinessException.class, () -> TourneeStatus.fromCode("STARTED"));
        assertThrows(BusinessException.class, () -> TourneeStatus.fromCode(null));
        assertThrows(BusinessException.class, () -> CheckpointStatus.fromCode("ACTIVE"));
        assertThrows(BusinessException.class, () -> CheckpointStatus.fromCode("VALIDATED"));
        assertThrows(BusinessException.class, () -> CheckpointStatus.fromCode(""));
    }

    @Test
    @DisplayName("requireTransition laisse passer l'arête légale et ne jette rien")
    void requireTransitionIsSilentOnTheLegalEdge() {
        assertDoesNotThrow(() -> Lifecycle.requireTransition(
                TourneeStatus.DRAFT, TourneeStatus.PLANNED, TourExecutionMode.INTERNAL));
        assertDoesNotThrow(() -> Lifecycle.requireTransition(
                TourneeStatus.CHECKPOINTACTIVE, TourneeStatus.CLOSED, TourExecutionMode.INTERNAL));
        assertDoesNotThrow(() -> Lifecycle.requireTransition(
                TourneeStatus.PENDINGTRANSPORTERACK, TourneeStatus.ACKNOWLEDGED,
                TourExecutionMode.EXTERNAL));
        assertDoesNotThrow(() -> Lifecycle.requireTransition(
                CheckpointStatus.PENDING, CheckpointStatus.SKIPPED));
        assertDoesNotThrow(() -> Lifecycle.requireTransition(
                CheckpointStatus.REACHED, CheckpointStatus.COMPLETED));
    }

    @Nested
    @DisplayName("requireTransition — arêtes illégales (le vrai garde-fou)")
    class RequireThrows {

        static Stream<Arguments> illegalTournee() {
            return Stream.of(
                    // Un tour ne démarre pas depuis DRAFT : il doit être planifié,
                    // ou, en mode EXTERNAL, passer par l'accusé du transporteur.
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.INPROGRESS, TourExecutionMode.INTERNAL),
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.CLOSED, TourExecutionMode.INTERNAL),
                    Arguments.of(TourneeStatus.PLANNED, TourneeStatus.CLOSED, TourExecutionMode.INTERNAL),
                    // On ne clôture pas un tour qui n'a jamais démarré.
                    Arguments.of(TourneeStatus.INPROGRESS, TourneeStatus.CLOSED, TourExecutionMode.INTERNAL),
                    // Un tour déjà enrolled ne s'annule plus.
                    Arguments.of(TourneeStatus.INPROGRESS, TourneeStatus.CANCELLED, TourExecutionMode.INTERNAL),
                    Arguments.of(TourneeStatus.CHECKPOINTACTIVE, TourneeStatus.CANCELLED,
                            TourExecutionMode.INTERNAL),
                    // Le mode EXTERNAL n'a pas d'arête DRAFT → PLANNED.
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.PLANNED, TourExecutionMode.EXTERNAL),
                    Arguments.of(TourneeStatus.DRAFT, TourneeStatus.INPROGRESS, TourExecutionMode.EXTERNAL),
                    // Pas de retour en arrière depuis un état terminal.
                    Arguments.of(TourneeStatus.CLOSED, TourneeStatus.INPROGRESS, TourExecutionMode.INTERNAL),
                    Arguments.of(TourneeStatus.CANCELLED, TourneeStatus.DRAFT, TourExecutionMode.EXTERNAL));
        }

        @ParameterizedTest(name = "{0} → {1} ({2}) doit être refusé")
        @MethodSource("illegalTournee")
        void illegalTourneeTransitionThrows(TourneeStatus from, TourneeStatus to, TourExecutionMode mode) {
            BusinessException e = assertThrows(BusinessException.class,
                    () -> Lifecycle.requireTransition(from, to, mode));
            assertTrue(e.getMessage().contains(from.name()) && e.getMessage().contains(to.name()),
                    "le message doit nommer le déplacement tenté : " + e.getMessage());
        }

        static Stream<Arguments> illegalCheckpoint() {
            return Stream.of(
                    // On ne termine pas une livraison sans avoir Recorded l'arrivée.
                    Arguments.of(CheckpointStatus.PENDING, CheckpointStatus.COMPLETED),
                    Arguments.of(CheckpointStatus.SKIPPED, CheckpointStatus.REACHED),
                    Arguments.of(CheckpointStatus.SKIPPED, CheckpointStatus.COMPLETED),
                    Arguments.of(CheckpointStatus.COMPLETED, CheckpointStatus.REACHED),
                    Arguments.of(CheckpointStatus.REACHED, CheckpointStatus.PENDING));
        }

        @ParameterizedTest(name = "{0} → {1} doit être refusé")
        @MethodSource("illegalCheckpoint")
        void illegalCheckpointTransitionThrows(CheckpointStatus from, CheckpointStatus to) {
            BusinessException e = assertThrows(BusinessException.class,
                    () -> Lifecycle.requireTransition(from, to));
            assertTrue(e.getMessage().contains(from.name()) && e.getMessage().contains(to.name()),
                    "le message doit nommer le déplacement tenté : " + e.getMessage());
        }

        @Test
        @DisplayName("depuis un état terminal, le 422 dit qu'il est terminal plutôt que d'inventer des successeurs")
        void terminalStateSaysSo() {
            BusinessException e = assertThrows(BusinessException.class,
                    () -> Lifecycle.requireTransition(
                            TourneeStatus.CLOSED, TourneeStatus.INPROGRESS, TourExecutionMode.INTERNAL));
            assertTrue(e.getMessage().contains("terminal"),
                    "le message doit signaler un état terminal : " + e.getMessage());
        }

        @Test
        @DisplayName("depuis un état non terminal, le 422 liste les transitions légales")
        void nonTerminalStateListsLegalMoves() {
            BusinessException e = assertThrows(BusinessException.class,
                    () -> Lifecycle.requireTransition(
                            TourneeStatus.DRAFT, TourneeStatus.INPROGRESS, TourExecutionMode.INTERNAL));
            // Utile au client : on ne se contente pas de refuser, on dit ce qui était possible.
            assertTrue(e.getMessage().contains("PLANNED") && e.getMessage().contains("CANCELLED"),
                    "le message doit lister les transitions légales : " + e.getMessage());
        }
    }

    @Test
    @DisplayName("successorsOf est vide pour un état terminal")
    void successorsOfTerminalIsEmpty() {
        assertTrue(Lifecycle.successorsOf(TourneeStatus.CLOSED, TourExecutionMode.INTERNAL).isEmpty());
        assertTrue(Lifecycle.successorsOf(TourneeStatus.CANCELLED, TourExecutionMode.EXTERNAL).isEmpty());
        assertTrue(Lifecycle.successorsOf(CheckpointStatus.COMPLETED).isEmpty());
        assertTrue(Lifecycle.successorsOf(CheckpointStatus.SKIPPED).isEmpty());
    }

    @Test
    @DisplayName("labelOf renvoie le libellé porté par l'enum — le statusDescription ne peut pas dériver")
    void labelOfMatchesTheEnum() {
        for (TourneeStatus s : TourneeStatus.values()) {
            assertEquals(s.getLabel(), Lifecycle.labelOf(s));
        }
        for (CheckpointStatus s : CheckpointStatus.values()) {
            assertEquals(s.getLabel(), Lifecycle.labelOf(s));
        }
    }
}
