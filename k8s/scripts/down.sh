#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# down.sh — stop the platform, or destroy it.
#
#   ./k8s/scripts/down.sh              # stop the workloads, keep the data
#   ./k8s/scripts/down.sh --purge      # delete the whole cluster AND the volumes
#   ./k8s/scripts/down.sh --purge --yes   # same, no confirmation prompt
#
# WHAT "STOP" ACTUALLY DOES, AND WHY IT IS THE DEFAULT
#   Scaling the 9 Deployments to 0 stops the application tier and leaves the 9
#   Postgres StatefulSets running. That is the right default for a "take the
#   API offline but keep the data" operation: the PVCs survive, so bringing
#   the platform back does not re-run the Hibernate bootstrap and the seeders
#   against empty volumes. The cost is 9 Postgres pods still holding ~2 GiB of
#   RAM, which is why this is documented rather than silently done.
#
# WHY THE DEVS DO NOT USE `replicas: 0` FOR THE DATABASES
#   k8s/overlays/dev explicitly leaves the databases alone. Teardown is
#   `kubectl delete -k`, not a scaled-down overlay, because a scaled-down
#   overlay and a deleted overlay are two different pieces of state to keep
#   straight when something goes wrong at 3am.
#
# WHY --purge IS GATED BEHIND A CONFIRMATION
#   Deleting a kind cluster deletes the node container, and the local-path
#   provisioner's ReclaimPolicy is Delete, so every `data-*` PVC goes with it.
#   That is nine databases of real data. The prompt is not ceremony.
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail
# shellcheck source=./common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

PURGE=0
ASSUME_YES=0

usage() {
    sed -n '3,8p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --purge)  PURGE=1; shift ;;
        --yes|-y) ASSUME_YES=1; shift ;;
        -h|--help) usage 0 ;;
        *) log_die "unknown argument: $1  (try --help)" ;;
    esac
done

require_tools_cluster
cd "$REPO_ROOT"

if ! cluster_exists; then
    log_warn "no kind cluster named '${CLUSTER_NAME}' — nothing to do."
    exit 0
fi
kubectl config use-context "kind-${CLUSTER_NAME}" >/dev/null 2>&1 || true

# ── purge: the whole cluster ────────────────────────────────────────────────
if [ "$PURGE" -eq 1 ]; then
    if [ "$ASSUME_YES" -ne 1 ]; then
        log_banner "DESTRUCTIVE: this deletes the cluster and every database in it"
        log_warn "  cluster : ${CLUSTER_NAME}"
        log_warn "  volumes : 9 Postgres PVCs (data-postgres-*-0), reclaim policy Delete"
        log_warn "  effect  : ALL application data is destroyed and not recoverable"
        printf '\n  Type the cluster name to confirm: '
        read -r answer
        if [ "$answer" != "$CLUSTER_NAME" ]; then
            log_info "aborted — nothing was changed"
            exit 0
        fi
    fi

    log_step "deleting cluster '${CLUSTER_NAME}'"
    kind delete cluster --name "$CLUSTER_NAME"
    log_ok "cluster, its containerd images and all 9 PVCs are gone"
    log_dim "Verify no node container lingers:  docker ps -a --filter name=${CLUSTER_NAME}"
    exit 0
fi

# ── stop: the application tier only ─────────────────────────────────────────
log_step "scaling the application tier to 0 (databases keep running, data is safe)"

# Enumerated live rather than from a hard-coded list, so a Deployment added to
# the base later is covered without editing this script. mailpit is excluded:
# it is the SMTP sink, it holds no application data, and leaving it up costs
# 128 MiB and gives whoever is still testing a working inbox.
DEPLOYS="$(kubectl get deploy -n "$NAMESPACE" -o name 2>/dev/null \
    | grep -v 'deploy/mailpit' || true)"

if [ -z "$DEPLOYS" ]; then
    log_warn "no Deployments found in ${NAMESPACE} — is the platform applied?"
else
    # shellcheck disable=SC2086  # DEPLOYS is a deliberate resource list
    kubectl scale -n "$NAMESPACE" $DEPLOYS --replicas=0 | sed 's/^/  /'
    log_ok "scaled 0:"
    printf '%s\n' "$DEPLOYS" | sed 's/^/       /'
fi

log_step "waiting for the application pods to terminate"
deadline=$(( $(date +%s) + 180 ))
while [ "$(date +%s)" -lt "$deadline" ]; do
    left="$(kubectl get pods -n "$NAMESPACE" \
        -l 'app.kubernetes.io/component in (application,infrastructure)' \
        --no-headers 2>/dev/null | grep -c '[^[:space:]]' || true)"
    [ "$left" -eq 0 ] && break
    sleep 3
done
left="$(kubectl get pods -n "$NAMESPACE" -l 'app.kubernetes.io/component=application' \
    --no-headers 2>/dev/null | grep -c '[^[:space:]]' || true)"
if [ "$left" -eq 0 ]; then
    log_ok "no application pods left running"
else
    log_warn "${left} application pod(s) still terminating:"
    kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/component=application | sed 's/^/  /'
fi

# ── what is still up ────────────────────────────────────────────────────────
log_step "still running (databases and the SMTP sink — deliberately preserved)"
kubectl get pods -n "$NAMESPACE" \
    -l app.kubernetes.io/component=database 2>/dev/null | sed 's/^/  /' || true
kubectl get pods -n "$NAMESPACE" \
    -l app.kubernetes.io/name=mailpit 2>/dev/null | sed 's/^/  /' || true

log_dim "Start again with:  ${COMMON_SH_DIR}/up.sh --skip-build"
log_dim "Destroy the data: ${COMMON_SH_DIR}/down.sh --purge"
