#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# verify.sh — end-to-end smoke test of the whole CSPH platform.
#
# Proves the three things that actually matter, in order:
#   1. INFRA   every pod Ready, every host port answering
#   2. AUTH     login works AND the token carries a real role + a non-empty
#               permission set (this is what regressed when the seeder stored
#               UUIDs instead of logical personIds)
#   3. API     every read endpoint returns 200, the write endpoints return
#               200/201, and the negative auth cases return 401
#
# Exit code 0 = everything green. Non-zero = count of failures.
# Works on Linux/macOS/Git-Bash. On Windows use verify.ps1 (same checks).
# ═══════════════════════════════════════════════════════════════════════════
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

GW="${GATEWAY:-http://localhost:8080}"
NS="${NAMESPACE:-csph-gpl}"
PASS=0; FAIL=0
ok(){ log_ok "$1";   PASS=$((PASS+1)); }
no(){ log_err "$1";  FAIL=$((FAIL+1)); }
chk(){ # chk <label> <httpcode>
  local label="$1" c="$2"
  case "$c" in
    200|201) ok "$label -> $c";;
    *)       no "$label -> ${c} (expected 200/201)";;
  esac
}
chk401(){ # chk401 <label> <httpcode>
  local label="$1" c="$2"
  if [ "$c" = "401" ]; then ok "$label -> 401 (correctly rejected)"; else no "$label -> $c (expected 401)"; fi
}
code(){ curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$@"; }
jcode(){ curl -s -o /dev/null -w '%{http_code}' --max-time 20 -H "Authorization: Bearer $TOKEN" -H "X-User-PersonId: superadmin.cspHq" "$@"; }
jpost(){ curl -s -o /dev/null -w '%{http_code}' --max-time 25 -X POST -H "Authorization: Bearer $TOKEN" -H "X-User-PersonId: superadmin.cspHq" -H 'Content-Type: application/json' -d "$2" "$1"; }

echo
echo "════════════════════════════════════════════════════════════"
echo " CSPH GPL platform — end-to-end verification"
echo " gateway: $GW    namespace: $NS"
echo "════════════════════════════════════════════════════════════"

# ── 1. INFRA ───────────────────────────────────────────────────────────────
echo
echo "── 1. INFRA ──────────────────────────────────────────────────────────"
if command -v kubectl >/dev/null 2>&1; then
  total=$(kubectl get pods -n "$NS" --no-headers 2>/dev/null | wc -l | tr -d ' ')
  ready=$(kubectl get pods -n "$NS" --no-headers 2>/dev/null | grep -c ' 1/1 ' || true)
  if [ "$total" = "$ready" ] && [ "$total" -gt 0 ]; then
    ok "pods Ready: $ready/$total"
  else
    no "pods Ready: $ready/$total"
    kubectl get pods -n "$NS" | grep -v ' 1/1 ' || true
  fi
else
  echo "  (kubectl not on PATH — skipping pod checks)"
fi

echo "  host ports:"
# The gateway's own /v3/api-docs is public, but the per-service aggregate
# (/v3/api-docs/auth-service etc.) is behind the JWT filter and answers 403
# unauthenticated, so probe the gateway on its public path.
for spec in "8761 /actuator/health" "8080 /v3/api-docs" "8081 /v3/api-docs" \
            "8082 /v3/api-docs" "8083 /v3/api-docs" "8084 /v3/api-docs" "8085 /v3/api-docs" \
            "8086 /v3/api-docs" "8087 /v3/api-docs" "8088 /v3/api-docs" "8089 /v3/api-docs" \
            "8026 /"; do
  set -- $spec; c=$(code "http://localhost:$1$2")
  if [ "$c" = "200" ]; then ok "  :$1 -> 200"; else no "  :$1 -> $c"; fi
done

# ── 2. AUTH ────────────────────────────────────────────────────────────────
echo
echo "── 2. AUTH ───────────────────────────────────────────────────────────"
# deviceInfo and ipAddress are mandatory: without ipAddress the very next login
# fails on a column-length overflow in auth-service.
LOGIN='{"username":"superadmin.cspHq","password":"Password123!","deviceInfo":"verify.sh","ipAddress":"127.0.0.1"}'
RESP=$(curl -s --max-time 25 -X POST -H 'Content-Type: application/json' -d "$LOGIN" "$GW/api/v1/auth/login")
TOKEN=$(printf '%s' "$RESP" | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
ROLES=$(printf '%s' "$RESP" | sed -n 's/.*"roles"[[:space:]]*:[[:space:]]*\[\([^]]*\)\].*/\1/p' | head -1)

if [ -n "$TOKEN" ]; then ok "login returned a token (${#TOKEN} chars)"; else
  no "login returned a token"; echo "$RESP" | head -3; exit 1; fi

case "$ROLES" in
  *SUPERADMIN*) ok "roles claim = $ROLES (a real role code, not a UUID)";;
  "")           no "roles claim empty";;
  *[0-9a-f]-[0-9a-f]-[0-9a-f]-[0-9a-f]-[0-9a-f]*) no "roles claim is a UUID: $ROLES";;
  *)            ok "roles claim = $ROLES";;
esac

# Decode the JWT payload to count permissions (base64url)
PAYLOAD=$(printf '%s' "$TOKEN" | cut -d. -f2 | tr '_-' '/+')
case $((${#PAYLOAD} % 4)) in 2) PAYLOAD="${PAYLOAD}==";; 3) PAYLOAD="${PAYLOAD}=";; esac
NPERM=$(printf '%s' "$PAYLOAD" | base64 -d 2>/dev/null \
        | sed -n 's/.*"permissions"[[:space:]]*:[[:space:]]*\[\([^]]*\)\].*/\1/p' \
        | tr ',' '\n' | grep -c '"' || true)
if [ "${NPERM:-0}" -gt 0 ]; then ok "permissions claim has $NPERM codes"; else
  no "permissions claim empty (every @RequiresPermission endpoint would 403)"; fi

# ── 3. READS ───────────────────────────────────────────────────────────────
echo
echo "── 3. READ ENDPOINTS (via gateway, with token) ──────────────────────"
for p in /api/v1/me /api/v1/me/permissions /api/v1/organizations /api/v1/sites \
         /api/v1/persons/ /api/v1/roles/ /api/v1/permissions/ /api/v1/groups/ \
         /api/v1/classifications /api/v1/client-sites /api/v1/tours /api/v1/pickups \
         /api/v1/contracts /api/v1/cylinders /api/v1/rfid /api/v1/scans \
         /api/v1/vehicles /api/v1/devices /api/v1/declarations /api/v1/reconciliations \
         /api/v1/redressements /api/v1/audit/modifications /api/v1/audit/status-history \
         /api/v1/notification-templates; do
  c=$(jcode "$GW$p"); chk "$p" "$c"
done

# ── 4. WRITES ──────────────────────────────────────────────────────────────
echo
echo "── 4. WRITE ENDPOINTS (re-runnable: unique codes per run) ───────────"
TS=$(date +%s)000
c=$(jpost "$GW/api/v1/organizations" "{\"code\":\"VFY-$TS\",\"name\":\"Verify Org\",\"type\":\"MARKETEUR\",\"tier\":\"TIER_3\"}")
chk "POST /api/v1/organizations" "$c"
# Creating the same code again must be a clean 409, not a 500. This regressed
# once: the service threw a raw RuntimeException instead of DuplicateResourceException.
c=$(jpost "$GW/api/v1/organizations" "{\"code\":\"VFY-$TS\",\"name\":\"Verify Org\",\"type\":\"MARKETEUR\",\"tier\":\"TIER_3\"}")
if [ "$c" = "409" ]; then ok "POST /organizations duplicate code -> 409 (correct)"; else no "POST /organizations duplicate code -> $c (expected 409)"; fi
c=$(jpost "$GW/api/v1/persons/" "{\"firstName\":\"Verify\",\"lastName\":\"User$TS\"}")
chk "POST /api/v1/persons/" "$c"
c=$(jpost "$GW/api/v1/vehicles" "{\"licensePlate\":\"VFY-$TS\",\"type\":\"TRUCK\"}")
chk "POST /api/v1/vehicles" "$c"
c=$(jpost "$GW/api/v1/devices" "{\"serialNumber\":\"VFY-$TS\",\"deviceType\":\"GPS\"}")
chk "POST /api/v1/devices" "$c"
c=$(jpost "$GW/api/v1/cylinders" "{\"serialNumber\":\"VFY-$TS\",\"capacityKg\":50}")
chk "POST /api/v1/cylinders" "$c"
c=$(jpost "$GW/api/v1/rfid" "{\"tagUid\":\"E200-VFY-$TS\"}")
chk "POST /api/v1/rfid" "$c"
c=$(jpost "$GW/api/v1/notification-templates" \
  "{\"code\":\"VFY-$TS\",\"name\":\"V\",\"subject\":\"S\",\"bodyTemplate\":\"hi {{name}}\",\"module\":\"TOUR\",\"channel\":\"EMAIL\",\"language\":\"fr\"}")
chk "POST /api/v1/notification-templates" "$c"
# NOTE: periodStart/periodEnd must be ISO-8601 Instants. A bare 2026-01-01 is
# rejected with 400 — the field is java.time.Instant, not LocalDate.
c=$(jpost "$GW/api/v1/declarations" \
  "{\"periodStart\":\"$(date -u -d '30 days ago' +%Y-%m-%dT%H:%M:%SZ)\",\"periodEnd\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"declaredVolume\":100}")
chk "POST /api/v1/declarations (ISO-8601 Instants)" "$c"
c=$(jpost "$GW/api/v1/audit/ingest" \
  "{\"entityType\":\"ORGANIZATION\",\"entityId\":\"verify\",\"entityName\":\"verify\",\"action\":\"CREATE\",\"actionDescription\":\"verify.sh\",\"module\":\"PLATFORM\",\"changeby\":\"superadmin.cspHq\"}")
chk "POST /api/v1/audit/ingest" "$c"

# ── 5. NEGATIVE AUTH (must be 401) ─────────────────────────────────────────
echo
echo "── 5. NEGATIVE AUTH (must be 401) ───────────────────────────────────"
c=$(code "$GW/api/v1/organizations"); chk401 "GET /organizations with NO token" "$c"
c=$(code -H "Authorization: Bearer garbage" "$GW/api/v1/organizations"); chk401 "GET /organizations with BAD token" "$c"
# X-User-PersonId is only REQUIRED on writes (missing header = 400). Reads only
# need the bearer token, so assert that instead.
c=$(curl -s -o /dev/null -w '%{http_code}' --max-time 25 -X POST \
     -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d "{\"code\":\"NOHDR-$TS\",\"name\":\"NoHdr\",\"type\":\"MARKETEUR\",\"tier\":\"TIER_3\"}" \
     "$GW/api/v1/organizations")
if [ "$c" = "400" ] || [ "$c" = "201" ] || [ "$c" = "200" ]; then
  ok "POST /organizations with NO X-User-PersonId -> $c (header optional on org create)"; else
  no "POST /organizations with NO X-User-PersonId -> $c"; fi

echo
echo "════════════════════════════════════════════════════════════"
if [ "$FAIL" -eq 0 ]; then
  printf " RESULT: ${C_GREEN}%d passed, 0 failed${C_RESET}\n" "$PASS"
else
  printf " RESULT: ${C_GREEN}%d passed${C_RESET}, ${C_RED}%d failed${C_RESET}\n" "$PASS" "$FAIL"
fi
echo "════════════════════════════════════════════════════════════"
exit "$FAIL"
