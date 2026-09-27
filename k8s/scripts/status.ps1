<#
.SYNOPSIS
  One-shot health summary for the CSPH platform: pod status + every host port probe.
#>
$ErrorActionPreference = 'Continue'
$env:Path = [System.Environment]::GetEnvironmentVariable('Path','Machine') + ';' +
            [System.Environment]::GetEnvironmentVariable('Path','User')
$ns = 'csph-gpl'

Write-Host "`n=== POD STATUS ===" -ForegroundColor Cyan
kubectl get pods -n $ns -o wide 2>&1 | ForEach-Object { $_ }

Write-Host "`n=== NOT-READY / CRASHING ===" -ForegroundColor Cyan
$bad = kubectl get pods -n $ns --no-headers 2>$null |
       Where-Object { $_ -notmatch '\sRunning\s' -or $_ -match 'Error|CrashLoop|Pending|ImagePull|Evict' }
if ($bad) { $bad | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow } }
else { Write-Host "  none - all pods Running" -ForegroundColor Green }

Write-Host "`n=== HOST PORT PROBES ===" -ForegroundColor Cyan
$probes = @(
  @{ n='eureka';            u='http://localhost:8761/actuator/health' },
  @{ n='api-gateway';       u='http://localhost:8080/actuator/health' },
  @{ n='auth-service';      u='http://localhost:8081/swagger-ui.html' },
  @{ n='organization-svc';  u='http://localhost:8082/v3/api-docs' },
  @{ n='user-service';      u='http://localhost:8083/v3/api-docs' },
  @{ n='audit-service';     u='http://localhost:8084/v3/api-docs' },
  @{ n='notification-svc';  u='http://localhost:8085/v3/api-docs' },
  @{ n='tour-service';      u='http://localhost:8086/v3/api-docs' },
  @{ n='cylinder-service';  u='http://localhost:8087/v3/api-docs' },
  @{ n='fleet-device-svc';  u='http://localhost:8088/v3/api-docs' },
  @{ n='subsidy-service';   u='http://localhost:8089/v3/api-docs' },
  @{ n='mailhog-ui';        u='http://localhost:8026/' }
)
foreach ($p in $probes) {
  try {
    $r = Invoke-WebRequest -Uri $p.u -TimeoutSec 8 -UseBasicParsing -ErrorAction Stop
    "{0,-20} {1,-4} {2}" -f $p.n, $r.StatusCode, $p.u
  } catch {
    $code = $null
    if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
    if ($code) { "{0,-20} {1,-4} {2}" -f $p.n, $code, $p.u }
    else       { "{0,-20} {1,-4} {2}" -f $p.n, 'ERR', $p.u }
  }
}
