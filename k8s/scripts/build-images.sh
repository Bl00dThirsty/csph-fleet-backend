#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# build-images.sh — build the 11 service images, then report honestly.
#
#   ./k8s/scripts/build-images.sh                             # all 11
#   ./k8s/scripts/build-images.sh auth-service api-gateway    # a subset
#   ./k8s/scripts/build-images.sh --serial                    # one at a time
#   ./k8s/scripts/build-images.sh --tag v1.4.0                # a release tag
#
# PARALLELISM
#   4 concurrent `docker build` invocations by default, as a rolling window:
#   the next build starts the moment any one finishes, so 11 images are not
#   forced into 3 rigid waves. Each Dockerfile is multi-stage and compiles its
#   own JAR inside Docker, so a build is CPU- and page-cache-heavy; 11 at once
#   makes every build slower, 4 keeps an 8-core box busy.
#
# HONEST REPORTING
#   The exit status of a backgrounded `docker build &` is not retrievable in
#   bash (`wait` returns it only for the most recent job, and `wait -n` needs
#   bash 4.3). The only trustworthy success signal is asking the Docker daemon
#   afterwards whether the tagged image exists. That is what the final report
#   uses — same intent as k8s/scripts/build-images.ps1 on Windows. Trusting a
#   PID, or the absence of the word ERROR in a log, can report success for an
#   image that was never produced.
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail
# shellcheck source=./common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

SERIAL=0
ONLY=""
TAG=""

usage() {
    sed -n '3,9p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --serial)  SERIAL=1; shift ;;
        --tag)     TAG="${2:?--tag needs a value}"; shift 2 ;;
        -h|--help) usage 0 ;;
        -*)        log_die "unknown option: $1  (try --help)" ;;
        *)         ONLY="${ONLY} $1"; shift ;;
    esac
done
[ -n "$TAG" ] || TAG="$IMAGE_TAG"

# ── resolve the work list ───────────────────────────────────────────────────
# An unknown name is a typo. Silently building 10 images when someone asked
# for 11 is the kind of thing that gets discovered an hour later.
for svc in $ONLY; do
    csv_contains "$svc" "$CSPH_SERVICES" || log_die "unknown service '${svc}'.
  Known: ${CSPH_SERVICES}
  (the argument is the module directory, without the gpl- prefix that the
   image tag carries)"
done
SERVICES=""
for svc in $CSPH_SERVICES; do
    if [ -z "$ONLY" ] || csv_contains "$svc" "$ONLY"; then
        SERVICES="${SERVICES}${svc} "
    fi
done
SERVICES="${SERVICES% }"
[ -n "$SERVICES" ] || log_die "nothing to build"

require_tools_docker
mkdir -p "$BUILD_LOG_DIR"

# SERVICES is a whitespace-separated list, so `wc -w` on the quoted string is
# the count. Unquoting it to "split on whitespace" would be the same number and
# would also let a stray glob expand.
service_count="$(printf '%s\n' "$SERVICES" | wc -w | tr -d ' ')"
log_step "Building ${service_count} image(s) as ${IMAGE_PREFIX}/gpl-*:${TAG}"
log_dim "context     = ${REPO_ROOT}"
log_dim "logs        = ${BUILD_LOG_DIR}"
log_dim "parallelism = $([ "$SERIAL" -eq 1 ] && echo 1 || echo 4)"
[ "$SERIAL" -eq 1 ] && log_dim "serial mode: one build at a time, so the log tails are readable"

# ── preflight: every Dockerfile must exist before any build starts ──────────
# Learning at minute six that one Dockerfile is missing, after ten minutes of
# building, is the worst possible time to learn it.
missing_df=""
for svc in $SERVICES; do
    df_path="${REPO_ROOT}/${svc}/Dockerfile"
    [ -f "$df_path" ] || missing_df="${missing_df} ${df_path}"
done
[ -z "$missing_df" ] || { log_err "missing Dockerfile(s):${missing_df}"; exit 1; }

# ── the parallel build loop ─────────────────────────────────────────────────
# The running set is a FILE (one "pid name log" row per build) rather than
# three parallel lists. Parallel lists have to be re-indexed on every pass and
# drift the moment a row is removed, which then pairs a name with the wrong
# log — the single most annoying bug in the PowerShell predecessor's
# bookkeeping. A file that is filtered and rewritten wholesale cannot drift.
ACTIVE="$(mktemp)"
ACTIVE_NEXT="$(mktemp)"
trap 'rm -f "$ACTIVE" "$ACTIVE_NEXT"' EXIT

start_build() {
    _sb_svc="$1"
    _sb_tag="${IMAGE_PREFIX}/gpl-${_sb_svc}:${TAG}"
    _sb_log="${BUILD_LOG_DIR}/${_sb_svc}.log"
    : > "$_sb_log"
    : > "${_sb_log}.err"
    # Absolute paths only: a relative -f is resolved against whatever cwd the
    # shell is in when the child is reaped, and docker then reports a bogus
    # "auth-service/Dockerfile: file not found".
    # </dev/null so the child cannot consume the loop's stdin.
    docker build -f "${REPO_ROOT}/${_sb_svc}/Dockerfile" -t "$_sb_tag" "$REPO_ROOT" \
        > "$_sb_log" 2> "${_sb_log}.err" < /dev/null &
    printf '%s %s %s\n' "$!" "$_sb_svc" "$_sb_log" >> "$ACTIVE"
    log_dim "  -> started  ${_sb_tag}"
}

report_one() {
    _ro_svc="$1"
    _ro_tag="${IMAGE_PREFIX}/gpl-${_ro_svc}:${TAG}"
    if docker image inspect "$_ro_tag" >/dev/null 2>&1; then
        log_ok "  ${_ro_tag}"
    else
        log_err "  ${_ro_tag}  ->  ${BUILD_LOG_DIR}/${_ro_svc}.log.err"
    fi
}

MAXPAR=4
[ "$SERIAL" -eq 1 ] && MAXPAR=1

: > "$ACTIVE"
queue="$SERVICES"
while [ -n "$queue" ] || [ -s "$ACTIVE" ]; do
    while [ -n "$queue" ] && [ "$(wc -l < "$ACTIVE" | tr -d ' ')" -lt "$MAXPAR" ]; do
        svc="$(printf '%s' "$queue" | awk '{print $1}')"
        queue="$(printf '%s' "$queue" | cut -d' ' -f2-)"
        start_build "$svc"
    done

    sleep 3

    : > "$ACTIVE_NEXT"
    while read -r pid svc log; do
        [ -n "$pid" ] || continue
        if kill -0 "$pid" 2>/dev/null; then
            printf '%s %s %s\n' "$pid" "$svc" "$log" >> "$ACTIVE_NEXT"
        else
            wait "$pid" 2>/dev/null || true
            report_one "$svc"
        fi
    done < "$ACTIVE"
    mv -f "$ACTIVE_NEXT" "$ACTIVE"
done

# ── the report ──────────────────────────────────────────────────────────────
log_step "BUILD REPORT (verified against the docker daemon, not against exit codes)"
ok=0; bad=0
for svc in $SERVICES; do
    tag="${IMAGE_PREFIX}/gpl-${svc}:${TAG}"
    size="$(docker image inspect "$tag" --format '{{.Size}}' 2>/dev/null || true)"
    if [ -n "$size" ]; then
        ok=$((ok + 1))
        printf '  %sOK   %-44s %6s MB%s\n' "${C_GREEN}" "$tag" "$(( size / 1000 / 1000 ))" "${C_RESET}"
    else
        bad=$((bad + 1))
        printf '  %sMISS %-44s%s\n' "${C_RED}" "$tag" "${C_RESET}"
    fi
done

total=$((ok + bad))
printf '\n  Built: %d   Missing: %d   (of %d)\n' "$ok" "$bad" "$total"
if [ "$bad" -gt 0 ]; then
    # Name one concrete file rather than a glob, so the next command is a
    # copy-paste rather than a search.
    first_bad=""
    for svc in $SERVICES; do
        if ! docker image inspect "${IMAGE_PREFIX}/gpl-${svc}:${TAG}" >/dev/null 2>&1; then
            first_bad="${BUILD_LOG_DIR}/${svc}.log.err"
            break
        fi
    done
    [ -n "$first_bad" ] && log_err "first failing build log: ${first_bad}"
    log_err "${bad} image(s) were not produced — not continuing."
    exit 1
fi
log_ok "all ${total} images present in the local docker daemon"
