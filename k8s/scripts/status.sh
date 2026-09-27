#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# status.sh — one-shot health summary.
#
#   ./k8s/scripts/status.sh
#   ./k8s/scripts/status.sh --wide        # add the node and IP columns
#
# Three tables, in the order that answers questions fastest:
#   1  pods          — what is running
#   2  not ready     — the only thing most people actually want
#   3  host ports    — can anything reach the platform from this machine
#
# This is the readable status page. It says nothing about WHY something is
# broken; for that use debug.sh, which pulls the previous container's log
# lines, the probe results and the registry state into one report.
#
# Exits 0 when every pod is Ready and every HTTP port answered 2xx/3xx,
# 1 otherwise — so it doubles as a CI/keepalive gate.
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail
# shellcheck source=./common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

WIDE=0
while [ "$#" -gt 0 ]; do
    case "$1" in
        --wide) WIDE=1; shift ;;
        -h|--help) sed -n '3,11p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) log_die "unknown argument: $1" ;;
    esac
done

require_tools_probe
cd "$REPO_ROOT"

if ! init_cluster_context; then
    log_banner "POD STATUS"
    log_err "kind cluster '${CLUSTER_NAME}' is not available."
    log_dim "  kind get clusters"
    log_dim "  ${COMMON_SH_DIR}/up.sh"
    exit 1
fi

# ── 1  pods ─────────────────────────────────────────────────────────────────
log_banner "POD STATUS  (namespace ${NAMESPACE})"
# Two branches rather than a variable that is either empty or "-o wide": the
# empty-string-into-`$WIDE` trick reads fine and breaks the moment somebody
# passes a flag that really does need quoting.
if [ -n "$WIDE" ]; then
    kubectl get pods -n "$NAMESPACE" -o wide 2>/dev/null | sed 's/^/  /' \
        || log_warn "kubectl get pods failed"
else
    kubectl get pods -n "$NAMESPACE" 2>/dev/null | sed 's/^/  /' \
        || log_warn "kubectl get pods failed"
fi

pod_count="$(kubectl get pods -n "$NAMESPACE" --no-headers 2>/dev/null | grep -c '[^[:space:]]' || true)"
ready_count="$(kubectl get pods -n "$NAMESPACE" --no-headers 2>/dev/null \
    | grep -Ec ' [0-9]+/[0-9]+ +Running +0 ' || true)"

# ── 2  not ready ────────────────────────────────────────────────────────────
log_banner "NOT READY / CRASHING"
BAD="$(kubectl get pods -n "$NAMESPACE" --no-headers 2>/dev/null \
    | grep -Ev ' [0-9]+/[0-9]+ +Running +0 ' \
    | grep -Ev 'Completed' || true)"
if [ -n "$BAD" ]; then
    printf '%s' "$BAD" | sed "s/^/  ${C_YELLOW}/;s/$/${C_RESET}/"
    log_dim "debug.sh explains WHY each of these is unhealthy."
else
    log_ok "none — ${ready_count}/${pod_count} pods Running with 0 restarts"
fi

# ── 3  host ports ───────────────────────────────────────────────────────────
log_banner "HOST PORT PROBES  (localhost, the 22 mappings in k8s/kind-cluster.yaml)"
printf '  %-6s %-24s %-6s %s\n' "PORT" "WORKLOAD" "CODE" "STATE"
printf '  %s\n' "---------------------------------------------------------------------"

unreachable=""
for row in $CSPH_HOST_PORTS; do
    port="$(printf '%s' "$row" | cut -d: -f1)"
    name="$(printf '%s' "$row" | cut -d: -f2)"
    kind="$(printf '%s' "$row" | cut -d: -f3)"
    path="$(printf '%s' "$row" | cut -d: -f4-)"

    if [ "$kind" = "app" ]; then
        # http_probe echoes a code and normally returns 0; the `|| printf ERR`
        # covers the one case where it does not — curl absent, which
        # require_tools_probe has already warned about.
        code="$(http_probe "http://127.0.0.1:${port}${path}" 5 || printf 'ERR')"
        state="$(classify_code "$code")"
    else
        # db / tcp: Postgres and SMTP do not speak HTTP. A bare TCP connect is
        # the honest test — a 22-byte Postgres greeting would be nicer but
        # nc's -z already proves the listener is accepting.
        if tcp_open 127.0.0.1 "$port" 3; then
            code="open"; state="OK"
        else
            code="----"; state="BAD"; unreachable="${unreachable} ${name}"
        fi
    fi

    case "$state" in
        OK)   printf '  %-6s %-24s %-6s %saccepting%s\n'  "$port" "$name" "$code" "$C_GREEN" "$C_RESET" ;;
        ALIVE) printf '  %-6s %-24s %-6s %sbounded, no route at that path%s\n' "$port" "$name" "$code" "$C_YELLOW" "$C_RESET" ;;
        *)    printf '  %-6s %-24s %-6s %sUNREACHABLE%s\n' "$port" "$name" "$code" "$C_RED" "$C_RESET" ;;
    esac
    if [ "$state" = "BAD" ] && [ "$kind" = "app" ]; then
        unreachable="${unreachable} ${name}"
    fi
done
printf '\n'
log_dim "A 404/401/403 still proves the port is bound and an HTTP server is on it."
log_dim "api-gateway (8080) ships no actuator, so 404 there is its healthy answer."

# ── verdict ─────────────────────────────────────────────────────────────────
if [ -n "$BAD" ] || [ -n "$unreachable" ]; then
    log_err "not healthy.${unreachable:+ unreachable ->${unreachable}}"
    log_dim "Diagnose with: ${COMMON_SH_DIR}/debug.sh"
    exit 1
fi
log_ok "healthy — ${ready_count} pods Ready, 22/22 host ports accepting"
