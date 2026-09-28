#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# install.sh — put the CSPH frontend under PM2 supervision and survive reboots.
#
#   ./install.sh              # install node/pm2 if needed, start, save, systemd
#   ./install.sh --no-startup # everything except the systemd unit
#
# WHAT IT DOES, IN ORDER
#   1. check node / npm / pm2, and offer to install pm2 if missing
#   2. check the built SPA actually exists (the most common failure is
#      starting PM2 against an empty dist/ and wondering why every route 404s)
#   3. create the log directory
#   4. pm2 start ecosystem.config.cjs --env production
#   5. pm2 save                      — persist the process list
#   6. pm2 startup + systemd unit    — bring it back after a reboot
#   7. verify, and print what to do next
#
# WHY STEPS 5 AND 6 ARE SEPARATE, which trips everyone up exactly once
#   `pm2 save` writes the process list to ~/.pm2/dump.pm2. It does NOT make
#   anything start at boot. `pm2 startup` installs a systemd unit that runs
#   `pm2 resurrect` on boot, and `pm2 resurrect` reads that dump. Both are
#   needed. With only `save`, a reboot silently leaves nothing running; with only
#   `startup`, a `pm2 delete` is undone by the next reboot.
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
DO_STARTUP=1

while [ "$#" -gt 0 ]; do
    case "$1" in
        --no-startup) DO_STARTUP=0; shift ;;
        -h|--help) sed -n '3,18p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) printf 'unknown argument: %s\n' "$1" >&2; exit 2 ;;
    esac
done

say()  { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
ok()   { printf '\033[32m ok \033[0m %s\n' "$*"; }
warn() { printf '\033[33mwarn\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[31mfail\033[0m %s\n' "$*" >&2; exit 1; }

# ── 1. dependencies ─────────────────────────────────────────────────────────
say "checking dependencies"

command -v node >/dev/null 2>&1 || die "node is not installed.
  Debian/Ubuntu: curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash - && sudo apt-get install -y nodejs
  RHEL/Rocky:   sudo dnf install -y nodejs"
NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]')"
[ "$NODE_MAJOR" -ge 18 ] || die "node $(node -v) is too old. This tooling needs node 18+ (node: prefix imports)."
ok "node $(node -v)"

command -v npm >/dev/null 2>&1 || die "npm is not installed (it normally ships with node)."
ok "npm $(npm -v)"

if ! command -v pm2 >/dev/null 2>&1; then
    warn "pm2 is not installed."
    if [ "${CSPH_AUTO_INSTALL:-0}" = "1" ]; then
        say "installing pm2 globally (CSPH_AUTO_INSTALL=1)"
        npm install -g pm2
    else
        die "install it with:  npm install -g pm2
  or re-run with CSPH_AUTO_INSTALL=1 to let this script do it."
    fi
fi
ok "pm2 $(pm2 -v)"

# ── 2. the built SPA ───────────────────────────────────────────────────────
say "checking the built SPA"
# Resolve the same way ecosystem.config.cjs does, so a mismatch is caught here
# rather than as a 404 on every route.
FRONTEND_ROOT="${CSPH_FRONTEND_ROOT:-}"
if [ -z "$FRONTEND_ROOT" ]; then
    FRONTEND_ROOT="$(cd "${SCRIPT_DIR}/../../../.." && pwd -P)/csph-fleet-frontend"
fi
DIST="${FRONTEND_ROOT}/apps/web/dist"

if [ ! -f "${DIST}/index.html" ]; then
    die "no built SPA at ${DIST}
  Build it first:
    cd '${FRONTEND_ROOT}' && npm ci && npm run build
  If your frontend repo is somewhere else:
    CSPH_FRONTEND_ROOT=/path/to/csph-fleet-frontend ./install.sh"
fi
ok "dist/ found: ${DIST}"
if [ ! -d "${DIST}/assets" ]; then
    warn "no ${DIST}/assets directory — that is unusual for a Vite build."
    warn "Static assets will 404 and the SPA will render unstyled."
fi

# ── 3. log directory ────────────────────────────────────────────────────────
LOG_DIR="${FRONTEND_ROOT}/.pm2-logs"
mkdir -p "$LOG_DIR"
ok "log directory: ${LOG_DIR}"

# ── 4. start ────────────────────────────────────────────────────────────────
say "starting with pm2"
# Stop first so a re-run is idempotent: `pm2 start` on an already-running app
# named the same thing refuses or duplicates depending on version.
if pm2 describe csph-frontend >/dev/null 2>&1; then
    warn "csph-frontend is already registered — restarting it in place"
    pm2 restart ecosystem.config.cjs --env production --update-env >/dev/null
else
    pm2 start "$SCRIPT_DIR/ecosystem.config.cjs" --env production
fi

# ── 5. persist ──────────────────────────────────────────────────────────────
say "saving the process list"
pm2 save
ok "${HOME}/.pm2/dump.pm2 written"

# ── 6. systemd ──────────────────────────────────────────────────────────────
if [ "$DO_STARTUP" -eq 1 ]; then
    say "installing the systemd unit"
    # `pm2 startup` PRINTS the command to run; it does not run it. The sudo is
    # required because it writes /etc/systemd/system. Capture and execute it,
    # because making a human copy-paste a long sudo line is how this step gets
    # skipped and the process silently dies on the next reboot.
    STARTUP_CMD=""
    if PM2_OUT="$(pm2 startup systemd -u "$(id -un)" --hp "$HOME" 2>&1)"; then
        STARTUP_CMD="$(printf '%s\n' "$PM2_OUT" | tail -1)"
    elif PM2_OUT="$(pm2 startup -u "$(id -un)" --hp "$HOME" 2>&1)"; then
        STARTUP_CMD="$(printf '%s\n' "$PM2_OUT" | tail -1)"
    fi

    case "$STARTUP_CMD" in
        sudo*)
            if [ "$(id -u)" -eq 0 ]; then
                # Already root: run it directly, the sudo prefix is a no-op.
                eval "$STARTUP_CMD" >/dev/null
                ok "systemd unit installed"
            elif command -v sudo >/dev/null 2>&1; then
                # `sudo sh -c`, NOT `sudo eval`. sudo executes a PROGRAM, and
                # `eval` is a shell builtin, so `sudo eval ...` fails with
                # "sudo: eval: command not found" — which would break the one
                # step that makes this survive a reboot, and only on machines
                # that are not already root.
                sudo sh -c "$STARTUP_CMD" >/dev/null \
                    || die "the systemd unit could not be installed. Run this yourself:
  $STARTUP_CMD"
                ok "systemd unit installed"
            else
                warn "sudo is not available. Run this yourself as root:"
                warn "  $STARTUP_CMD"
                warn "Without it, nothing starts after a reboot."
            fi
            ;;
        *)
            warn "could not determine the systemd command (pm2 said: ${PM2_OUT:-nothing})"
            warn "Run:  sudo env PATH=\$PATH pm2 startup systemd -u $(id -un) --hp $HOME"
            ;;
    esac
else
    warn "--no-startup: skipping the systemd unit. Nothing will start after a reboot."
fi

# ── 7. verify ───────────────────────────────────────────────────────────────
say "verifying"
STATUS="$(pm2 jlist 2>/dev/null || echo '[]')"
# Single quotes are correct here: the JavaScript contains `${...}` template
# literals that must NOT be expanded by the shell. SC2016 is a false positive.
# shellcheck disable=SC2016
node -e '
const list = JSON.parse(process.argv[1] || "[]");
if (list.length === 0) { console.error("  no processes registered"); process.exit(1); }
let bad = 0;
for (const p of list) {
  const online = p.pm2_env.status === "online";
  const restarts = p.pm2_env.restart_time;
  if (!online) bad++;
  console.log(`  ${online ? "online " : "OFFLINE"}  ${p.name.padEnd(26)} pid=${String(p.pid).padEnd(8)} restarts=${restarts}`);
}
process.exit(bad === 0 ? 0 : 1);
' "$STATUS" || warn "at least one process is not online — check the logs below"

cat <<EOF

──────────────────────────────────────────────────────────────────────────────
 Installed.

   SPA            http://localhost:3000/
   API proxy      http://localhost:3001/api/*  ->  the gateway on :8080

 Check it actually serves:
   curl -s -o /dev/null -w '%{http_code}\n' http://localhost:3000/          # 200
   curl -s -o /dev/null -w '%{http_code}\n' http://localhost:3001/api/v1/auth/login   # 401/403 from the gateway

 Everyday commands:
   pm2 status
   pm2 logs csph-frontend
   pm2 logs csph-frontend-api-proxy --lines 100
   pm2 restart csph-frontend --update-env
   pm2 reload csph-frontend            # zero-downtime; restarts if it cannot
   pm2 monit                           # CPU/memory, live

 After a rebuild of the SPA, no restart is needed for new asset files, but you
 MUST restart for a changed index.html to be picked up by an already-loaded tab:
   pm2 restart csph-frontend

 Read k8s/scripts/pm2/README.md for the VITE_API_BASE_URL build-time trap, which
 is the single most common way a production SPA silently points at localhost.
──────────────────────────────────────────────────────────────────────────────
EOF
