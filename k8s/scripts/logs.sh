#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# logs.sh — follow or filter the logs of any workload.
#
#   ./k8s/scripts/logs.sh auth-service                 # last 200 lines
#   ./k8s/scripts/logs.sh auth-service --follow        # stream
#   ./k8s/scripts/logs.sh auth-service --errors        # only the interesting lines
#   ./k8s/scripts/logs.sh --all --follow               # every pod at once
#   ./k8s/scripts/logs.sh api-gateway --since 30m
#   ./k8s/scripts/logs.sh auth-service --previous       # the crashed instance
#   ./k8s/scripts/logs.sh --list                        # what can be tailed
#
# THE TWO FLAGS THAT ACTUALLY MATTER HERE
#   --previous  On a CrashLoopBackOff pod, `kubectl logs` shows the CURRENT
#              attempt's first lines. The cause of the crash is one restart
#              ago. This flag flips straight to it, which is the single most
#              useful thing this script does.
#   --errors    The default error filter for a Spring Boot service. It is
#              tuned to the shapes this platform actually emits: Spring's
#              "APPLICATION FAILED TO START", the Caused-by chain, HikariCP
#              pool errors, and the gateway's "Unable to find instance".
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail
# shellcheck source=./common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

TARGET=""
ALL=0
FOLLOW=0
PREVIOUS=0
ERRORS=0
SINCE=""
TAIL=200
LIST=0

# The error filter. Deliberately broad on the JVM vocabulary and specific on
# the platform's own failure strings, so it stays quiet during normal
# operation — a filter that matches everything is the same as no filter.
ERROR_PATTERN="APPLICATION FAILED TO START|Caused by:|ERROR|Exception|Error creating bean|Failed to (configure|initialize)|HikariPool|Connection refused|Communications link failure|UnknownHostException|Unable to find instance|no instances available|BeanCreationException|UnsatisfiedDependency|OutOfMemoryError|NoClassDefFoundError|ClassNotFoundException|could not be found|Access denied|401 Unauthorized|403 Forbidden|404 Not Found|500 Internal"

usage() {
    sed -n '3,17p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit 0
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --all|-a)     ALL=1; shift ;;
        --follow|-f)  FOLLOW=1; shift ;;
        --previous|-p) PREVIOUS=1; shift ;;
        --errors|-e)  ERRORS=1; shift ;;
        --since)      SINCE="${2:?--since needs a duration like 30m or 2h}"; shift 2 ;;
        --tail)       TAIL="${2:?--tail needs a line count}"; shift 2 ;;
        --list|-l)    LIST=1; shift ;;
        -h|--help)    usage ;;
        -*)           log_die "unknown option: $1  (try --help)" ;;
        *)            TARGET="$1"; shift ;;
    esac
done

require_tools_cluster
cd "$REPO_ROOT"

if ! init_cluster_context; then
    log_die "kind cluster '${CLUSTER_NAME}' is not available.  kind get clusters"
fi

# ── --list ──────────────────────────────────────────────────────────────────
if [ "$LIST" -eq 1 ]; then
    log_banner "LOGGABLE WORKLOADS  (namespace ${NAMESPACE})"
    printf '  %-24s %-8s %-6s %s\n' "WORKLOAD" "KIND" "PORTS" "LOG HINT"
    printf '  %s\n' "----------------------------------------------------------------------"
    for svc in $CSPH_SERVICES; do
        printf '  %-24s %-8s %-6s %s\n' "$svc" "deploy" "-" \
            "kubectl -n ${NAMESPACE} logs -f deploy/${svc}"
    done
    printf '  %-24s %-8s %-6s %s\n' "mailpit" "deploy" "-" \
        "kubectl -n ${NAMESPACE} logs -f deploy/mailpit"
    for db in $CSPH_DATABASES; do
        printf '  %-24s %-8s %-6s %s\n' "$db" "sts" "-" \
            "kubectl -n ${NAMESPACE} logs -f statefulset/${db}"
    done
    exit 0
fi

# ── build the kubectl log invocation ───────────────────────────────────────
# `kubectl logs deploy/x` is the right unit for a single-replica Deployment and
# the wrong one the moment there are two: it silently follows one arbitrary
# pod. So when --all is asked for, or a workload has more than one replica, the
# pod list is resolved first and every pod gets its own labelled stream.
build_args() {
    LOG_ARGS="--tail=${TAIL}"
    [ -n "$SINCE" ] && LOG_ARGS="${LOG_ARGS} --since=${SINCE}"
    [ "$FOLLOW" -eq 1 ] && LOG_ARGS="${LOG_ARGS} --follow"
    [ "$PREVIOUS" -eq 1 ] && LOG_ARGS="${LOG_ARGS} --previous"
    [ "$ERRORS" -eq 1 ] && LOG_ARGS="${LOG_ARGS} --all-containers"
    export LOG_ARGS
}

build_args

# ── --all: everything ───────────────────────────────────────────────────────
if [ "$ALL" -eq 1 ]; then
    if [ "$FOLLOW" -eq 1 ]; then
        log_info "streaming every container in ${NAMESPACE} (ctrl-c to stop)"
        # shellcheck disable=SC2086
        kubectl logs -n "$NAMESPACE" --all-containers --prefix --follow --tail="$TAIL" || true
        exit 0
    fi
    log_banner "LOGS — every pod in ${NAMESPACE}"
    for pod in $(kubectl get pods -n "$NAMESPACE" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null); do
        log_step "$pod"
        # shellcheck disable=SC2086
        kubectl logs -n "$NAMESPACE" "$pod" --tail="$TAIL" 2>/dev/null | sed 's/^/  | /' || true
    done
    exit 0
fi

# ── a target is required from here ──────────────────────────────────────────
if [ -z "$TARGET" ]; then
    log_die "name a workload, or use --all / --list.
  workloads: ${CSPH_SERVICES} mailpit
  databases: ${CSPH_DATABASES}"
fi

# Accept a StatefulSet name, a Deployment name, a bare pod name, or a label
# selector fragment, and resolve each to pods.
PODS=""
if kubectl get deploy "$TARGET" -n "$NAMESPACE" >/dev/null 2>&1; then
    PODS="$(kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/name=${TARGET}" \
        -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null || true)"
elif kubectl get statefulset "$TARGET" -n "$NAMESPACE" >/dev/null 2>&1; then
    PODS="$(kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/name=${TARGET}" \
        -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null || true)"
elif kubectl get pod "$TARGET" -n "$NAMESPACE" >/dev/null 2>&1; then
    PODS="$TARGET"
else
    log_die "'${TARGET}' is not a Deployment, StatefulSet or Pod in ${NAMESPACE}.
  Run: ${COMMON_SH_DIR}/logs.sh --list"
fi

[ -n "$PODS" ] || log_die "no pods for '${TARGET}'."

pod_count="$(printf '%s\n' "$PODS" | grep -c '[^[:space:]]' || true)"

# ── header ──────────────────────────────────────────────────────────────────
log_banner "LOGS  ${TARGET}  (${pod_count} pod(s) in ${NAMESPACE})"
log_dim "kubectl -n ${NAMESPACE} logs ${TARGET} ${LOG_ARGS}"

if [ "$PREVIOUS" -eq 1 ]; then
    log_warn "--previous: showing the PREVIOUS container. On a CrashLoopBackOff pod"
    log_warn "this is where the actual cause of the crash is written."
fi
if [ "$ERRORS" -eq 1 ]; then
    log_dim "--errors: filtering to the failure vocabulary of this platform."
fi

emit() {
    local pod="$1"
    if [ "$pod_count" -gt 1 ] || [ "$ALL" -eq 1 ]; then
        log_step "$pod"
    fi
    if [ "$ERRORS" -eq 1 ]; then
        # grep exits 1 when nothing matches, which is a normal outcome here
        # and must not abort the loop.
        # shellcheck disable=SC2086
        kubectl logs -n "$NAMESPACE" "$pod" $LOG_ARGS 2>&1 \
            | sed 's/^/  | /' \
            | grep -Ei "$ERROR_PATTERN" \
            || log_dim "(no matching error lines)"
    else
        # shellcheck disable=SC2086
        kubectl logs -n "$NAMESPACE" "$pod" $LOG_ARGS 2>&1 | sed 's/^/  | /' || true
    fi
}

# ── follow mode ─────────────────────────────────────────────────────────────
if [ "$FOLLOW" -eq 1 ]; then
    if [ "$pod_count" -eq 1 ]; then
        if [ "$ERRORS" -eq 1 ]; then
            # shellcheck disable=SC2086
            kubectl logs -n "$NAMESPACE" "$(printf '%s' "$PODS" | head -1)" $LOG_ARGS 2>&1 \
                | grep --line-buffered -Ei "$ERROR_PATTERN" || true
        else
            # shellcheck disable=SC2086
            kubectl logs -n "$NAMESPACE" "$(printf '%s' "$PODS" | head -1)" $LOG_ARGS || true
        fi
    else
        # >1 pod and --follow: `kubectl logs` cannot follow a Deployment and
        # stream every replica, so each pod gets its own prefixed stream and
        # the streams are interleaved. Without the prefix they are unreadable.
        log_info "following ${pod_count} pods; streams are interleaved and labelled"
        printf '%s\n' "$PODS" | while read -r pod; do
            [ -n "$pod" ] || continue
            # shellcheck disable=SC2086
            ( kubectl logs -n "$NAMESPACE" "$pod" $LOG_ARGS 2>&1 | sed "s/^/[$pod] /" ) &
        done
        wait
    fi
    exit 0
fi

# ── one-shot ────────────────────────────────────────────────────────────────
printf '%s\n' "$PODS" | while read -r pod; do
    [ -n "$pod" ] || continue
    emit "$pod"
done
