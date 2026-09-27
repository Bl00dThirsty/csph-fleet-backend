#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# up.sh — bring the whole CSPH GPL Fleet Platform up. Idempotent.
#
#   ./k8s/scripts/up.sh                    # everything, from nothing
#   ./k8s/scripts/up.sh --skip-build       # images already built locally
#   ./k8s/scripts/up.sh --rebuild          # force docker build even if present
#   ./k8s/scripts/up.sh --timeout 1200     # longer waits on a cold/slow server
#
# STEPS
#   1  create the kind cluster from k8s/kind-cluster.yaml, if it is missing
#   2  build the 11 images (each Dockerfile is multi-stage, so no host JDK or
#      ~/.m2 is required — a stock Ubuntu server with only Docker is enough)
#   3  `kind load` those images into the node's containerd
#   4  kubectl apply -k k8s/overlays/dev
#   5  wait for all 21 pods Ready
#   6  wait for all 10 applications REGISTERED WITH EUREKA
#   7  print a health summary
#
# ── STEP 6 IS THE ONE THAT MATTERS. DO NOT REMOVE IT. ──────────────────────
# Every one of the 10 JVM Deployments uses a tcpSocket readinessProbe. That
# probe passes the INSTANT the port is bound — before Spring finishes
# auto-configuring Hibernate, before the seeders run, and, critically, before
# the Eureka client has issued its first registration (the client's initial
# delay alone is ~30 s, plus the registry's own response time).
#
# So for roughly a minute after `kubectl apply` returns, the platform looks
# perfect and does not work:
#
#     kubectl get pods        ->  21/21 Running, all Ready
#     curl localhost:8080/... ->  503 "Unable to find instance for auth-service"
#
# This was a real, repeatedly misdiagnosed failure on this platform: up.ps1
# finished on pod readiness alone, printed "all pods Ready", and the operator
# spent the next twenty minutes looking for a database problem that did not
# exist. Pod readiness is necessary and NOT sufficient. Step 6 is the only
# thing that distinguishes "the containers started" from "the platform can
# serve a request", so up.sh refuses to declare success without it.
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail
# shellcheck source=./common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

SKIP_BUILD=0
REBUILD=""
TIMEOUT=900
EUREKA_TIMEOUT=420
EXPECTED_PODS=21

usage() {
    sed -n '3,11p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --skip-build)  SKIP_BUILD=1; shift ;;
        --rebuild)     REBUILD="--rebuild"; shift ;;
        --timeout)     TIMEOUT="${2:?--timeout needs seconds}"; shift 2 ;;
        --eureka-timeout) EUREKA_TIMEOUT="${2:?needs seconds}"; shift 2 ;;
        -h|--help)     usage 0 ;;
        *)             log_die "unknown argument: $1  (try --help)" ;;
    esac
done

require_tools_docker

cd "$REPO_ROOT"

# ── 1/7  cluster ────────────────────────────────────────────────────────────
# `kind get clusters` writes "No kind clusters found." to STDERR and exits
# non-zero when there is nothing, so stderr is discarded and stdout inspected.
log_step "1/7  kind cluster '${CLUSTER_NAME}'"
if cluster_exists; then
    log_ok "already exists — reusing it"
else
    log_info "creating from ${KIND_CONFIG} ..."
    # --wait 240s bounds the node's own control-plane startup instead of hanging
    # forever when the host is out of memory or the port mappings collide.
    kind create cluster --name "$CLUSTER_NAME" --config "$KIND_CONFIG" --wait 240s
    log_ok "created"
fi

kubectl config use-context "kind-${CLUSTER_NAME}" >/dev/null
kubectl wait --for=condition=Ready node --all --timeout=180s >/dev/null
log_ok "api server ready on context kind-${CLUSTER_NAME}"

# ── 2/7  images ─────────────────────────────────────────────────────────────
log_step "2/7  service images"
if [ "$SKIP_BUILD" -eq 1 ]; then
    log_info "--skip-build: assuming ${IMAGE_PREFIX}/gpl-*:${IMAGE_TAG} already exist locally"
else
    # shellcheck disable=SC2086  # REBUILD is intentionally "" or one flag
    "${COMMON_SH_DIR}/build-images.sh" $REBUILD
fi

# ── 3/7  kind load ──────────────────────────────────────────────────────────
# The node runs its own containerd; the host Docker daemon's images are
# invisible to it. Without this every pod sits in ErrImagePull forever, which
# is the single most common way a first `up.sh` on a new server appears to hang.
log_step "3/7  loading images into the node's containerd"
TAGS=""
for svc in $CSPH_SERVICES; do
    TAGS="${TAGS} ${IMAGE_PREFIX}/gpl-${svc}:${IMAGE_TAG}"
done
# shellcheck disable=SC2086  # TAGS is a deliberate word list
kind load docker-image --name "$CLUSTER_NAME" $TAGS 2>&1 \
    | grep -v 'already present' \
    | sed 's/^/  /' || true
log_ok "loaded $(printf '%s\n' "$CSPH_SERVICES" | wc -w | tr -d ' ') image(s)"

# ── 4/7  apply ──────────────────────────────────────────────────────────────
log_step "4/7  applying ${OVERLAY_DIR#"${REPO_ROOT}/"}"
# Rendered first so a malformed overlay fails here, loudly, instead of as a
# partially-applied namespace.
kubectl kustomize "$OVERLAY_DIR" > /dev/null
kubectl apply -k "$OVERLAY_DIR" | sed 's/^/  /'
log_ok "applied"

# ── 5/7  pods ───────────────────────────────────────────────────────────────
log_step "5/7  pod readiness"
if ! wait_for_pods "$TIMEOUT" "$EXPECTED_PODS"; then
    log_err "pods did not all become Ready. Run: ${COMMON_SH_DIR}/debug.sh"
    exit 1
fi

# ── 6/7  Eureka registration — the gate that pod readiness is not ──────────
log_step "6/7  Eureka service registration"
log_dim "tcpSocket probes pass before a service registers; the gateway answers 503 in that window."
if ! wait_for_eureka "$EUREKA_TIMEOUT"; then
    log_err "not every application registered, so the platform is NOT serving traffic yet."
    log_dim "The gateway will answer 503 for any route until this clears."
    log_dim "Run: ${COMMON_SH_DIR}/debug.sh"
    exit 1
fi

# ── 7/7  summary ────────────────────────────────────────────────────────────
log_step "7/7  health summary"
"${COMMON_SH_DIR}/status.sh"

log_ok "platform is up: 21 pods Ready, 10/10 applications registered, gateway on :${GATEWAY_HOST_PORT}"
log_dim "Follow the gateway: kubectl -n ${NAMESPACE} logs -f deploy/api-gateway"
log_dim "Diagnose:          ${COMMON_SH_DIR}/debug.sh"
