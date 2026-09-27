# Build all 11 service images in parallel, then report honestly.
# Every Dockerfile is multi-stage, so each image compiles its own JAR inside
# Docker -- no host Maven, no host JDK, no ~/.m2 required.
#
#   .\k8s\scripts\build-images.ps1              # all 11
#   .\k8s\scripts\build-images.ps1 auth-service api-gateway   # a subset
#   .\k8s\scripts\build-images.ps1 -Serial      # one at a time (easier to read logs)
param(
  [string[]]$Only = @(),
  [switch]$Serial
)
$ErrorActionPreference = 'Continue'
# This script lives at <repo>/k8s/scripts/, so the repo root is two levels up.
$repoRoot = (Resolve-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))).Path
Set-Location $repoRoot

$all = @(
  'discovery-server','api-gateway','auth-service','organization-service',
  'user-service','audit-service','notification-service','tour-service',
  'cylinder-service','fleet-device-service','subsidy-service'
)
$services = if ($Only.Count -gt 0) { $all | Where-Object { $Only -contains $_ } } else { $all }

$logDir = Join-Path $PSScriptRoot 'build-logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$maxParallel = if ($Serial) { 1 } else { 4 }

$pending = [System.Collections.Queue]::new()
foreach ($s in $services) { $pending.Enqueue($s) }

# Track running children as a plain hashtable: name -> System.Diagnostics.Process
$running = @{}

function Start-Build([string]$svc) {
  $tag = "csph/gpl-${svc}:dev"
  $log = Join-Path $logDir "$svc.log"
  # ABSOLUTE paths only -- a relative -f path gets mangled when Start-Process
  # re-parses the argument list, and docker then reports a bogus
  # "GetFileAttributesEx <service>: file not found".
  $dockerfile = Join-Path $repoRoot "$svc\Dockerfile"
  if (-not (Test-Path -LiteralPath $dockerfile)) { throw "No Dockerfile at $dockerfile" }
  $args = @('build','-f',"`"$dockerfile`"",'-t',$tag,"`"$repoRoot`"")
  $p = Start-Process -FilePath 'docker' -ArgumentList $args -WorkingDirectory $repoRoot `
       -NoNewWindow -PassThru -RedirectStandardOutput $log -RedirectStandardError "$log.err"
  return @{ Svc = $svc; Tag = $tag; Proc = $p; Log = $log }
}

while ($pending.Count -gt 0 -or $running.Count -gt 0) {
  while ($running.Count -lt $maxParallel -and $pending.Count -gt 0) {
    $svc = $pending.Dequeue()
    $running[$svc] = Start-Build $svc
    Write-Output "  -> started  csph/gpl-$($svc):dev"
  }
  Start-Sleep -Seconds 5
  foreach ($svc in @($running.Keys)) {
    $b = $running[$svc]
    if ($b.Proc.HasExited) {
      # Do NOT trust $b.Proc.ExitCode here: Start-Process -NoNewWindow -PassThru does
      # not always populate ExitCode (it comes back empty). The authoritative
      # success signal is whether the daemon actually has the tagged image.
      $has = [bool](docker image inspect $b.Tag 2>$null)
      if ($has) { Write-Output "  OK    $($b.Tag)" }
      else      { Write-Output "  FAIL  $($b.Tag)  (see $($b.Log).err)" }
      $running.Remove($svc)
    }
  }
}

Write-Output "`n=== BUILD REPORT (verified by inspecting the docker daemon) ==="
$ok = 0; $bad = 0
foreach ($svc in $services) {
  $tag = "csph/gpl-${svc}:dev"
  $size = docker image inspect $tag --format '{{.Size}}' 2>$null
  if ($size) { $ok++; "  OK    $tag  $([math]::Round([int64]$size/1MB,1)) MB" }
  else       { $bad++; "  MISS  $tag" }
}
Write-Output "`nBuilt: $ok   Missing: $bad   (of $($services.Count))"
if ($bad -gt 0) { exit 1 }
