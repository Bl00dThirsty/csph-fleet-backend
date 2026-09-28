package com.gpl.common.lifecycle;

import com.gpl.common.enums.TourExecutionMode;
import com.gpl.common.exception.BusinessException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tables de transition des cycles de vie de Flux 2.
 *
 * <p>Fonctions pures : ni Spring, ni JPA, ni E/S. C'est la surface de test du
 * module — chaque arête légale et illégale se vérifie sans démarrer
 * l'application ni une base de données.</p>
 *
 * <p>Une clé absente de la table signifie « état terminal, aucune arête
 * sortante ».</p>
 */
public final class Lifecycle {

    /**
     * Largeur de la colonne {@code status}.
     *
     * <p>Le schéma canonique déclare {@code status} comme un ENUM Postgres, sans
     * limite de longueur. En Java la colonne est un {@code varchar} et
     * {@code AuditableEntity} la déclare en 20 caractères — largeur insuffisante :
     * {@code PENDINGTRANSPORTERACK} en fait 21, et l'insertion échouerait chez le
     * fournisseur. La largeur vit donc avec le vocabulaire, et l'entité s'y réfère,
     * pour que les deux ne puissent plus diverger.</p>
     */
    public static final int MAX_STATUS_LENGTH = 32;

    private static final Map<TourExecutionMode, Map<TourneeStatus, Set<TourneeStatus>>> TOURNEE =
            tourneeTable();

    private static final Map<CheckpointStatus, Set<CheckpointStatus>> CHECKPOINT =
            checkpointTable();

    private Lifecycle() {
    }

    private static Map<TourExecutionMode, Map<TourneeStatus, Set<TourneeStatus>>> tourneeTable() {
        Map<TourneeStatus, Set<TourneeStatus>> internal = new EnumMap<>(TourneeStatus.class);
        internal.put(TourneeStatus.DRAFT,
                EnumSet.of(TourneeStatus.PLANNED, TourneeStatus.CANCELLED));
        internal.put(TourneeStatus.PLANNED,
                EnumSet.of(TourneeStatus.INPROGRESS, TourneeStatus.CANCELLED));
        internal.put(TourneeStatus.INPROGRESS,
                EnumSet.of(TourneeStatus.CHECKPOINTACTIVE));
        internal.put(TourneeStatus.CHECKPOINTACTIVE,
                EnumSet.of(TourneeStatus.CLOSED));

        Map<TourneeStatus, Set<TourneeStatus>> external = new EnumMap<>(TourneeStatus.class);
        external.put(TourneeStatus.DRAFT,
                EnumSet.of(TourneeStatus.PENDINGTRANSPORTERACK, TourneeStatus.CANCELLED));
        external.put(TourneeStatus.PENDINGTRANSPORTERACK,
                EnumSet.of(TourneeStatus.ACKNOWLEDGED, TourneeStatus.CANCELLED));
        external.put(TourneeStatus.ACKNOWLEDGED,
                EnumSet.of(TourneeStatus.INPROGRESS, TourneeStatus.CANCELLED));
        external.put(TourneeStatus.INPROGRESS,
                EnumSet.of(TourneeStatus.CHECKPOINTACTIVE));
        external.put(TourneeStatus.CHECKPOINTACTIVE,
                EnumSet.of(TourneeStatus.CLOSED));

        Map<TourExecutionMode, Map<TourneeStatus, Set<TourneeStatus>>> table =
                new EnumMap<>(TourExecutionMode.class);
        table.put(TourExecutionMode.INTERNAL, internal);
        table.put(TourExecutionMode.EXTERNAL, external);
        return Collections.unmodifiableMap(table);
    }

    private static Map<CheckpointStatus, Set<CheckpointStatus>> checkpointTable() {
        Map<CheckpointStatus, Set<CheckpointStatus>> table = new EnumMap<>(CheckpointStatus.class);
        table.put(CheckpointStatus.PENDING,
                EnumSet.of(CheckpointStatus.REACHED, CheckpointStatus.SKIPPED));
        table.put(CheckpointStatus.REACHED,
                EnumSet.of(CheckpointStatus.COMPLETED, CheckpointStatus.SKIPPED));
        return Collections.unmodifiableMap(table);
    }

    public static boolean canTransition(TourneeStatus from, TourneeStatus to, TourExecutionMode mode) {
        return TOURNEE.get(mode)
                .getOrDefault(from, EnumSet.noneOf(TourneeStatus.class))
                .contains(to);
    }

    public static boolean canTransition(CheckpointStatus from, CheckpointStatus to) {
        return CHECKPOINT.getOrDefault(from, EnumSet.noneOf(CheckpointStatus.class)).contains(to);
    }

    /**
     * Successors legally reachable from {@code from} in the given mode.
     *
     * <p>Empty for a terminal state. Exposed so a 422 can tell the caller what
     * <em>would</em> have been legal instead of only what was attempted.</p>
     */
    public static Set<TourneeStatus> successorsOf(TourneeStatus from, TourExecutionMode mode) {
        return TOURNEE.get(mode).getOrDefault(from, EnumSet.noneOf(TourneeStatus.class));
    }

    public static Set<CheckpointStatus> successorsOf(CheckpointStatus from) {
        return CHECKPOINT.getOrDefault(from, EnumSet.noneOf(CheckpointStatus.class));
    }

    /**
     * The door every lifecycle write goes through.
     *
     * <p>Still pure — it throws, it does not touch Spring, JPA or any I/O — but it
     * is the single place the rule "an illegal move is a 422, not a silent
     * overwrite" is stated. Every service that transitions an entity calls this,
     * so the seven guards cannot drift apart the way seven hand-written
     * terminal-state blacklists already have.</p>
     *
     * @throws BusinessException naming the attempted move and the legal ones
     */
    public static void requireTransition(TourneeStatus from, TourneeStatus to, TourExecutionMode mode) {
        if (!canTransition(from, to, mode)) {
            Set<TourneeStatus> legal = successorsOf(from, mode);
            throw new BusinessException("Transition interdite pour la tournée : " + from.name() + " → "
                    + to.name() + " (mode " + mode.name() + ")."
                    + (legal.isEmpty()
                            ? " L'état " + from.name() + " est terminal : aucune transition sortante."
                            : " Transitions autorisées depuis " + from.name() + " : " + join(legal) + "."));
        }
    }

    /**
     * @throws BusinessException naming the attempted move and the legal ones
     */
    public static void requireTransition(CheckpointStatus from, CheckpointStatus to) {
        if (!canTransition(from, to)) {
            Set<CheckpointStatus> legal = successorsOf(from);
            throw new BusinessException("Transition interdite pour l'arrêt de tournée : " + from.name()
                    + " → " + to.name() + "."
                    + (legal.isEmpty()
                            ? " L'état " + from.name() + " est terminal : aucune transition sortante."
                            : " Transitions autorisées depuis " + from.name() + " : " + join(legal) + "."));
        }
    }

    /**
     * Libellé du statut — le « statusDescription » du pattern dual Maximo.
     *
     * <p>Le service écrit ce libellé en même temps que le code, ce qui rend
     * impossible leur désynchronisation : la seule façon d'obtenir un libellé
     * périmé est de le saisir à la main.</p>
     */
    public static String labelOf(TourneeStatus status) {
        return status.getLabel();
    }

    public static String labelOf(CheckpointStatus status) {
        return status.getLabel();
    }

    private static String join(Set<? extends Enum<?>> statuses) {
        return statuses.stream().map(Enum::name).sorted().reduce((a, b) -> a + ", " + b).orElse("");
    }

    public static boolean isTerminal(TourneeStatus status) {
        return !TOURNEE.get(TourExecutionMode.INTERNAL).containsKey(status)
                && !TOURNEE.get(TourExecutionMode.EXTERNAL).containsKey(status);
    }

    public static boolean isTerminal(CheckpointStatus status) {
        return !CHECKPOINT.containsKey(status);
    }

    public static Set<TourneeStatus> cancellableFrom(TourExecutionMode mode) {
        return TOURNEE.get(mode).entrySet().stream()
                .filter(entry -> entry.getValue().contains(TourneeStatus.CANCELLED))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TourneeStatus.class)));
    }
}
