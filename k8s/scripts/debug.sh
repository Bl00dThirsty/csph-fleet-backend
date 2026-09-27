#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# debug.sh — diagnose a broken platform in one command.
#
#   ./k8s/scripts/debug.sh                        # full report
#   ./k8s/scripts/debug.sh --quick                # skip the log dumps
#   ./k8s/scripts/debug.sh --service auth-service # one workload, in full
#   ./k8s/scripts/debug.sh --lines 200            # longer log excerpts
#
# WHY THIS SCRIPT EXISTS
#   Two failures on this platform are invisible to `kubectl get pods`:
#
#   1. A Spring Boot pod whose tcpSocket probe passed, whose port is bound, and
#      which is NOT in the Eureka registry. Every `lb://` route through the
#      gateway answers 503 "Unable to find instance for auth-service" while the
#      status page says 21/21 Running. This is why up.sh gates on registration
#      and not on readiness.
#   2. A CrashLoopBackOff whose cause is in the PREVIOUS container's log.
#      `kubectl logs <pod>` on a restarting container shows the new attempt's
#      first lines, or nothing at all. The real stack trace — the
#      UnsatisfiedDependencyException, the "Killed" line from an OOM, the
#      connection refused to postgres — is in `--previous`.
#
# WHAT IT PRINTS, IN ORDER
#   1  cluster / node / namespace state, replicas, recent events
#   2  the unhealthy-pod list
#   3  per unhealthy pod: image + pull policy, last N log lines, the PREVIOUS
#      container's error lines, the probe verdict from `kubectl describe`, the
#      container waiting reason
#   4  all 9 Postgres instances, each probed with pg_isready inside its own pod
#   5  Eureka: how many of the 10 applications are registered, and which are not
#   6  all 22 host ports
#   7  the gateway's own logs, filtered for 4xx/5xx and Eureka resolution errors
#   8  a MOST LIKELY CAUSE line
#
# EXIT CODE
#   0  healthy        1  unhealthy or unreachable        2  bad usage
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail
# shellcheck source=./common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

QUICK=0
FOCUS=""
LOG_LINES=40
PREV_LINES=20
# Section 7 only looks at the RECENT gateway log. Without a window it reads the
# whole buffer and reports the connection-refused noise every replica emits at
# startup, while discovery-server is still coming up, as if it were a live
# fault. That false positive was caught on a completely healthy cluster and it
# is exactly the kind of thing that teaches an operator to ignore the report.
GW_WINDOW="${CSPH_GW_LOG_WINDOW:-10m}"

while [ "$#" -gt 0 ]; do
    case "$1" in
        --quick)   QUICK=1; shift ;;
        --service) FOCUS="${2:?--service needs a name}"; shift 2 ;;
        --lines)   LOG_LINES="${2:?needs a count}"; shift 2 ;;
        --window)  GW_WINDOW="${2:?--window needs a duration like 10m}"; shift 2 ;;
        -h|--help) sed -n '3,8p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) log_die "unknown argument: $1  (try --help)" ;;
    esac
done

# ── the cause list ──────────────────────────────────────────────────────────
# Findings are appended with a PRIORITY. Section 8 prints the lowest number,
# because not all evidence is equally strong and a report that leads with the
# weakest signal is worse than no report:
#
#   10 no cluster at all              45 a pod is in a bad state
#   20 node NotReady                  46 the previous container says why
#   30 namespace / pods missing       50 a database is down
#   40 replicas not available         55 an application is unregistered
#                                     60 a host port is unreachable
#                                     70 the gateway logged 4xx/5xx  (weak:
#                                        time-sensitive, and often just
#                                        startup noise — hence --window)
#
# The structural findings (10-60) are facts about the present. The log-based
# ones (46, 70) are inferences from text that may be minutes old, so they only
# become the headline when nothing structural is wrong.
#
# Initialised empty because `set -u` is on: a healthy run adds nothing, and
# referencing an unset CAUSES at that point would abort a run that had in fact
# found the platform perfectly healthy.
CAUSES=""
add_cause() { CAUSES="${CAUSES}|${1}|${2}"; }

# ── helpers, defined before first use ───────────────────────────────────────
# Bash reads a script top to bottom: a function called above its definition
# does not exist yet. Everything section 3 needs lives here.

# describe_probe_verdict <pod>
# The probe outcomes from `kubectl describe`, which is the only place the
# kubelet's own accounting appears ("Readiness probe failed: connection
# refused", "Liveness probe failed", "Startup probe failed").
describe_probe_verdict() {
    local pod="$1" verdict
    verdict="$(kubectl describe pod "$pod" -n "$NAMESPACE" 2>/dev/null \
        | grep -E 'Readiness|Liveness|Startup|State:|Reason:|Message:|Exit Code|Started:|Finished:' \
        | sed 's/^  */  /' || true)"
    if [ -n "$verdict" ]; then
        printf '  %s--- probe + container state (from kubectl describe) ---%s\n' "$C_DIM" "$C_RESET"
        printf '%s\n' "$verdict"
    fi
}

# dump_pod_detail <label> <pod-name>
dump_pod_detail() {
    local label="$1" pod="$2"
    [ -n "$pod" ] || return 0

    local image policy reason restarts out prev_hits prev_window gwhits has5xx

    image="$(kubectl get pod "$pod" -n "$NAMESPACE" \
        -o jsonpath='{.spec.containers[*].image}' 2>/dev/null || true)"
    policy="$(kubectl get pod "$pod" -n "$NAMESPACE" \
        -o jsonpath='{.spec.containers[*].imagePullPolicy}' 2>/dev/null || true)"
    restarts="$(kubectl get pod "$pod" -n "$NAMESPACE" \
        -o jsonpath='{.status.containerStatuses[*].restartCount}' 2>/dev/null || true)"
    reason="$(kubectl get pod "$pod" -n "$NAMESPACE" \
        -o jsonpath='{range .status.containerStatuses[*]}{.state.waiting.reason}{" "}{.state.waiting.message}{"\n"}{end}' 2>/dev/null || true)"

    log_step "pod ${label}"
    printf '  %-12s %s\n' "image"      "${image:-<unknown>}"
    printf '  %-12s %s\n' "pullPolicy" "${policy:-<unknown>}"
    printf '  %-12s %s\n' "restarts"   "${restarts:-0}"

    if [ -n "$(printf '%s' "$reason" | tr -d ' \n')" ]; then
        printf '  %-12s %s%s%s\n' "waiting" "$C_YELLOW" "$(printf '%s' "$reason" | tr '\n' ' ')" "$C_RESET"
        case "$reason" in
            *ImagePullBackOff*|*ErrImagePull*)
                add_cause 45 "${label}: the image cannot be pulled (${image}). Either it was never \`kind load\`ed into the node, or the local tag is missing. Fix: ${COMMON_SH_DIR}/build-images.sh then ${COMMON_SH_DIR}/up.sh --skip-build" ;;
            *CrashLoopBackOff*)
                add_cause 45 "${label}: the container starts and then exits. The stack trace is in the PREVIOUS container block below — read that, not the log above it." ;;
            *CreateContainerConfigError*)
                add_cause 45 "${label}: a referenced ConfigMap or Secret is missing, so the container cannot be created at all. Check: kubectl -n ${NAMESPACE} get cm,secret" ;;
            *InvalidImageName*)
                add_cause 45 "${label}: the image reference is malformed (${image})." ;;
            *ContainerCreating*)
                add_cause 45 "${label}: the container has not started yet. If this persists, the image pull is stalling — check the node's containerd and the host's free disk." ;;
        esac
    fi

    printf '\n  %s--- last %s log lines ---%s\n' "$C_DIM" "$LOG_LINES" "$C_RESET"
    if out="$(kubectl logs "$pod" -n "$NAMESPACE" --tail="$LOG_LINES" --all-containers 2>/dev/null)"; then
        if [ -n "$out" ]; then
            printf '%s\n' "$out" | sed 's/^/  | /'
        else
            printf '  %s(no output — the container has not written anything yet)%s\n' "$C_DIM" "$C_RESET"
        fi
    else
        printf '  %s(cannot read logs — the container is not running)%s\n' "$C_DIM" "$C_RESET"
    fi

    # ── THE PREVIOUS CONTAINER ──────────────────────────────────────────────
    # This block is the reason the script exists. For a CrashLoopBackOff the
    # current container's log is either empty or shows the new attempt's first
    # lines; the actual cause is one restart ago.
    case "${restarts:-0}" in
        ''|*[!0-9]*) : ;;
        0) : ;;
        *)
            # Read 10x the requested excerpt from the previous container, then
            # filter that down to PREV_LINES error lines: a Spring stack trace
            # buries its "Caused by" several hundred lines down, so tailing only
            # PREV_LINES raw lines would routinely miss the actual cause.
            prev_window=$(( PREV_LINES * 10 ))
            printf '\n  %s--- PREVIOUS container: the real cause (last %s error lines) ---%s\n' \
                "$C_BOLD$C_YELLOW" "$PREV_LINES" "$C_RESET"
            out="$(kubectl logs "$pod" -n "$NAMESPACE" --previous --tail="$prev_window" --all-containers 2>/dev/null || true)"
            if [ -n "$out" ]; then
                prev_hits="$(printf '%s\n' "$out" \
                    | grep -Ei 'error|exception|caused by|failed|refused|denied|timed out|unable|not authorized|no such|does not exist|OutOfMemory|Killed' \
                    | tail -"$PREV_LINES" || true)"
                if [ -n "$prev_hits" ]; then
                    printf '%s\n' "$prev_hits" | sed 's/^/  ! /'
                else
                    printf '  %s(no error line matched — the crash is silent; see the events above)%s\n' "$C_DIM" "$C_RESET"
                fi
                printf '\n  %s--- PREVIOUS container: last %s lines verbatim ---%s\n' \
                    "$C_DIM" "$PREV_LINES" "$C_RESET"
                printf '%s\n' "$out" | tail -"$PREV_LINES" | sed 's/^/  > /'
                if printf '%s' "$out" | grep -qi 'Connection refused.*5432\|could not open JDBC\|Communications link failure'; then
                    add_cause 46 "${label}: the previous container could not reach Postgres. A Spring Boot service cannot start without its datasource — fix the database (section 4) before touching this service."
                elif printf '%s' "$out" | grep -qi 'UnknownHostException\|Connection refused.*8761\|DiscoveryClient.*ERROR'; then
                    add_cause 46 "${label}: the previous container could not reach discovery-server:8761, so it never registered and never will. Check section 4/5."
                elif printf '%s' "$out" | grep -qi 'OutOfMemoryError\|Killed'; then
                    add_cause 46 "${label}: the previous container ran out of memory and was OOMKilled. Raise the memory limit (k8s/base/services.yaml) or lower MaxRAMPercentage in jvm-config.yaml."
                fi
            else
                printf '  (no previous container log available)\n'
            fi
            ;;
    esac

    describe_probe_verdict "$pod"
    printf '\n'
}

require_tools_probe
cd "$REPO_ROOT"

# ═══ 1  cluster / node / namespace ═════════════════════════════════════════
log_banner "1. CLUSTER / NODE / NAMESPACE"
if ! cluster_exists; then
    log_err "kind cluster '${CLUSTER_NAME}' does not exist."
    log_dim "  kind get clusters"
    log_dim "  ${COMMON_SH_DIR}/up.sh"
    printf '\n%sMOST LIKELY CAUSE%s\n' "$C_BOLD$C_RED" "$C_RESET"
    log_err "no cluster. Nothing further in this report can be true."
    exit 1
fi
init_cluster_context
log_ok "cluster  ${CLUSTER_NAME}   (context kind-${CLUSTER_NAME})"
kubectl cluster-info 2>/dev/null | sed 's/^/        /' || true

log_step "node"
kubectl get nodes -o wide 2>/dev/null | sed 's/^/  /' || log_warn "kubectl get nodes failed"
NOTREADY_NODES="$(kubectl get nodes --no-headers 2>/dev/null | grep -Ev ' Ready ' || true)"
if [ -n "$NOTREADY_NODES" ]; then
    printf '%s' "$NOTREADY_NODES" | sed "s/^/  ${C_RED}/;s/$/${C_RESET}/"
    add_cause 20 "the node is NotReady. Every pod is Pending or Evicted until Docker is back — fix the node, not the workloads."
    log_dim "  docker ps -a | grep ${CLUSTER_NAME}"
    log_dim "  docker start ${CLUSTER_NAME}-control-plane"
else
    log_ok "node Ready"
fi

log_step "namespace ${NAMESPACE}"
kubectl get namespace "$NAMESPACE" -o wide 2>/dev/null | sed 's/^/  /' \
    || add_cause 30 "namespace '${NAMESPACE}' does not exist — the overlay has never been applied"
POD_TOTAL="$(kubectl get pods -n "$NAMESPACE" --no-headers 2>/dev/null | grep -c '[^[:space:]]' || true)"
log_dim "${POD_TOTAL} pod(s) present (21 expected: 10 JVM + 9 Postgres + mailpit)"
if [ "$POD_TOTAL" -lt 21 ]; then
    add_cause 30 "only ${POD_TOTAL}/21 pods exist — the apply is incomplete or a controller has not caught up"
fi

log_step "workload replicas (desired/ready)"
kubectl get deploy,sts -n "$NAMESPACE" 2>/dev/null | sed 's/^/  /' || true
NOTREADY_REPLICAS="$(kubectl get deploy,sts -n "$NAMESPACE" --no-headers 2>/dev/null \
    | awk '{ split($2, a, "/"); if (a[1] != a[2]) print }' || true)"
if [ -n "$NOTREADY_REPLICAS" ]; then
    log_warn "not fully available:"
    printf '%s\n' "$NOTREADY_REPLICAS" | sed 's/^/       /'
    add_cause 40 "a Deployment/StatefulSet has fewer ready replicas than desired"
fi

log_step "recent events (last 20)"
kubectl get events -n "$NAMESPACE" --sort-by=.lastTimestamp 2>/dev/null \
    | tail -20 | sed 's/^/  /' || true

# ═══ 2  unhealthy pods ═════════════════════════════════════════════════════
log_banner "2. UNHEALTHY PODS"
UNHEALTHY="$(kubectl get pods -n "$NAMESPACE" --no-headers 2>/dev/null \
    | grep -Ev ' [0-9]+/[0-9]+ +Running +0 ' | grep -Ev 'Completed' || true)"

if [ -z "$UNHEALTHY" ]; then
    log_ok "no pod in a CrashLoop / ImagePull / Pending / Error state"
    NOTREADY_ONLY="$(kubectl get pods -n "$NAMESPACE" --no-headers 2>/dev/null \
        | grep -E ' [0-9]+/[0-9]+ +' | grep -vE ' [0-9]+/[0-9]+ +Running +0 ' || true)"
    if [ -n "$NOTREADY_ONLY" ]; then
        log_warn "but these are Running and NOT Ready (their probe is failing):"
        printf '%s\n' "$NOTREADY_ONLY" | sed "s/^/       ${C_YELLOW}/;s/$/${C_RESET}/"
        add_cause 45 "a pod is Running but not Ready, so no traffic reaches it. The probe verdict is in section 3."
    fi
else
    printf '%s' "$UNHEALTHY" | sed "s/^/  ${C_RED}/;s/$/${C_RESET}/"
    add_cause 45 "a pod is in a non-Running state; its own logs and probe verdict are in section 3"
fi

# ═══ 3  per-pod deep dive ══════════════════════════════════════════════════
log_banner "3. POD DETAIL"
if [ -n "$FOCUS" ]; then
    csv_contains "$FOCUS" "$CSPH_SERVICES $CSPH_DATABASES mailpit" \
        || log_warn "'${FOCUS}' is not a known workload — dumping whatever the label matches anyway."
    focus_pod="$(kubectl get pods -n "$NAMESPACE" \
        -l "app.kubernetes.io/name=${FOCUS}" \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
    if [ -n "$focus_pod" ]; then
        dump_pod_detail "$FOCUS" "$focus_pod"
    else
        log_err "no pod matches app.kubernetes.io/name=${FOCUS}"
    fi
elif [ -n "$UNHEALTHY" ]; then
    printf '%s\n' "$UNHEALTHY" | while read -r pod _rest; do
        [ -n "$pod" ] || continue
        if [ "$QUICK" -eq 1 ]; then
            log_step "pod ${pod} (--quick: probe verdict only)"
            describe_probe_verdict "$pod"
        else
            dump_pod_detail "$pod" "$pod"
        fi
    done
elif [ "$QUICK" -eq 1 ]; then
    log_info "no unhealthy pods and --quick was given, so there is nothing to dump."
else
    log_info "no unhealthy pods, so there is nothing to dump."
    log_dim "To read one workload anyway:  ${COMMON_SH_DIR}/debug.sh --service auth-service"
fi

# ═══ 4  postgres ═══════════════════════════════════════════════════════════
log_banner "4. POSTGRES HEALTH  (pg_isready inside each of the 9 pods)"
if [ "$QUICK" -eq 1 ]; then
    log_info "--quick: skipped"
else
    dbs_down=""
    for pair in $CSPH_DB_NAMES; do
        sts="$(printf '%s' "$pair" | cut -d: -f1)"
        db="$(printf '%s' "$pair" | cut -d: -f2)"
        pod="$(kubectl get pod -n "$NAMESPACE" -l "app.kubernetes.io/name=${sts}" \
            -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
        if [ -z "$pod" ]; then
            printf '  %-18s %-22s %sNO POD%s\n' "$sts" "$db" "$C_RED" "$C_RESET"
            dbs_down="${dbs_down} ${sts}"
            add_cause 50 "${sts}: no pod, so ${db} is unreachable and the service that needs it cannot start"
            continue
        fi
        # pg_isready runs INSIDE the pod with the same database name the
        # readiness probe uses, so a pass is not a guess and a failure is the
        # database's own opinion rather than an inference.
        out="$(kubectl exec -n "$NAMESPACE" "$pod" -c postgres -- \
            pg_isready -U postgres -d "$db" 2>&1 || true)"
        case "$out" in
            *"accepting connections"*)
                printf '  %-18s %-22s %sUP%s\n' "$sts" "$db" "$C_GREEN" "$C_RESET" ;;
            *)
                printf '  %-18s %-22s %sDOWN%s  %s\n' "$sts" "$db" "$C_RED" "$C_RESET" "$out"
                dbs_down="${dbs_down} ${sts}" ;;
        esac
    done
    printf '\n'
    if [ -n "$dbs_down" ]; then
        log_err "databases down:${dbs_down}"
        add_cause 50 "Postgres down ->${dbs_down}. A Spring Boot service cannot open its datasource, so its pod CrashLoops and the gateway answers 503 for that route. Fix the database first — the services will recover on their own."
    else
        log_ok "all 9 databases accepting connections"
    fi

    log_step "PVC usage — a full volume fails writes, not connections, and is therefore quiet"
    kubectl get pvc -n "$NAMESPACE" 2>/dev/null | sed 's/^/  /' || true
fi

# ═══ 5  Eureka ═════════════════════════════════════════════════════════════
log_banner "5. EUREKA REGISTRATION"
raw_path="/api/v1/namespaces/${NAMESPACE}/services/http:discovery-server:8761/proxy/eureka/apps"
body="$(kubectl get --raw "$raw_path" 2>/dev/null || true)"
eureka_missing=""
eureka_present=0
if [ -z "$body" ]; then
    log_err "the registry is unreadable at ${raw_path}"
    log_dim "  kubectl -n ${NAMESPACE} get pods -l app.kubernetes.io/name=discovery-server"
    add_cause 55 "discovery-server is down or its Service is missing. With no registry, nothing registers and every gateway route 503s."
else
    printf '  %-26s %s\n' "APPLICATION" "STATE"
    printf '  %s\n' "----------------------------------------"
    for want in $EUREKA_APPS; do
        pattern="(<name>${want}</name>|\"name\"[[:space:]]*:[[:space:]]*\"${want}\")"
        if printf '%s' "$body" | grep -qE "$pattern"; then
            printf '  %-26s %sregistered%s\n' "$want" "$C_GREEN" "$C_RESET"
            eureka_present=$((eureka_present + 1))
        else
            printf '  %-26s %sMISSING%s\n' "$want" "$C_RED" "$C_RESET"
            eureka_missing="${eureka_missing} ${want}"
        fi
    done
    printf '\n'
    if [ -n "$eureka_missing" ]; then
        log_err "${eureka_present}/10 registered. Missing:${eureka_missing}"
        add_cause 55 "unregistered ->${eureka_missing}. Their pods are Ready — the tcpSocket probe passes the moment the port binds — but the gateway resolves \`lb://<name>\` through Eureka, so it answers 503. This is the one failure a pod-status table reports as green."
        log_dim "  In the missing service's own log, look for a registration error,"
        log_dim "  'Connect to discovery-server:8761 refused', or 'UnknownHostException'."
    else
        log_ok "10/10 registered — every \`lb://\` route the gateway can resolve"
    fi
fi

# ═══ 6  host ports ═════════════════════════════════════════════════════════
log_banner "6. HOST PORT PROBES  (22 mappings from k8s/kind-cluster.yaml)"
printf '  %-6s %-24s %-6s %s\n' "PORT" "WORKLOAD" "CODE" "STATE"
printf '  %s\n' "---------------------------------------------------------------------"
unreachable=""
for row in $CSPH_HOST_PORTS; do
    port="$(printf '%s' "$row" | cut -d: -f1)"
    name="$(printf '%s' "$row" | cut -d: -f2)"
    kind="$(printf '%s' "$row" | cut -d: -f3)"
    path="$(printf '%s' "$row" | cut -d: -f4-)"

    if [ "$kind" = "app" ]; then
        code="$(http_probe "http://127.0.0.1:${port}${path}" 5 || printf 'ERR')"
        state="$(classify_code "$code")"
    else
        # Postgres and SMTP do not speak HTTP; a bare TCP connect is the
        # honest test for "is anything listening on the host port".
        if tcp_open 127.0.0.1 "$port" 3; then
            code="open"; state="OK"
        else
            code="----"; state="BAD"
        fi
    fi

    case "$state" in
        OK)    printf '  %-6s %-24s %-6s %saccepting%s\n'    "$port" "$name" "$code" "$C_GREEN"  "$C_RESET" ;;
        ALIVE) printf '  %-6s %-24s %-6s %sbounded%s\n'         "$port" "$name" "$code" "$C_YELLOW" "$C_RESET" ;;
        *)     printf '  %-6s %-24s %-6s %sUNREACHABLE%s\n'  "$port" "$name" "$code" "$C_RED"    "$C_RESET" ;;
    esac
    if [ "$state" = "BAD" ]; then
        unreachable="${unreachable} ${name}"
    fi
done
printf '\n'
if [ -n "$unreachable" ]; then
    log_err "unreachable:${unreachable}"
    log_dim "A host port is unreachable when the pod is not Ready, the extraPortMapping is"
    log_dim "missing from k8s/kind-cluster.yaml, or another host process owns the port."
    add_cause 60 "host port(s) unreachable ->${unreachable}. That is a pod that is not Ready, or a missing extraPortMapping. Section 2 says which."
else
    log_ok "22/22 host ports accepting"
fi
log_dim "404/401/403 shows as 'bounded': it proves an HTTP server is listening, which is"
log_dim "what this table tests. api-gateway ships no actuator, so 404 there is normal."

# ═══ 7  gateway 4xx/5xx ═══════════════════════════════════════════════════
log_banner "7. API-GATEWAY 4xx / 5xx"
if [ "$QUICK" -eq 1 ]; then
    log_info "--quick: skipped"
else
    gw_pod="$(kubectl get pod -n "$NAMESPACE" -l app.kubernetes.io/name=api-gateway \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
    if [ -z "$gw_pod" ]; then
        log_err "no api-gateway pod"
        add_cause 45 "api-gateway has no pod, so nothing is reachable through the host ports at all"
    else
        log_dim "pod ${gw_pod}"
        log_dim "window: the last ${GW_WINDOW} only (override with --window)."
        # Spring Cloud Gateway emits a Reactor access-log line per request:
        #   12:00:01.234 ... GET /api/v1/users FAILED: ... Connection refused
        # and for an unresolvable lb:// route:
        #   ... 503 Service Unavailable  / "Unable to find instance for auth-service"
        #
        # --since is essential, not an optimisation. Every replica logs
        # "Connect to discovery-server:8761 refused" for the first seconds of
        # its life, because it starts before the registry does. Reading the
        # whole buffer turns that normal boot ordering into a headline fault
        # on a cluster that is completely healthy — which is precisely the
        # false positive this window exists to remove. Widen it only when you
        # are deliberately hunting an old incident.
        gwlog="$(kubectl logs "$gw_pod" -n "$NAMESPACE" --since="$GW_WINDOW" --tail=800 --all-containers 2>/dev/null || true)"
        if [ -z "$gwlog" ]; then
            log_ok "no gateway log output in the last ${GW_WINDOW}"
        fi
        gwhits="$(printf '%s' "$gwlog" \
            | grep -Ei 'status=5[0-9][0-9]|status=4[0-9][0-9]| FAILED |Unable to find instance|Connection refused|no instances available|reactor\.netty' \
            | tail -20 || true)"
        if [ -n "$gwhits" ]; then
            printf '%s\n' "$gwhits" | sed "s/^/  ${C_YELLOW}/;s/$/${C_RESET}/"
            printf '\n'
            # A 4xx is a client problem, not a platform problem: a 401 for an
            # expired token is the system working. Only 5xx is escalated to a
            # cause, which is the whole reason this tool does not cry wolf.
            has5xx="$(printf '%s' "$gwhits" | grep -Eic 'status=5[0-9][0-9]' || true)"
            if printf '%s' "$gwhits" | grep -qi 'Unable to find instance\|no instances available'; then
                add_cause 55 "the gateway cannot resolve a \`lb://\` route through Eureka — the 'registered nowhere' 503. Section 5 names the application."
            elif [ "${has5xx:-0}" -gt 0 ] && printf '%s' "$gwhits" | grep -qi 'Connection refused\|Connection reset'; then
                add_cause 70 "the gateway reached a registered instance and the connection was refused — that pod is gone or not bound. Section 3 has its logs."
            elif [ "${has5xx:-0}" -gt 0 ]; then
                add_cause 70 "the gateway logged 5xx in the last ${GW_WINDOW} — read the lines above; the request never reached a healthy service."
            else
                log_warn "only 4xx in the last ${GW_WINDOW}. A 4xx is a client error (bad token," 
                log_dim "missing permission, wrong URL) and is the platform behaving correctly."
                log_dim "Widen the window with --window 2h if you are chasing an old incident."
            fi
        else
            log_ok "no 4xx/5xx in the last ${GW_WINDOW}"
        fi

        printf '\n  %s--- Eureka resolution, last %s ---%s\n' "$C_DIM" "$GW_WINDOW" "$C_RESET"
        printf '%s' "$gwlog" \
            | grep -Ei 'Unable to find instance|ServiceInstanceListSupplier|registration status' \
            | tail -10 | sed 's/^/  /' \
            || printf '  (none)\n'
    fi
fi

# ═══ 8  verdict ════════════════════════════════════════════════════════════
log_banner "MOST LIKELY CAUSE"
# Split on '|' into alternating priority/message records, then take the lowest
# priority. `sort -n` on the priorities alone is not enough — the messages have
# to travel with their number — so the two fields are interleaved and the
# winner is picked with a small awk pass.
cause_count="$(printf '%s' "$CAUSES" | tr -cd '|' | wc -c | tr -d ' ')"
cause_count=$(( cause_count / 2 ))

if [ "$cause_count" -eq 0 ]; then
    log_ok "nothing wrong found: node Ready, pods Ready, 9/9 databases up, 10/10 registered, 22/22 ports open."
    log_dim "If something still fails, the problem is outside the cluster:"
    log_dim "  * the caller (Postman, the SPA) hitting the wrong host or port"
    log_dim "  * a route api-gateway does not define — see its log in section 7"
    log_dim "  * data rather than availability: the service is up and the row is wrong"
    exit 0
fi

# The list, worst first.
printf '%s' "$CAUSES" | tr '|' '\n' | grep -v '^$' | paste - - \
    | sort -t'	' -k1,1n \
    | awk -F'\t' '{ printf "  [%3s] %s\n", $1, $2 }'

best_prio="$(printf '%s' "$CAUSES" | tr '|' '\n' | grep -v '^$' \
    | paste - - | sort -t'	' -k1,1n | head -1 | cut -f1)"
best_msg="$(printf '%s' "$CAUSES" | tr '|' '\n' | grep -v '^$' \
    | paste - - | sort -t'	' -k1,1n | head -1 | cut -f2-)"
printf '\n'

if [ "$best_prio" -ge 70 ] 2>/dev/null; then
    # Everything structural is clean, so this is an inference from recent log
    # text rather than a fact about the present. Say so, instead of presenting
    # a guess with the same confidence as "the database is down".
    log_warn "no structural fault found. This is a lead from the last ${GW_WINDOW} of"
    log_warn "gateway log text, not a confirmed fault — treat it as a starting point:"
else
    log_warn "${cause_count} problem(s) found, most significant first:"
fi
printf '  %s%s%s\n\n' "$C_BOLD$C_RED" "$best_msg" "$C_RESET"
log_dim "Next:  ${COMMON_SH_DIR}/debug.sh --service <name>    # one workload, in full"
log_dim "       ${COMMON_SH_DIR}/logs.sh <name> --previous    # the crashed instance"
exit 1
