<#
.SYNOPSIS
  Bring the entire CSPH GPL Fleet Platform up in the local kind cluster.

.DESCRIPTION
  Idempotent. Safe to re-run. Performs, in order:
    1. create the kind cluster (if missing) from k8s/kind-cluster.yaml
    2. build any service image that does not exist yet (all 11 are multi-stage,
       so they compile their own JAR inside Docker -- no host Maven/JDK needed)
    3. `kind load` those images into the node's containerd
    4. render the overlay with `kubectl kustomize` first (a malformed overlay
       fails here, loudly, instead of as a partially-applied namespace),
       then `kubectl apply -k k8s/overlays/dev`
    5. wait for every pod to become Ready
    6. wait for all 10 applications to REGISTER WITH EUREKA, then print a
       health summary

  STEP 6 IS THE ONE THAT MATTERS. Every JVM Deployment uses a tcpSocket
  readinessProbe, which passes the INSTANT the port binds — before Hibernate,
  before the seeders, and ~30s before the Eureka client's first registration.
  For roughly a minute after apply, pods are Ready but the gateway answers 503
  "Unable to find instance". Pod readiness is necessary and NOT sufficient;
  this script refuses success until the registry holds all 10 apps.

.PARAMETER SkipBuild   assume images already exist, only load + apply
.PARAMETER Rebuild      force `docker build` even if the image is present
.PARAMETER TimeoutSec   how long to wait for all pods (default 900)
.PARAMETER EurekaTimeoutSec   how long to wait for Eureka registration (default 420)
#>
param(
  [switch]$SkipBuild,
  [switch]$Rebuild,
  [int]$TimeoutSec = 900,
  [int]$EurekaTimeoutSec = 420
)
# NOTE on $ErrorActionPreference: this must stay 'Continue', NOT 'Stop'.
# PowerShell 5.1 wraps a native command's STDERR into error records, and kind /
# docker / kubectl all write their normal progress output to stderr. With 'Stop'
# the script dies on the very first progress line ("Creating cluster ...").
# Real failures are caught explicitly where they matter instead.
$ErrorActionPreference = 'Continue'
$env:Path = [System.Environment]::GetEnvironmentVariable('Path','Machine') + ';' +
            [System.Environment]::GetEnvironmentVariable('Path','User')

$repoRoot = (Resolve-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))).Path
Set-Location $repoRoot
$cluster = 'csph-gpl'
$ns      = 'csph-gpl'

$services = @('discovery-server','api-gateway','auth-service','organization-service',
              'user-service','audit-service','notification-service','tour-service',
              'cylinder-service','fleet-device-service','subsidy-service')

function Say($m) { Write-Host $m }

# ── 1. cluster ───────────────────────────────────────────────────────────────
# `kind get clusters` prints "No kind clusters found." to STDERR when there is
# none, and $ErrorActionPreference='Stop' would turn that into a terminating
# error. So run it with stderr swallowed and inspect stdout instead.
$existing = (cmd /c "kind get clusters 2>nul" 2>$null) -split "`r?`n" | Where-Object { $_.Trim() }
if ($existing -notcontains $cluster) {
  Say "`n[1/6] Creating kind cluster '$cluster'..."
  kind create cluster --name $cluster --config k8s\kind-cluster.yaml --wait 240s
} else {
  Say "`n[1/6] Cluster '$cluster' already exists."
}
kubectl config use-context "kind-$cluster" | Out-Null
kubectl wait --for=condition=Ready node --all --timeout=180s | Out-Null

# ── 2. images ────────────────────────────────────────────────────────────────
if ($SkipBuild) {
  Say "[2/6] Skipping build (--SkipBuild)."
} else {
  Say "[2/6] Building service images (4 at a time)..."
  & "$PSScriptRoot\build-images.ps1" -Rebuild:$Rebuild
}

# ── 3. load images into the node ─────────────────────────────────────────────
Say "`n[3/6] Loading images into the kind node..."
$tags = $services | ForEach-Object { "csph/gpl-$($_)`:dev" }
kind load docker-image --name $cluster @tags 2>&1 | Where-Object { $_ -notmatch 'already present' }

# ── 4. apply ─────────────────────────────────────────────────────────────────
Say "`n[4/6] Rendering k8s/overlays/dev (fails loudly here, not half-applied) ..."
kubectl kustomize k8s/overlays/dev | Out-Null
if (-not $?) { throw "kubectl kustomize failed — overlay is malformed, nothing was applied." }
Say "      rendered OK, applying ..."
kubectl apply -k k8s/overlays/dev | Out-Null
Say "      applied."

# ── 5. pods ──────────────────────────────────────────────────────────────────
Say "`n[5/6] Waiting up to ${TimeoutSec}s for every pod to become Ready..."
$deadline = (Get-Date).AddSeconds($TimeoutSec)
do {
  $pending = kubectl get pods -n $ns --no-headers 2>$null |
             Where-Object { $_ -notmatch ' Running .* 0 ' -and $_ -notmatch 'Completed' }
  if (-not $pending) { break }
  Start-Sleep -Seconds 10
} while ((Get-Date) -lt $deadline)
if ($pending) { throw "Pods did not all become Ready in ${TimeoutSec}s. Diagnose: k8s\scripts\debug.sh" }

# ── 6. eureka registration — the gate that pod readiness is not ──────────────
Say "`n[6/6] Waiting up to ${EurekaTimeoutSec}s for all 10 apps to register with Eureka..."
Say "      (tcpSocket probes pass ~30s before registration; the gateway 503s in that window.)"
$eurekaApps = @('API-GATEWAY','AUTH-SERVICE','ORGANIZATION-SERVICE','USER-SERVICE',
  'AUDIT-SERVICE','NOTIFICATION-SERVICE','TOUR-SERVICE','CYLINDER-SERVICE',
  'FLEET-DEVICE-SERVICE','SUBSIDY-SERVICE')
$eurekaDeadline = (Get-Date).AddSeconds($EurekaTimeoutSec)
$missing = @()
do {
  try { $appsXml = (Invoke-WebRequest -Uri 'http://localhost:8761/eureka/apps' -UseBasicParsing -TimeoutSec 10).Content }
  catch { $appsXml = '' }
  $missing = @($eurekaApps | Where-Object { $appsXml -notmatch "<name>$_</name>" })
  if ($missing.Count -eq 0) { break }
  Start-Sleep -Seconds 10
} while ((Get-Date) -lt $eurekaDeadline)
if ($missing.Count -gt 0) { throw "Eureka registration incomplete, missing: $($missing -join ', '). Gateway 503s until this clears. Diagnose: k8s\scripts\debug.sh" }
Say "      all 10 applications registered."

& "$PSScriptRoot\status.ps1"
