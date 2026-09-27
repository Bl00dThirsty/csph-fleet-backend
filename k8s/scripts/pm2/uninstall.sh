#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# uninstall.sh — remove the PM2 supervision for the CSPH frontend.
#
#   ./uninstall.sh                  # stop, deregister, remove the systemd unit
#   ./uninstall.sh --keep-logs      # also keep .pm2-logs/
#   ./uninstall.sh --purge-logs     # delete .pm2-logs/ too (the default is to keep)
#
# ORDER MATTERS HERE, and getting it wrong leaves a machine that resurrects what
# you just deleted:
#
#   1. pm2 delete the two apps        — stop the processes
#   2. pm2 save                       — OVERWRITE the dump. Without this, the
#                                      dump still lists the apps, and the next
#                                      boot (or the next `pm2 resurrect`) brings
#                                      them back and you have no idea why.
#   3. remove the systemd unit        — otherwise every boot runs
#                                      `pm2 resurrect` against a dump that no
#                                      longer has them, which is harmless but
#                                      confusing, and it keeps resurrecting any
#                                      OTHER app you have registered.
#   4. pm2 kill                      — stop the PM2 daemon itself (optional; it
#                                      will simply restart on next use)
#
# This removes only the two CSPH apps by name. It does NOT run `pm2 delete all`,
# because a shared server may be running unrelated Node applications and
# deleting those would be an extremely unpleasant surprise.
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
APPS="csph-frontend csph-frontend-api-proxy"
PURGE_LOGS=0
KEEP_LOGS=0

while [ "$#" -gt 0 ]; do
    case "$1" in
        --keep-logs)  KEEP_LOGS=1; shift ;;
        --purge-logs) PURGE_LOGS=1; shift ;;
        -h|--help) sed -n '3,22p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) printf 'unknown argument: %s\n' "$1" >&2; exit 2 ;;
    esac
done

say()  { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
ok()   { printf '\033[32m ok \033[0m %s\n' "$*"; }
warn() { printf '\033[33mwarn\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[31mfail\033[0m %s\n' "$*" >&2; exit 1; }

command -v pm2 >/dev/null 2>&1 || die "pm2 is not installed, so there is nothing of ours to remove.
  The systemd unit may still exist though — see step 3 below."

# ── 1. stop and deregister ──────────────────────────────────────────────────
say "removing the CSPH processes from pm2"
found=0
for app in $APPS; do
    if pm2 describe "$app" >/dev/null 2>&1; then
        pm2 delete "$app" >/dev/null
        ok "deleted $app"
        found=1
    else
        warn "$app is not registered with pm2"
    fi
done
[ "$found" -eq 1 ] || warn "neither CSPH app was registered"

# ── 2. overwrite the dump, so a resurrect cannot bring them back ────────────
say "refreshing the pm2 dump"
pm2 save
ok "dump rewritten — these apps are no longer in it"

# ── 3. remove the systemd unit ─────────────────────────────────────────────
UNIT="pm2-$(id -un)"
say "removing the systemd unit (${UNIT})"
UNIT_FILE="/etc/systemd/system/${UNIT}.service"
if [ -f "$UNIT_FILE" ] || systemctl list-unit-files 2>/dev/null | grep -q "^${UNIT}\.service"; then
    if [ "$(id -u)" -eq 0 ]; then
        systemctl disable --now "$UNIT" >/dev/null 2>&1 || true
        rm -f "$UNIT_FILE"
        systemctl daemon-reload
        ok "removed (as root)"
    elif command -v sudo >/dev/null 2>&1; then
        sudo systemctl disable --now "$UNIT" >/dev/null 2>&1 || true
        sudo rm -f "$UNIT_FILE"
        sudo systemctl daemon-reload
        ok "removed"
    else
        warn "sudo is not available. Run these yourself as root:"
        warn "  sudo systemctl disable --now ${UNIT}"
        warn "  sudo rm -f ${UNIT_FILE}"
        warn "  sudo systemctl daemon-reload"
    fi
else
    ok "no systemd unit found (was install.sh run with --no-startup?)"
fi

# ── 4. the pm2 daemon ──────────────────────────────────────────────────────
say "stopping the pm2 daemon"
if pm2 kill >/dev/null 2>&1; then
    ok "pm2 daemon stopped"
else
    warn "pm2 kill reported a problem; the daemon may already be gone"
fi

# ── 5. logs, only on request ───────────────────────────────────────────────
FRONTEND_ROOT="${CSPH_FRONTEND_ROOT:-$(cd "${SCRIPT_DIR}/../../../.." && pwd -P)/csph-fleet-frontend}"
LOG_DIR="${FRONTEND_ROOT}/.pm2-logs"
if [ "$PURGE_LOGS" -eq 1 ] && [ "$KEEP_LOGS" -eq 0 ]; then
    if [ -d "$LOG_DIR" ]; then
        rm -rf "$LOG_DIR"
        ok "deleted ${LOG_DIR}"
    else
        ok "no log directory to delete"
    fi
else
    [ -d "$LOG_DIR" ] && ok "logs kept at ${LOG_DIR} (use --purge-logs to delete)"
fi

cat <<'EOF'

──────────────────────────────────────────────────────────────────────────────
 Removed. Nothing of ours will start on the next boot.

 Deliberately NOT removed, because it is not ours:
   * node and pm2 themselves
   * ~/.pm2/dump.pm2 entries for any OTHER app on this machine
   * the built SPA in csph-fleet-frontend/apps/web/dist
   * .pm2-logs/ unless you passed --purge-logs

 To put it back:  k8s/scripts/pm2/install.sh
──────────────────────────────────────────────────────────────────────────────
EOF
