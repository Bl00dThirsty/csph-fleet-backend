#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# reset-db.sh — wipe Postgres PVCs so the Hibernate + seeder bootstrap reruns.
#
#   ./k8s/scripts/reset-db.sh --all                     # all 9, asks first
#   ./k8s/scripts/reset-db.sh --all --yes               # all 9, no prompt
#   ./k8s/scripts/reset-db.sh --service user            # one bounded context
#   ./k8s/scripts/reset-db.sh --service fleet-device    # ditto
#   ./k8s/scripts/reset-db.sh --list                    # what is on disk now
#   ./k8s/scripts/reset-db.sh --yes                     # interactive: pick one
#
# ⚠ THIS DESTROYS DATA. It is gated behind an explicit --yes (or a typed
# confirmation). There is no dry-run-by-accident path.
#
# WHAT IT ACTUALLY DOES, AND WHY IT IS NOT JUST "kubectl delete pvc"
#   1. scale the owning StatefulSet to 0 (otherwise the pod is recreated the
#      instant the PVC disappears, and the new pod re-attaches a volume the
#      kubelet is still trying to mount)
#   2. delete the PVC `data-postgres-<x>-0`
#   3. scale the StatefulSet back to 1
#   4. wait for the pod to be Ready
#   5. WAIT FOR THE OWNING SERVICE TO RECONNECT — the Hikari pool is opened at
#      Spring startup, so a service whose database just vanished underneath it
#      is NOT automatically healthy again. It has to be restarted.
#
# Step 5 is the part that is easy to forget and produces the classic
# "I wiped the DB and now everything is broken" afternoon: the schema comes
# back, but the service that was talking to it is holding a dead connection.
#
# ── THE prod PROFILE TRAP. READ THIS. ───────────────────────────────────────
# The five seeders (AuthDataInitializer, DataInitializer, UserDataInitializer,
# RolesPermissionsInitializer, TourDataInitializer) are all annotated
#     @Profile({"dev", "test", "local", "default"})
# and k8s/overlays/dev sets SPRING_PROFILES_ACTIVE=dev, so they run.
#
# k8s/overlays/prod sets SPRING_PROFILES_ACTIVE=prod. Under that profile:
#     * Hibernate ddl-auto=update STILL runs — the schema is created
#     * the seeders DO NOT run — every table is EMPTY
#
# So wiping a database on the prod overlay produces a working, empty schema:
# no users, no roles, no permissions, no organisations, no tours. Nobody can
# log in, and there is nothing in the logs to explain why. This script refuses
# to run against prod without an extra, deliberate flag.
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail
# shellcheck source=./common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

TARGET_SERVICE=""
ALL=0
ASSUME_YES=0
ALLOW_PROD=0
LIST=0
NO_RESTART=0

usage() {
    sed -n '3,15p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit 0
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --all)        ALL=1; shift ;;
        --yes|-y)     ASSUME_YES=1; shift ;;
        --allow-prod) ALLOW_PROD=1; shift ;;
        --no-restart) NO_RESTART=1; shift ;;
        --list)       LIST=1; shift ;;
        --service)    TARGET_SERVICE="${2:?--service needs a bounded context, e.g. user}"; shift 2 ;;
        -h|--help)    usage ;;
        *)            log_die "unknown argument: $1  (try --help)" ;;
    esac
done

require_tools_cluster
cd "$REPO_ROOT"

if ! init_cluster_context; then
    log_die "kind cluster '${CLUSTER_NAME}' is not available."
fi

# ── map a service name onto its StatefulSet ─────────────────────────────────
# Accepts the service (user) or the StatefulSet (postgres-user) so the operator
# does not have to remember which of the two nomenclatures applies.
resolve_sts() {
    local want="$1"
    if csv_contains "$want" "$CSPH_DATABASES"; then
        printf '%s' "$want"
        return 0
    fi
    local pair svc sts
    for pair in $CSPH_SERVICE_DB; do
        svc="${pair%%:*}"
        sts="${pair##*:}"
        if [ "$svc" = "$want" ]; then
            printf '%s' "$sts"
            return 0
        fi
    done
    return 1
}

# ── what is on disk ─────────────────────────────────────────────────────────
db_rows() {
    for sts in $CSPH_DATABASES; do
        pvc="data-${sts}-0"
        size="$(kubectl get pvc "$pvc" -n "$NAMESPACE" \
            -o jsonpath='{.status.capacity.storage}' 2>/dev/null || true)"
        bound="$(kubectl get pvc "$pvc" -n "$NAMESPACE" \
            -o jsonpath='{.status.phase}' 2>/dev/null || true)"
        if [ -n "$bound" ]; then
            printf '  %-18s %-28s %-10s %s\n' "$sts" "$pvc" "${size:-?}" "$bound"
        else
            printf '  %-18s %-28s %s\n' "$sts" "$pvc" "does not exist"
        fi
    done
}

log_banner "POSTGRES PVCs  (namespace ${NAMESPACE})"
db_rows
printf '\n'

if [ "$LIST" -eq 1 ]; then
    log_dim "Reset one with:  ${COMMON_SH_DIR}/reset-db.sh --service user --yes"
    exit 0
fi

# ── work out the target set ─────────────────────────────────────────────────
TARGETS=""
if [ "$ALL" -eq 1 ]; then
    TARGETS="$CSPH_DATABASES"
elif [ -n "$TARGET_SERVICE" ]; then
    sts="$(resolve_sts "$TARGET_SERVICE" || true)"
    [ -n "$sts" ] || log_die "unknown service '${TARGET_SERVICE}'.
  services:  auth organization user audit notification tour fleet-device cylinder subsidy
  (or the StatefulSet name, e.g. postgres-user)"
    TARGETS="$sts"
else
    # No flag at all: present the menu rather than guessing. A default of
    # --all here would destroy nine databases on a stray Enter.
    log_step "pick one bounded context (or re-run with --all)"
    i=1
    for pair in $CSPH_SERVICE_DB; do
        svc="${pair%%:*}"
        printf '   %2d) %-22s -> %s\n' "$i" "$svc" "${pair##*:}"
        i=$((i + 1))
    done
    printf '\n   all) all nine databases\n'
    printf '\n  choice: '
    read -r choice
    if [ "$choice" = "all" ]; then
        ALL=1
        TARGETS="$CSPH_DATABASES"
    else
        case "$choice" in
            ''|*[!0-9]*) log_die "not a valid choice." ;;
        esac
        i=1
        for pair in $CSPH_SERVICE_DB; do
            if [ "$i" -eq "$choice" ]; then
                TARGETS="${pair##*:}"
                break
            fi
            i=$((i + 1))
        done
        [ -n "$TARGETS" ] || log_die "not a valid choice."
    fi
fi

[ -n "$TARGETS" ] || log_die "nothing selected."

# ── the prod guard ──────────────────────────────────────────────────────────
# Reads the live profile rather than guessing from the overlay that was applied,
# because the same PVCs are shared by both overlays.
active_profile="$(kubectl get deploy -n "$NAMESPACE" \
    -o jsonpath='{range .items[*].spec.template.spec.containers[*].envFrom[*]}{.configMapRef.name}{"\n"}{end}' 2>/dev/null \
    | while read -r cm; do
        [ -n "$cm" ] || continue
        kubectl get configmap "$cm" -n "$NAMESPACE" -o jsonpath='{.data.SPRING_PROFILES_ACTIVE}' 2>/dev/null || true
        printf '\n'
      done | grep -v '^$' | sort -u | tr '\n' ',')"

if printf '%s' "$active_profile" | grep -q 'prod'; then
    log_warn "the deployed ConfigMaps set SPRING_PROFILES_ACTIVE=prod."
    if [ "$ALLOW_PROD" -ne 1 ]; then
        log_err "refusing to reset under the prod profile."
        log_dim ""
        log_dim "The five seeders are @Profile({\"dev\",\"test\",\"local\",\"default\"}). Under"
        log_dim "prod they do NOT run, while Hibernate ddl-auto=update still does. Wiping a"
        log_dim "database here therefore yields an EMPTY schema — no users, no roles, no"
        log_dim "permissions — and every login fails with no obvious cause in the logs."
        log_dim ""
        log_dim "If you genuinely mean it (a fresh environment, a schema experiment):"
        log_dim "  ${COMMON_SH_DIR}/reset-db.sh --all --yes --allow-prod"
        log_dim "and then load real data by hand. It will not be seeded for you."
        exit 1
    fi
    log_warn "--allow-prod given: proceeding under the prod profile. The schema will come"
    log_warn "back EMPTY. You will need to populate it yourself."
fi

# ── the confirmation ────────────────────────────────────────────────────────
if [ "$ASSUME_YES" -ne 1 ]; then
    log_banner "DESTRUCTIVE: these databases will be ERASED"
    log_warn "  statefulsets : $(printf '%s ' "$TARGETS")"
    log_warn "  pvc pattern  : data-<sts>-0"
    log_warn "  effect       : schema is recreated empty, all rows lost, no undo"
    if [ "$NO_RESTART" -eq 0 ]; then
        log_dim "  after wiping, the owning service is restarted so its connection pool is"
        log_dim "  rebuilt against the new volume."
    fi
    printf '\n  Type RESET to confirm: '
    read -r answer
    if [ "$answer" != "RESET" ]; then
        log_info "aborted — nothing was changed"
        exit 0
    fi
fi

# ── the reset ───────────────────────────────────────────────────────────────
failures=0
for sts in $TARGETS; do
    owner="${sts#postgres-}"
    pvc="data-${sts}-0"

    log_step "resetting ${sts}"
    if ! kubectl get pvc "$pvc" -n "$NAMESPACE" >/dev/null 2>&1; then
        log_warn "  ${pvc} does not exist — the volume was never created, nothing to do"
    else
        # 1. stop the StatefulSet. Deleting a PVC under a running pod leaves the
        #    kubelet retrying a mount for a volume that no longer exists, and
        #    the new pod it creates immediately re-attaches.
        kubectl scale statefulset "$sts" -n "$NAMESPACE" --replicas=0 >/dev/null
        log_dim "  scaled ${sts} to 0"

        # Wait for the pod to actually go away; a scale-down is asynchronous.
        gone=0
        w=0
        while [ "$w" -lt 120 ]; do
            if ! kubectl get pod -n "$NAMESPACE" -l "app.kubernetes.io/name=${sts}" \
                --no-headers 2>/dev/null | grep -q '[^[:space:]]'; then
                gone=1
                break
            fi
            sleep 2
            w=$((w + 2))
        done
        if [ "$gone" -ne 1 ]; then
            log_err "  pod for ${sts} did not terminate in 120s — aborting to avoid a stuck mount"
            kubectl scale statefulset "$sts" -n "$NAMESPACE" --replicas=1 >/dev/null
            failures=$((failures + 1))
            continue
        fi
        log_dim "  pod gone"

        # 2. delete the volume
        kubectl delete pvc "$pvc" -n "$NAMESPACE" --wait=true
        log_dim "  deleted ${pvc}"

        # 3. bring it back. The StatefulSet's volumeClaimTemplates recreates
        #    the PVC, and the local-path provisioner (WaitForFirstConsumer)
        #    binds it when the pod is scheduled.
        kubectl scale statefulset "$sts" -n "$NAMESPACE" --replicas=1 >/dev/null
        log_dim "  scaled ${sts} back to 1"
    fi

    # 4. wait for Ready
    if kubectl wait --for=condition=Ready "pod/${sts}-0" -n "$NAMESPACE" --timeout=180s >/dev/null 2>&1; then
        log_ok "  ${sts}-0 Ready — an empty database accepting connections"
    else
        log_err "  ${sts}-0 did not become Ready in 180s"
        failures=$((failures + 1))
        continue
    fi

    # 5. restart the owning service so Hikari opens a fresh pool
    if [ "$NO_RESTART" -eq 0 ]; then
        if csv_contains "$owner" "$CSPH_SERVICES"; then
            kubectl rollout restart "deployment/${owner}" -n "$NAMESPACE" >/dev/null
            log_dim "  restarted deployment/${owner} (Hikari pool was pointing at the old volume)"
            if kubectl rollout status "deployment/${owner}" -n "$NAMESPACE" --timeout=240s >/dev/null 2>&1; then
                log_ok "  ${owner} rolled out"
            else
                log_warn "  ${owner} did not finish its rollout in 240s — check: kubectl -n ${NAMESPACE} logs deploy/${owner}"
            fi
        else
            log_dim "  no owning service found for ${sts} (the discovery-server has no database)"
        fi
    fi
done

log_banner "RESULT"
db_rows
printf '\n'
if [ "$failures" -gt 0 ]; then
    log_err "${failures} database(s) did not come back cleanly. Run: ${COMMON_SH_DIR}/debug.sh"
    exit 1
fi
log_ok "all requested database(s) reset. Schema recreated empty by Hibernate."
log_dim "Verify registration with: ${COMMON_SH_DIR}/up.sh  (or: kubectl -n ${NAMESPACE} get pods)"
