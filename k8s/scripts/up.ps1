<#
.SYNOPSIS
  Bring the entire CSPH GPL Fleet Platform up in the local kind cluster.

.DESCRIPTION
  Idempotent. Safe to re-run. Performs, in order:
    1. create the kind cluster (if missing) from k8s/kind-cluster.yaml
    2. build any service image that does not exist yet (all 11 are multi-stage,
       so they compile their own JAR inside Docker -- no host Maven/JDK needed)
    3. `kind load` those images into the node's containerd
    4. `kubectl apply -k k8s/overlays/dev`
    5. wait for every pod to become Ready, then print a health summary

.PARAMETER SkipBuild   assume images already exist, only load + apply
.PARAMETER Rebuild      force `docker build` even if the image is present
.PARAMETER TimeoutSec   how long to wait for all pods (default 900)
#>
param(
  [switch]$SkipBuild,
  [switch]$Rebuild,
  [int]$TimeoutSec = 900
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
  Say "`n[1/5] Creating kind cluster '$cluster'..."
  kind create cluster --name $cluster --config k8s\kind-cluster.yaml --wait 240s
} else {
  Say "`n[1/5] Cluster '$cluster' already exists."
}
kubectl config use-context "kind-$cluster" | Out-Null
kubectl wait --for=condition=Ready node --all --timeout=180s | Out-Null

# ── 2. images ────────────────────────────────────────────────────────────────
if ($SkipBuild) {
  Say "[2/5] Skipping build (--SkipBuild)."
} else {
  Say "[2/5] Building service images (4 at a time)..."
  & "$PSScriptRoot\build-images.ps1" -Rebuild:$Rebuild
}

# ── 3. load images into the node ─────────────────────────────────────────────
Say "`n[3/5] Loading images into the kind node..."
$tags = $services | ForEach-Object { "csph/gpl-$($_)`:dev" }
kind load docker-image --name $cluster @tags 2>&1 | Where-Object { $_ -notmatch 'already present' }

# ── 4. apply ─────────────────────────────────────────────────────────────────
Say "`n[4/5] Applying manifests from k8s/overlays/dev ..."
kubectl apply -k k8s/overlays/dev | Out-Null
Say "      applied."

# ── 5. wait + summary ────────────────────────────────────────────────────────
Say "`n[5/5] Waiting up to ${TimeoutSec}s for every pod to become Ready..."
$deadline = (Get-Date).AddSeconds($TimeoutSec)
do {
  $pending = kubectl get pods -n $ns --no-headers 2>$null |
             Where-Object { $_ -notmatch ' Running .* 0 ' -and $_ -notmatch 'Completed' }
  if (-not $pending) { break }
  Start-Sleep -Seconds 10
} while ((Get-Date) -lt $deadline)

& "$PSScriptRoot\status.ps1"
