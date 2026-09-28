<#
.SYNOPSIS
  End-to-end smoke test of the whole CSPH platform (Windows twin of verify.sh).

.DESCRIPTION
  Proves the three things that actually matter, in order:
    1. INFRA   every pod Ready, every host port answering
    2. AUTH     login works AND the token carries a real role + a non-empty
                permission set
    3. API     every read endpoint returns 200, the write endpoints return
                200/201, and the negative auth cases return 401

  Exit code 0 = everything green, else the count of failures.

  .\k8s\scripts\verify.ps1
  .\k8s\scripts\verify.ps1 -Gateway http://localhost:18080   # probe compose stack
#>
param(
  [string]$Gateway = 'http://localhost:8080',
  [string]$Namespace = 'csph-gpl'
)
$ErrorActionPreference = 'Continue'
$GW = $Gateway.TrimEnd('/')
$script:pass = 0; $script:fail = 0
function Ok($m)  { Write-Host "  [ ok ] $m" -ForegroundColor Green; $script:pass++ }
function No($m)  { Write-Host "  [fail] $m" -ForegroundColor Red;   $script:fail++ }
function Chk($label, $c) {
  if ($c -eq 200 -or $c -eq 201) { Ok "$label -> $c" } else { No "$label -> $c (expected 200/201)" }
}
function Chk401($label, $c) {
  if ($c -eq 401) { Ok "$label -> 401 (correctly rejected)" } else { No "$label -> $c (expected 401)" }
}
function Code($url, $headers = @{}, $method = 'GET', $body = $null) {
  try {
    $p = @{ Uri = $url; Method = $method; Headers = $headers; TimeoutSec = 20; UseBasicParsing = $true; ErrorAction = 'Stop' }
    if ($body -ne $null) { $p.Body = $body; $p.ContentType = 'application/json' }
    (Invoke-WebRequest @p).StatusCode
  } catch {
    $r = $_.Exception.Response
    if ($r) { [int]$r.StatusCode } else { 'ERR' }
  }
}

Write-Host "`n================================================================"
Write-Host " CSPH GPL platform - end-to-end verification"
Write-Host " gateway: $GW    namespace: $Namespace"
Write-Host "================================================================`n"

# ── 1. INFRA ──
Write-Host "── 1. INFRA ──"
if (Get-Command kubectl -ErrorAction SilentlyContinue) {
  $pods = kubectl get pods -n $Namespace --no-headers 2>$null
  $total = @($pods | Where-Object { $_.Trim() }).Count
  $ready = @($pods | Where-Object { $_ -match ' 1/1 ' }).Count
  if ($total -gt 0 -and $total -eq $ready) { Ok "pods Ready: $ready/$total" }
  else { No "pods Ready: $ready/$total"; $pods | Where-Object { $_ -notmatch ' 1/1 ' } | ForEach-Object { Write-Host "    $_" } }
} else { Write-Host "  (kubectl not on PATH - skipping pod checks)" }

Write-Host "  host ports:"
foreach ($spec in @('8761 /actuator/health','8080 /v3/api-docs','8081 /v3/api-docs',
    '8082 /v3/api-docs','8083 /v3/api-docs','8084 /v3/api-docs','8085 /v3/api-docs',
    '8086 /v3/api-docs','8087 /v3/api-docs','8088 /v3/api-docs','8089 /v3/api-docs','8026 /')) {
  $port, $path = $spec -split ' ', 2
  $c = Code "http://localhost:${port}${path}"
  if ($c -eq 200) { Ok "  :$port -> 200" } else { No "  :$port -> $c" }
}

# ── 2. AUTH ──
Write-Host "`n── 2. AUTH ──"
$loginBody = '{"username":"superadmin.cspHq","password":"Password123!","deviceInfo":"verify.ps1","ipAddress":"127.0.0.1"}'
try {
  $login = Invoke-WebRequest -Uri "$GW/api/v1/auth/login" -Method POST -Body $loginBody `
    -ContentType 'application/json' -TimeoutSec 25 -UseBasicParsing -ErrorAction Stop | ConvertFrom-Json
} catch { No "login request failed: $($_.Exception.Message)"; exit 1 }
$TOKEN = $login.data.accessToken
$ROLES = $login.data.roles
if ($TOKEN) { Ok "login returned a token ($($TOKEN.Length) chars)" } else { No "login returned a token"; exit 1 }
if ($ROLES -contains 'SUPERADMIN') { Ok "roles claim = $($ROLES -join ',') (a real role code, not a UUID)" }
elseif (-not $ROLES) { No "roles claim empty" }
elseif ($ROLES -match '[0-9a-f]{8}-') { No "roles claim is a UUID: $($ROLES -join ',')" }
else { Ok "roles claim = $($ROLES -join ',')" }

# JWT payload: base64url decode, count the permissions claim.
$payload = $TOKEN.Split('.')[1].Replace('-','+').Replace('_','/')
while ($payload.Length % 4) { $payload += '=' }
$nPerm = ([System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload)) |
  ConvertFrom-Json).permissions.Count
if ($nPerm -gt 0) { Ok "permissions claim has $nPerm codes" }
else { No "permissions claim empty (every @RequiresPermission endpoint would 403)" }

$JH = @{ Authorization = "Bearer $TOKEN"; 'X-User-PersonId' = 'superadmin.cspHq' }

# ── 3. READS ──
Write-Host "`n── 3. READ ENDPOINTS (via gateway, with token) ──"
foreach ($p in @('/api/v1/me','/api/v1/me/permissions','/api/v1/organizations','/api/v1/sites',
    '/api/v1/persons/','/api/v1/roles/','/api/v1/permissions/','/api/v1/groups/',
    '/api/v1/classifications','/api/v1/client-sites','/api/v1/tours','/api/v1/pickups',
    '/api/v1/contracts','/api/v1/cylinders','/api/v1/rfid','/api/v1/scans',
    '/api/v1/vehicles','/api/v1/devices','/api/v1/declarations','/api/v1/reconciliations',
    '/api/v1/redressements','/api/v1/audit/modifications','/api/v1/audit/status-history',
    '/api/v1/notification-templates')) {
  Chk $p (Code "$GW$p" $JH)
}

# ── 4. WRITES (re-runnable: unique codes per run) ──
Write-Host "`n── 4. WRITE ENDPOINTS (re-runnable: unique codes per run) ──"
$TS = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds().ToString() + '000'
Chk 'POST /api/v1/organizations' (Code "$GW/api/v1/organizations" $JH 'POST' "{`"code`":`"VFY-$TS`",`"name`":`"Verify Org`",`"type`":`"MARKETEUR`",`"tier`":`"TIER_3`"}")
$c = Code "$GW/api/v1/organizations" $JH 'POST' "{`"code`":`"VFY-$TS`",`"name`":`"Verify Org`",`"type`":`"MARKETEUR`",`"tier`":`"TIER_3`"}"
if ($c -eq 409) { Ok 'POST /organizations duplicate code -> 409 (correct)' } else { No "POST /organizations duplicate code -> $c (expected 409)" }
Chk 'POST /api/v1/persons/' (Code "$GW/api/v1/persons/" $JH 'POST' "{`"firstName`":`"Verify`",`"lastName`":`"User$TS`"}")
Chk 'POST /api/v1/vehicles' (Code "$GW/api/v1/vehicles" $JH 'POST' "{`"licensePlate`":`"VFY-$TS`",`"type`":`"TRUCK`"}")
Chk 'POST /api/v1/devices' (Code "$GW/api/v1/devices" $JH 'POST' "{`"serialNumber`":`"VFY-$TS`",`"deviceType`":`"GPS`"}")
Chk 'POST /api/v1/cylinders' (Code "$GW/api/v1/cylinders" $JH 'POST' "{`"serialNumber`":`"VFY-$TS`",`"capacityKg`":50}")
Chk 'POST /api/v1/rfid' (Code "$GW/api/v1/rfid" $JH 'POST' "{`"tagUid`":`"E200-VFY-$TS`"}")
Chk 'POST /api/v1/notification-templates' (Code "$GW/api/v1/notification-templates" $JH 'POST' "{`"code`":`"VFY-$TS`",`"name`":`"V`",`"subject`":`"S`",`"bodyTemplate`":`"hi {{name}}`",`"module`":`"TOUR`",`"channel`":`"EMAIL`",`"language`":`"fr`"}")
$now = [DateTime]::UtcNow; $ago = $now.AddDays(-30)
Chk 'POST /api/v1/declarations (ISO-8601 Instants)' (Code "$GW/api/v1/declarations" $JH 'POST' "{`"periodStart`":`"$($ago.ToString('yyyy-MM-ddTHH:mm:ssZ'))`",`"periodEnd`":`"$($now.ToString('yyyy-MM-ddTHH:mm:ssZ'))`",`"declaredVolume`":100}")
Chk 'POST /api/v1/audit/ingest' (Code "$GW/api/v1/audit/ingest" $JH 'POST' "{`"entityType`":`"ORGANIZATION`",`"entityId`":`"verify`",`"entityName`":`"verify`",`"action`":`"CREATE`",`"actionDescription`":`"verify.ps1`",`"module`":`"PLATFORM`",`"changeby`":`"superadmin.cspHq`"}")

# ── 5. NEGATIVE AUTH ──
Write-Host "`n── 5. NEGATIVE AUTH (must be 401) ──"
Chk401 'GET /organizations with NO token' (Code "$GW/api/v1/organizations")
Chk401 'GET /organizations with BAD token' (Code "$GW/api/v1/organizations" @{ Authorization = 'Bearer garbage' })

Write-Host "`n================================================================"
if ($script:fail -eq 0) { Write-Host " RESULT: $($script:pass) passed, 0 failed" -ForegroundColor Green }
else { Write-Host " RESULT: $($script:pass) passed, $($script:fail) failed" -ForegroundColor Red }
Write-Host "================================================================`n"
exit $script:fail
