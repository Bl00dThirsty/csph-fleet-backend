#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# common.sh — shared library for the CSPH GPL Fleet Platform bash tooling.
#
# SOURCED, never executed. Every operational script in this directory starts
# with:
#
#     set -euo pipefail
#     # shellcheck source=./common.sh
#     source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
#
# and then calls `require_cmd` / `init_cluster_context` as needed.
#
# WHAT LIVES HERE
#   - the canonical fact table (cluster, namespace, 11 services, 9 databases,
#     22 host ports, the 10 Eureka application names) so no script can drift
#     from another
#   - coloured, TTY-aware logging that honours NO_COLOR and degrades to plain
#     text when stdout is not a terminal (CI logs, `kubectl logs | cat`)
#   - `retry` / `wait_for_pods` / `wait_for_eureka` — the three waits this
#     platform actually needs
#
# WHY THE STRICT MODE IS RE-ASSERTED HERE
#   A sourced file runs in the caller's shell. If a caller forgets
#   `set -euo pipefail`, this library re-asserts it so no script in this
#   directory can ever run half-unset. It is a guard rail, not a convenience:
#   with `pipefail` off, `kubectl get pods | grep -q CrashLoop` would report
#   success on the grep's exit code alone even when kubectl had failed.
#
# POSIX/BASH COMPATIBILITY
#   Target is bash 4.1+ (RHEL 7 / CentOS 7 era) and up. Deliberately NOT used:
#     ${var^^} / ${var,,}          (bash 4.0+ but noisier than tr)
#     mapfile -d                   (bash 4.4+)
#     declare -A                   (bash 4.0+, used only where unavoidable)
#     ${!var@}                     (bash 4.3+)
#   Associative arrays are avoided entirely; plain parallel indexed arrays
#   ("csv" + `IFS=, read -r`) are used instead so the scripts also parse under
#   bash 3.2 if anyone runs them on a Mac for a quick check.
# ═══════════════════════════════════════════════════════════════════════════

set -euo pipefail

# ── paths ───────────────────────────────────────────────────────────────────
# This file lives at <repo>/k8s/scripts/common.sh, so the repository root is two
# levels up — the same rule every other script in this directory uses, which
# means the whole toolchain works from any working directory.
#
# Declare, assign, then export — never `export X="$(...)"`. In one statement
# the export masks the substitution's exit status, so a `cd` that failed would
# export an empty REPO_ROOT and every later path would be silently wrong.
COMMON_SH_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${COMMON_SH_DIR}/../.." && pwd -P)"
K8S_DIR="${REPO_ROOT}/k8s"
OVERLAY_DIR="${K8S_DIR}/overlays/dev"
KIND_CONFIG="${K8S_DIR}/kind-cluster.yaml"
BUILD_LOG_DIR="${COMMON_SH_DIR}/build-logs"
export COMMON_SH_DIR REPO_ROOT K8S_DIR OVERLAY_DIR KIND_CONFIG BUILD_LOG_DIR

# ── the canonical fact table ────────────────────────────────────────────────
export CLUSTER_NAME="${CSPH_CLUSTER:-csph-gpl}"
export NAMESPACE="${CSPH_NAMESPACE:-csph-gpl}"
export IMAGE_TAG="${CSPH_IMAGE_TAG:-dev}"
export IMAGE_PREFIX="${CSPH_IMAGE_PREFIX:-csph}"
export EUREKA_HOST_PORT=8761
export GATEWAY_HOST_PORT=8080

# The 11 images built from Dockerfiles in this repository, in dependency order
# (discovery first, gateway last) because that is the order the failures are
# easiest to read in.
CSPH_SERVICES="discovery-server api-gateway auth-service organization-service user-service audit-service notification-service tour-service cylinder-service fleet-device-service subsidy-service"
export CSPH_SERVICES

# service -> postgres StatefulSet. Used by debug.sh (per-DB health) and by
# reset-db.sh (--service <name> -> the PVC to wipe).
CSPH_SERVICE_DB="auth-service:postgres-auth organization-service:postgres-org user-service:postgres-user audit-service:postgres-audit notification-service:postgres-notif tour-service:postgres-tour cylinder-service:postgres-cylinder fleet-device-service:postgres-fleet subsidy-service:postgres-subsidy"
export CSPH_SERVICE_DB

# The 9 StatefulSets, in the order the platform documents them.
CSPH_DATABASES="postgres-auth postgres-org postgres-user postgres-audit postgres-notif postgres-tour postgres-fleet postgres-cylinder postgres-subsidy"
export CSPH_DATABASES

# database -> database name, which is what pg_isready needs for -d.
CSPH_DB_NAMES="postgres-auth:gpl_auth_db postgres-org:gpl_organization_db postgres-user:gpl_user_db postgres-audit:gpl_audit_db postgres-notif:gpl_notification_db postgres-tour:gpl_tour_db postgres-fleet:gpl_fleet_db postgres-cylinder:gpl_cylinder_db postgres-subsidy:gpl_subsidy_db"
export CSPH_DB_NAMES

# The 10 applications that must appear in the Eureka registry. Eureka uppercases
# every application name it stores (FLEET-DEVICE-SERVICE, not
# fleet-device-service), so the comparison in wait_for_eureka is done in upper
# case on both sides — getting that wrong is the single easiest way to write a
# wait loop that can never succeed.
EUREKA_APPS="API-GATEWAY AUTH-SERVICE ORGANIZATION-SERVICE USER-SERVICE AUDIT-SERVICE NOTIFICATION-SERVICE TOUR-SERVICE CYLINDER-SERVICE FLEET-DEVICE-SERVICE SUBSIDY-SERVICE"
export EUREKA_APPS

# hostport:label:kind:http-path
#   kind=app     -> HTTP probe, the status code is the interesting value
#   kind=db      -> raw TCP, Postgres does not speak HTTP
# The 22 host ports are exactly the extraPortMappings in k8s/kind-cluster.yaml.
CSPH_HOST_PORTS="8080:api-gateway:app:/actuator/health 8081:auth-service:app:/v3/api-docs 8082:organization-service:app:/v3/api-docs 8083:user-service:app:/v3/api-docs 8084:audit-service:app:/v3/api-docs 8085:notification-service:app:/v3/api-docs 8086:tour-service:app:/v3/api-docs 8087:cylinder-service:app:/v3/api-docs 8088:fleet-device-service:app:/v3/api-docs 8089:subsidy-service:app:/v3/api-docs 8761:discovery-server:app:/actuator/health 5500:postgres-auth:db: 5501:postgres-org:db: 5502:postgres-user:db: 5503:postgres-audit:db: 5504:postgres-notif:db: 5505:postgres-tour:db: 5506:postgres-fleet:db: 5507:postgres-cylinder:db: 5508:postgres-subsidy:db: 1026:mailpit-smtp:tcp: 8026:mailpit-ui:app:/"
export CSPH_HOST_PORTS

# ── colour ──────────────────────────────────────────────────────────────────
# Honour NO_COLOR (https://no-color.org/) and the de-facto NO_COLOUR, and only
# emit escapes on a TTY. This matters because these scripts are read at 2am
# from a terminal AND captured into CI logs where raw ESC[ sequences make the
# output unreadable.
if [ -t 1 ] && [ -z "${NO_COLOR:-}" ] && [ -z "${NO_COLOUR:-}" ]; then
    C_RESET=$'\033[0m'
    C_BOLD=$'\033[1m'
    C_DIM=$'\033[2m'
    C_RED=$'\033[31m'
    C_GREEN=$'\033[32m'
    C_YELLOW=$'\033[33m'
    C_BLUE=$'\033[34m'
    C_CYAN=$'\033[36m'
else
    C_RESET=''
    C_BOLD=''
    C_DIM=''
    C_RED=''
    C_GREEN=''
    C_YELLOW=''
    C_BLUE=''
    C_CYAN=''
fi

# ── logging ─────────────────────────────────────────────────────────────────
# printf everywhere, never `echo -e`: `echo -e` is not POSIX, and on bash
# built with --enable-strict-posix-default it prints the -e literally.
_ts() { date '+%H:%M:%S'; }

log_step() {
    printf '\n%s[%s] %s%s\n' "${C_BOLD}${C_CYAN}" "$(_ts)" "$*" "${C_RESET}"
}
log_info() { printf '%s[%s]%s %s\n'   "${C_BLUE}"   "$(_ts)" "${C_RESET}" "$*"; }
log_ok()   { printf '%s[ ok ]%s %s\n' "${C_GREEN}"  "${C_RESET}" "$*"; }
log_warn() { printf '%s[warn]%s %s\n' "${C_YELLOW}" "${C_RESET}" "$*" >&2; }
log_err()  { printf '%s[fail]%s %s\n' "${C_RED}"    "${C_RESET}" "$*" >&2; }
log_dim()  { printf '%s       %s%s\n' "${C_DIM}"    "$*" "${C_RESET}"; }

# A titled banner. Used by debug.sh and status.sh so the sections are
# scannable rather than a wall of text.
log_banner() {
    printf '\n%s%s\n' "${C_BOLD}${C_CYAN}" "$(printf '=%.0s' $(seq 1 74))"
    printf '== %s\n' "$*"
    printf '%s%s\n' "$(printf '=%.0s' $(seq 1 74))" "${C_RESET}"
}

log_die() {
    log_err "$*"
    exit 1
}

# ── dependency checks ───────────────────────────────────────────────────────
# require_cmd <command> [install hint]
# Called BEFORE anything is created, so a missing dependency fails with a
# sentence about what to install rather than halfway through a half-built
# cluster.
require_cmd() {
    _rc_cmd="$1"
    _rc_hint="${2:-}"
    if command -v "$_rc_cmd" >/dev/null 2>&1; then
        return 0
    fi
    log_err "'${_rc_cmd}' is not on PATH."
    if [ -n "$_rc_hint" ]; then
        log_dim "${_rc_hint}"
    fi
    log_dim "This script assumes the standard Linux install locations:"
    case "$_rc_cmd" in
        kind)    log_dim "  curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.29.0/kind-linux-amd64 && sudo install -m 0755 ./kind /usr/local/bin/kind" ;;
        kubectl) log_dim "  curl -LO 'https://dl.k8s.io/release/v1.31.0/bin/linux/amd64/kubectl' && sudo install -m 0755 ./kubectl /usr/local/bin/kubectl" ;;
        docker)  log_dim "  https://docs.docker.com/engine/install/  (then: sudo usermod -aG docker \$USER && newgrp docker)" ;;
    esac
    exit 1
}

# require_tools_cluster / require_tools_docker / require_tools_probe split the
# single up-front check so a script that never touches Docker (status.sh) does
# not demand a running Docker daemon it has no use for.
require_tools_cluster() {
    require_cmd kind    "Install kind: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    require_cmd kubectl "Install kubectl: https://kubernetes.io/docs/tasks/tools/"
}

require_tools_docker() {
    require_tools_cluster
    require_cmd docker "Install Docker Engine: https://docs.docker.com/engine/install/"
    if ! docker info >/dev/null 2>&1; then
        log_err "the Docker daemon is not reachable."
        log_dim "Start Docker, or add yourself to the 'docker' group: sudo usermod -aG docker \$USER"
        exit 1
    fi
}

# require_tools_probe is for scripts that only read: kubectl plus one of
# curl/nc for the host-port table. curl is strongly preferred (it yields a
# status code); without it the probes degrade to "port open / closed".
require_tools_probe() {
    require_tools_cluster
    if ! command -v curl >/dev/null 2>&1 && ! command -v nc >/dev/null 2>&1; then
        log_err "neither 'curl' nor 'nc' is on PATH, so the host-port table cannot be produced."
        log_dim "  Debian/Ubuntu: sudo apt-get install -y curl"
        log_dim "  RHEL/Rocky:     sudo dnf install -y curl"
        exit 1
    fi
}

# ── cluster / context plumbing ──────────────────────────────────────────────
cluster_exists() {
    # `kind get clusters` prints "No kind clusters found." to STDERR and exits
    # non-zero when there is nothing. Swallow both, inspect stdout.
    _ce_list="$(kind get clusters 2>/dev/null || true)"
    printf '%s\n' "$_ce_list" | grep -qx -- "$CLUSTER_NAME"
}

# Point kubectl at kind-<cluster> and make sure the API server answers.
# Without the `use-context`, every kubectl call below silently targets
# whatever the operator's default context happens to be.
init_cluster_context() {
    if ! cluster_exists; then
        return 1
    fi
    kubectl config use-context "kind-${CLUSTER_NAME}" >/dev/null 2>&1 || true
    if ! kubectl cluster-info >/dev/null 2>&1; then
        log_err "cluster '${CLUSTER_NAME}' exists but its API server is not answering."
        log_dim "docker ps -a | grep ${CLUSTER_NAME}   # is the node container running?"
        return 1
    fi
    return 0
}

# The kubectl prefix every call in every script must use. Centralised so the
# namespace is impossible to forget on one call site.
k() {
    kubectl -n "$NAMESPACE" "$@"
}

# ── small helpers ───────────────────────────────────────────────────────────
# csv_contains <needle> <space-or-comma separated haystack>
# Membership test without depending on associative arrays.
csv_contains() {
    local needle="$1" hay="$2" item
    for item in $hay; do
        [ "$item" = "$needle" ] && return 0
    done
    return 1
}

# upper <string> — locale-independent, unlike ${s^^} on some bash builds.
upper() {
    printf '%s' "$1" | tr '[:lower:]' '[:upper:]'
}

# ── retry ───────────────────────────────────────────────────────────────────
# retry <attempts> <sleep_seconds> -- <command...>
#
# Runs <command> until it exits 0 or the attempts are exhausted. The `--`
# separator is mandatory so a command that legitimately starts with `-` (or
# takes a dash-leading argument) is not mistaken for the separator.
#
# On the last attempt the command's own stderr is NOT swallowed, so the real
# error message survives. Silently discarding it on every attempt is how people
# end up with a loop that "failed 10 times" and no reason why.
retry() {
    local attempts="$1" sleep_s="$2"
    shift 2
    if [ "${1:-}" = "--" ]; then shift; fi
    if [ "$#" -eq 0 ]; then
        log_die "retry: no command given"
    fi

    # `if cmd; then ...; fi` returns 0 even when cmd failed, because there is no
    # else branch. $? therefore has to be captured inside an explicit else or
    # the retry would always report rc=0 and the caller's error handling would
    # see success.
    local n=1 rc=0
    while [ "$n" -le "$attempts" ]; do
        if "$@"; then
            return 0
        else
            rc=$?
        fi
        if [ "$n" -eq "$attempts" ]; then
            break
        fi
        log_dim "attempt ${n}/${attempts} failed (rc=${rc}); retrying in ${sleep_s}s ..."
        sleep "$sleep_s"
        n=$((n + 1))
    done
    log_err "gave up after ${attempts} attempt(s): $*"
    return "$rc"
}

# ── wait_for_pods ───────────────────────────────────────────────────────────
# wait_for_pods [timeout_seconds]
#
# Blocks until every pod in the namespace is Running AND Ready (1/1), or the
# timeout expires. Returns 0 on success, 1 on timeout — callers decide whether
# that is fatal (up.sh) or merely reported (status.sh).
#
# WHY NOT `kubectl wait --for=condition=Ready pod --all`
#   `kubectl wait` returns 0 the moment the selector matches nothing, which
#   happens for a real window right after `apply` while the ReplicaSets are
#   still being created by the controllers. The script would then declare
#   victory against an empty namespace. This loop first asserts the expected
#   pod COUNT is present, and only then asserts readiness.
wait_for_pods() {
    local timeout="${1:-600}"
    local expected_pods="${2:-21}"
    local deadline elapsed not_ready count
    deadline=$(( $(date +%s) + timeout ))
    local last_report=0

    log_info "waiting up to ${timeout}s for ${expected_pods} pods to be Ready ..."

    while :; do
        # Single kubectl call, counted rather than eyeballed.
        count="$(k get pods --no-headers 2>/dev/null | grep -c '[^[:space:]]' || true)"
        not_ready="$(k get pods --no-headers 2>/dev/null \
            | grep -Ev '(^[^ ]+ +[0-9]+/[0-9]+ +Running +0 +)' \
            | grep -Ev 'Completed' \
            | grep -c '[^[:space:]]' || true)"

        if [ "$not_ready" -eq 0 ] && [ "$count" -ge "$expected_pods" ]; then
            log_ok "all ${count} pods Ready"
            return 0
        fi

        elapsed=$(( $(date +%s) ))
        if [ "$elapsed" -ge "$deadline" ]; then
            log_err "timed out after ${timeout}s: ${not_ready} pod(s) not Ready, ${count}/${expected_pods} present"
            k get pods --no-headers 2>/dev/null \
                | grep -Ev '(^[^ ]+ +[0-9]+/[0-9]+ +Running +0 +)' \
                | grep -Ev 'Completed' \
                | sed 's/^/       /' || true
            return 1
        fi

        # Progress line every 15s, otherwise a 10-minute wait is a black box.
        if [ $(( elapsed - last_report )) -ge 15 ]; then
            last_report="$elapsed"
            log_dim "$(printf '%3ss left' "$(( deadline - elapsed ))")  ready=$(( count - not_ready ))/${count}"
        fi
        sleep 5
    done
}

# ── wait_for_eureka ─────────────────────────────────────────────────────────
# wait_for_eureka [timeout_seconds]
#
# THE OBSERVED FAILURE THIS EXISTS TO PREVENT
#   A Spring Boot service's tcpSocket readinessProbe passes the instant
#   Netty/Tomcat binds the port. Service discovery registration happens
#   *after* that, on the Eureka client's own schedule (first registration is
#   ~30s of initial delay plus the registry's own response time). For roughly
#   a minute after `apply`, therefore:
#       * every pod is Ready
#       * every host port answers
#       * and yet `curl localhost:8080/api/v1/anything` returns
#         503 "Unable to find instance for auth-service"
#   This was a real, repeatedly-misdiagnosed failure on this platform: the
#   status page said "everything green" while the API was down. up.sh MUST NOT
#   declare success on pod readiness alone.
#
# HOW IT CHECKS
#   Reads the registry through the API server's service proxy
#   (`kubectl get --raw .../services/http:discovery-server:8761/proxy/eureka/apps`)
#   rather than curling host:8761. Two reasons: it needs no curl, and it
#   works even when the NodePort mapping is unreachable from the machine
#   running the script (a different host, a firewall, a remote kubeconfig) —
#   which is the normal case once this tooling runs on the Linux server.
#
##   Eureka's representation is NOT stable across access paths, and getting this
#   wrong is silent: the wait loop simply never succeeds. Measured on this
#   platform, `curl localhost:8761/eureka/apps` returns XML
#   (<name>AUTH-SERVICE</name>) while `kubectl get --raw .../proxy/eureka/apps`
#   returns JSON ({"name":"AUTH-SERVICE"}) because the API server's service
#   proxy asks for JSON and this Eureka build honours the Accept header. So
#   BOTH shapes are matched. Verified against the live registry, not assumed.
wait_for_eureka() {
    local timeout="${1:-420}"
    local deadline elapsed registered missing want
    local raw_path="/api/v1/namespaces/${NAMESPACE}/services/http:discovery-server:8761/proxy/eureka/apps"
    local body total_apps missing_n pattern
    deadline=$(( $(date +%s) + timeout ))
    local last_report=0
    total_apps="$(printf '%s\n' "$EUREKA_APPS" | wc -w | tr -d ' ')"

    log_info "waiting up to ${timeout}s for all 10 applications to register with Eureka ..."

    while :; do
        body="$(kubectl get --raw "$raw_path" 2>/dev/null || true)"

        if [ -n "$body" ]; then
            missing=""
            for want in $EUREKA_APPS; do
                # $want is upper-case letters and hyphens only (EUREKA_APPS is a
                # constant), so it is safe to interpolate into an ERE without
                # escaping any metacharacters.
                pattern="(<name>${want}</name>|\"name\"[[:space:]]*:[[:space:]]*\"${want}\")"
                if ! printf '%s' "$body" | grep -qE "$pattern"; then
                    missing="${missing} ${want}"
                fi
            done
            if [ -z "$missing" ]; then
                log_ok "all ${total_apps} applications registered and routable"
                return 0
            fi

            missing_n="$(printf '%s' "$missing" | wc -w | tr -d ' ')"
            registered=$(( total_apps - missing_n ))

            elapsed=$(date +%s)
            if [ $(( elapsed - last_report )) -ge 15 ]; then
                last_report="$elapsed"
                log_dim "$(printf '%3ss left' "$(( deadline - elapsed ))")  registered=${registered}/${total_apps}  missing:${missing}"
            fi
        fi

        if [ "$(date +%s)" -ge "$deadline" ]; then
            if [ -z "${body:-}" ]; then
                log_err "timed out after ${timeout}s: the Eureka registry is unreadable at ${raw_path}"
                log_dim "Is discovery-server up?  kubectl -n ${NAMESPACE} get pods -l app.kubernetes.io/name=discovery-server"
            else
                log_err "timed out after ${timeout}s: still unregistered ->${missing}"
                log_dim "A pod can be Ready and still be missing here. Check the service's own log for a registration error."
            fi
            return 1
        fi
        sleep 5
    done
}

# ── host-port probes ───────────────────────────────────────────────────────
# tcp_open <host> <port> <timeout_seconds>
# Uses bash's /dev/tcp redirection, which is a bash feature and needs no netcat.
# The subshell keeps the failed connection from printing a redirection error to
# stderr on every closed port, which would bury the real table in noise.
tcp_open() {
    local host="$1" port="$2" timeout_s="${3:-2}"
    if command -v nc >/dev/null 2>&1; then
        nc -z -w "$timeout_s" "$host" "$port" >/dev/null 2>&1
    else
        ( exec 3<>"/dev/tcp/${host}/${port}" ) >/dev/null 2>&1
    fi
}

# http_probe <url> [timeout_seconds] -> echoes the status code, or ERR/000
http_probe() {
    local url="$1" timeout_s="${2:-5}" code
    if ! command -v curl >/dev/null 2>&1; then
        return 1
    fi
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time "$timeout_s" "$url" 2>/dev/null || true)"
    case "$code" in
        ''|000) printf 'ERR' ;;
        *)      printf '%s' "$code" ;;
    esac
}

# classify_code <code-or-ERR> -> OK | ALIVE | BAD
#
# "Is this port reachable" and "did this endpoint return 200" are two different
# questions, and conflating them produces false alarms. Measured on the live
# cluster:
#   8761 /actuator/health   -> 200   the one workload that ships actuator
#   8081 /v3/api-docs      -> 200
#   8026 /                 -> 200   mailpit UI
#   8080 /actuator/health  -> 404   api-gateway has NO actuator on the
#                                  classpath, so 404 is its HEALTHY answer:
#   8080 /                 -> 404    the reactor is bound and answering.
#   8080 /api/v1/auth/login-> 403    no credentials supplied; still serving.
# A 404 or 403 therefore proves the thing the probe is actually testing — the
# port is bound and an HTTP server is on it — so it is reported as ALIVE, in
# yellow, with the caveat printed once at the foot of the table rather than as
# a failure.
classify_code() {
    case "$1" in
        2*|3*)          printf 'OK' ;;
        404|401|403)    printf 'ALIVE' ;;
        5*|ERR|000|'')  printf 'BAD' ;;
        *)              printf 'ALIVE' ;;
    esac
}
