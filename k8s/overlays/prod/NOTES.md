# k8s/overlays/prod — production overlay notes

Every production-only decision in this directory, why it was made, what it costs,
and what must change before a real cluster. Written to be read by whoever is on
call, not by whoever wrote it.

**This overlay runs on ONE node. That is not production Kubernetes.** It is
production-*shaped*: the manifests are what you would deploy to a real cluster,
and the one-node cluster cannot satisfy half of them. Section 9 is the honest
account of what will and will not work here.

---

## 0. Contents

| File | What it is |
|---|---|
| `kustomization.yaml` | image remap, labels, and the three patch files |
| `patches/jvm-workloads-prod.yaml` | the 11 stateless JVMs: replicas, strategy, probes, spread, resources |
| `patches/postgres-prod.yaml` | the 9 databases: explicit `replicas: 1`, resources, storage, soft spread |
| `patches/configmaps-prod.yaml` | `SPRING_PROFILES_ACTIVE: prod` on 9 ConfigMaps |
| `poddisruptionbudgets.yaml` | 11 PDBs, `minAvailable: 50%` |
| `horizontalpodautoscalers.yaml` | 10 HPAs (gateway + 9 services) |
| `networkpolicies.yaml` | default-deny ingress + 15 allow rules |
| `imagepull-secret.md` | how to add a registry pull secret, and what to replace it with |

Nothing here edits `k8s/base/` or `k8s/overlays/dev/`. Everything is additive.

---

## 1. Image remap: the kustomize `images:` transformer

`kustomization.yaml` remaps all 11 images from `csph/gpl-<svc>:dev` to
`ghcr.io/csp-hq/gpl-<svc>:v1.0.0` using the `images:` transformer — the same
mechanism as `kustomize edit set image`.

The alternative was an 11-document patch file hardcoding new image strings. The
transformer was chosen because:

* **The base stays the single source of truth for what runs locally.** One line
  per service changes the registry; zero lines change the base.
* **A missed service is a visible diff.** With a patch file, forgetting
  `subsidy-service` means it silently stays on `csph/gpl-subsidy-service:dev`
  and pulls from an image that only exists on the kind node. With the
  transformer, the `images:` list is the checklist.
* It rewrites `spec.containers[*].image` in place, so `imagePullPolicy` and
  everything else in the container is untouched.

`imagePullPolicy: IfNotPresent` is already correct in the base and is left
alone. It is right for prod **provided the tag is immutable** — which is the
next section.

Public base images (`postgres:16-alpine`, `timescale/timescaledb-ha:pg16`,
`axllent/mailpit:latest`) are deliberately NOT remapped. They are already public
upstream. `mailpit` keeps `imagePullPolicy: Always` because its tag is
`:latest`, which is the one tag that is not immutable — see section 10.

### The promotion workflow: dev → staging → prod

Tags are the whole mechanism. There is exactly one thing to change to promote a
build, and it is one line in `kustomization.yaml`.

```
  build            tag              where it is applied
  ───────────────  ───────────────  ───────────────────────────────────
  local dev        :dev             k8s/overlays/dev, loaded with
                                     `kind load docker-image` — never
                                     pushed anywhere
  candidate        :v1.4.0-rc3      staging overlay, pushed to ghcr.io
  release          :v1.4.0          prod overlay (this directory)
  rollback         :v1.3.2          change the one tag back
```

Concretely:

```bash
# 1. build and push a release image from the backend repo
./k8s/scripts/build-images.sh --tag v1.4.0
for svc in discovery-server api-gateway auth-service organization-service \
           user-service audit-service notification-service tour-service \
           cylinder-service fleet-device-service subsidy-service; do
  docker push ghcr.io/csp-hq/gpl-$svc:v1.4.0
done

# 2. promote — one line per service in k8s/overlays/prod/kustomization.yaml
#    newTag: v1.4.0     (x11), or:
cd k8s/overlays/prod
kustomize edit set image \
  csph/gpl-discovery-server=ghcr.io/csp-hq/gpl-discovery-server:v1.4.0
# ... etc

# 3. rehearse the exact manifest that will ship, before it ships
kubectl kustomize k8s/overlays/prod | kubectl apply --dry-run=server -f -

# 4. roll out
kubectl apply -k k8s/overlays/prod
kubectl -n csph-gpl rollout status deploy --timeout=10m
```

Three rules that this workflow depends on:

1. **Never reuse a tag.** `:v1.4.0` must mean exactly one build forever. With
   `IfNotPresent`, reusing a tag means some nodes keep serving the old bits and
   a rollout changes nothing on them. This is the single most common way a
   "successful" deploy leaves the cluster half-updated.
2. **Tag what you built, promote by retagging the reference.** Never rebuild for
   prod; the bytes that passed staging are the bytes that ship.
3. **Rollback is a one-line change**, not a rebuild. That is the entire point of
   immutable tags, and it is worth keeping the previous tag addressable in
   ghcr.io (do not enable tag deletion) so a rollback six weeks later still
   resolves.

There is no staging overlay in this repository yet. Adding
`k8s/overlays/staging/` as a copy of prod with `newTag: v1.4.0-rc3` and
`SPRING_PROFILES_ACTIVE: dev` is the natural next step; it is not here because a
staging overlay nobody has asked for is a maintenance liability, not a feature.

---

## 2. `SPRING_PROFILES_ACTIVE: prod` — and the trap underneath it

Nine ConfigMaps change `dev` → `prod`. `api-gateway` and `discovery-server` are
not touched: neither has a `SPRING_PROFILES_ACTIVE` today, neither has a
datasource, and neither has an `application-prod.yml`. Pinning a profile there
would be a claim about a file that does not exist.

### ⚠ This is the most dangerous line in the overlay

The five seeders are annotated `@Profile({"dev", "test", "local", "default"})`:

| Seeder | Service |
|---|---|
| `AuthDataInitializer` | auth-service |
| `DataInitializer` | organization-service |
| `UserDataInitializer` | user-service |
| `RolesPermissionsInitializer` | user-service |
| `TourDataInitializer` | tour-service |

The dev overlay sets `dev` precisely so they run deterministically. Under
`prod`:

* Hibernate `ddl-auto: update` **still runs** — it is not profile-gated — so the
  **schema is created**.
* The seeders **do not run** — so every table comes back **empty**.

The result is a database that is structurally perfect and contains no users, no
roles, no permissions, no organisations and no tours. Every login fails. And
nothing in the logs says why, because **nothing failed** — the application
started cleanly and the table it needed was simply empty.

Consequences you must act on before the first real deploy:

* `k8s/scripts/reset-db.sh` **refuses to run** under this profile without
  `--allow-prod`, precisely because of this. Do not reach for that flag to "fix"
  a problem: it will not fix one, it will create one.
* **Load the data explicitly**, from a migration job, not from application
  startup. `csph_gpl_schema_v6_2.sql` and `V1__seed_test_data.sql` in the repo
  root are the inputs. Seed data belongs in a migration.
* The alternative — adding `prod` to the `@Profile` lists — is a Java change
  (out of scope) *and* the wrong answer: an application that seeds itself on
  every start will fight every real migration you ever write.

---

## 3. Replicas: 2 stateless, 1 per database

### Why the databases stay at 1

Scaling a PostgreSQL StatefulSet to 2 without a replication mechanism does not
give you a highly-available database. It gives you **two independent writers
behind one ClusterIP Service**:

```
  POST /login  ->  pod-0, which has the users
  POST /order  ->  pod-1, which has never heard of them
```

kube-proxy load-balances per TCP flow, not per transaction, so one
login-then-navigate sequence can silently hit two different databases. The
failure is not a crash — it is missing rows, intermittently, for one user, in a
way nobody can reproduce. **That is strictly worse than being down, because being
down is obvious.**

Three independent reasons 1 is the only correct value:

1. **No replication is configured.** No Patroni, no pgpool, no streaming config,
   no failover proxy anywhere in this repository.
   `timescale/timescaledb-ha:pg16` provides the TimescaleDB *extensions* — it is
   not a HA operator, and the name invites exactly this misreading.
2. **The volumes cannot be shared.** Every `data-*` PVC is `ReadWriteOnce`,
   exclusive per node. Two replicas could not even be co-located to replicate.
3. **The applications cannot use a replica set.** All nine
   `SPRING_DATASOURCE_URL` values name a single logical host
   (`jdbc:postgresql://postgres-auth:5432/...`) with no replica-set or
   multi-host parameter. A correct replica set would be invisible to them
   without a pooler in front.

`replicas: 1` is written **explicitly** in `patches/postgres-prod.yaml`, not
merely inherited. An explicit 1 is a decision a reviewer can see; an inherited 1
is an omission somebody will "fix" in six months.

**What real production needs**, in order of preference:

1. **Managed Postgres** (Cloud SQL / RDS / Azure Database) and delete these
   StatefulSets. Nine separate databases is nine backups, nine failovers and nine
   credential rotations; a managed service gives all of that away.
2. **An operator** — CloudNativePG or Patroni — which owns replication, failover
   and the anti-affinity rules this file cannot express. The manifest shape
   changes completely.
3. **At minimum**: PgBouncer or HAProxy in transaction-pooling mode in front of
   a replicated pair, with the JDBC URL pointing at the pooler.

None of that is here, because all three require changing the application
configuration files, which are explicitly out of scope. It is item 1 on the
checklist in section 11.

### Why 2 for everything else

Two replicas is the floor for any availability claim: a node drain, a rolling
update or a crash then costs zero availability. One replica means every deploy of
every service is a 30-45 second outage for that service.

**mailpit stays at 1.** It is an SMTP sink, it holds no application data, two
replicas would just split captured mail across two inboxes, and its own
`k8s/base/mailpit.yaml` note already explains why its host ports were chosen
carefully (1025/8025 are taken on this machine).

### ⚠ The replicas/HPA conflict

`patches/jvm-workloads-prod.yaml` sets `replicas: 2`, and
`horizontalpodautoscalers.yaml` targets the same Deployments. **Those are two
controllers writing one field, and the Deployment controller wins on every
apply:**

```
14:00  HPA scales auth-service 2 -> 5
14:10  someone runs kubectl apply -k k8s/overlays/prod
14:10  Deployment controller resets spec.replicas to 2
14:11  3 pods terminated
14:11  HPA scales back up to 5
...    forever, on every apply
```

`minReplicas: 2` on every HPA means the two at least agree at the bottom of the
range, so nothing flaps at rest. The fix, before the first real deploy:

* **(a) Delete the `replicas:` key** from the 11 Deployments in
  `patches/jvm-workloads-prod.yaml` and let the HPA own it. Cleanest.
* **(b) Add the legacy annotation** to each pod template:
  `autoscaling/v2: "<hpa-name>"`. Works, deprecated, and it is being removed.

Until one of those is done these HPAs can only ever hold at 2. That is still a
working configuration — 2 replicas, a PDB, production resources — it is just not
an autoscaling one.

---

## 4. The startupProbe

Added to **all 11** JVM Deployments. The brief said 10 (the workloads that
register in Eureka: gateway + 9 services); `discovery-server` is the 11th JVM and
gets one too, because it has exactly the same cold-start risk and no reason to be
the exception. Flagged here so the discrepancy is a decision, not an oversight.

### The failure it prevents

The base has `livenessProbe.initialDelaySeconds: 90` against a `tcpSocket` probe.
On a node also running a build, or on a cold page cache, a JVM can take longer
than 90s to bind its port. Then:

```
t=0    container starts
t=90   liveness fires for the FIRST time, port not bound yet
t=90+  kubelet kills the container
t=95   container starts again from scratch
t=185  killed again                <-- and again, forever
```

A restart loop that **never converges**: the pod can never become Ready, because
it is killed before it is ever allowed to finish starting. It presents as "the
service is broken" and it is actually "the probe is impatient".

A `startupProbe` removes the failure mode by construction. **While it has not
succeeded, the liveness and readiness probes are not run at all.** The JVM gets
an unconditional `failureThreshold: 60 × periodSeconds: 5 = 300s` to bind its
port, and only then does normal supervision begin. 300s is comfortably above the
worst observed start here (~45s) and above a genuinely cold node.

Consequence worth knowing: `initialDelaySeconds` on liveness/readiness is
*ignored* until the `startupProbe` succeeds. The values in the patch (0 and 5) are
therefore "how long after startup do we start judging", not "how long do we
wait".

`readinessProbe.failureThreshold` drops from 20 to 3 (30s) and `initialDelaySeconds`
from 45 to 5. Now that a startupProbe gates the whole sequence, the old 45s
initial delay is dead weight, and a 20-failure readiness threshold means a pod
stays in `Running 0/1` for 200s after a real problem instead of 30s — long enough
for an operator to have stopped watching.

### Where the probes go, and how that was found

`livenessProbe`, `readinessProbe` and `startupProbe` are **container** fields.
Writing them as siblings of `containers:` under `spec.template.spec` produces a
manifest that:

* renders without complaint (`kubectl kustomize` validates no schemas),
* passes `kubectl apply --dry-run=client` without complaint (it does no schema
  validation at all),
* and is rejected by the API server with
  `strict decoding error: unknown field "spec.template.spec.startupProbe"`.

It is also a silent no-op in the worst way: because the misplaced probe never
merges with the base, the base's probes stay exactly as they were
(`initialDelaySeconds: 90` / `45`) and the intended 300-second grace is simply
absent.

**Only `kubectl apply --dry-run=server` catches this**, which is why the
validation procedure in section 12 is server-side and not client-side.

### tour-service keeps tcpSocket

tour-service *configures* `management.endpoints.web.exposure.include` in its
`application.yml`, but that block is inert without
`spring-boot-starter-actuator` on the classpath, and it is not there. An
`httpGet` probe on `/actuator/health` would sit in a permanent restart loop.
The base's reasoning is carried into the prod patch unchanged. `discovery-server`
is the only JVM that genuinely has actuator, so it is the only one using
`httpGet` — and the only one where that is honest.

---

## 5. The dev rollout strategy, removed

The base carries `maxSurge: 0 / maxUnavailable: 1`. That is **correct on one
node** — it means the node never has to hold two copies of a 1.5 GiB JVM at once,
which is a real constraint on a kind node and the reason the live cluster's
events show `Insufficient memory` while pods queue.

It is **wrong on three nodes**, where it means every rollout takes the service
fully down for the length of one cold JVM start. Production uses
`maxSurge: 1 / maxUnavailable: 0`, which keeps at least one pod serving
throughout.

---

## 6. Spread and anti-affinity

`topologySpreadConstraints` over `kubernetes.io/hostname` with
`whenUnsatisfiable: DoNotSchedule`, plus
`podAntiAffinity: requiredDuringSchedulingIgnoredDuringExecution`, on all 11
stateless workloads. The brief asked for both; either alone is sufficient, and a
real cluster would normally keep one. Both are here.

The databases get `topologySpreadConstraints: ScheduleAnyway` and **no**
anti-affinity term. A soft "prefer another node" is expressed by the spread
constraint alone, and `DoNotSchedule` on a 1-replica database would be the
difference between a preference and an outage.

### A Kubernetes 1.37 API change, found by validating

`preferredDuringSchedulingIgnoredDuringExecution` is the one affinity form this
overlay deliberately avoids, and the reason is worth recording because it will
bite anyone who adds a soft anti-affinity term later.

**Kubernetes 1.37 no longer accepts the inlined `PodAffinityTerm` inside
`WeightedPodAffinityTerm`.** The API server rejects the traditional spelling:

```
strict decoding error: unknown field
  "spec.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].labelSelector",
  unknown field "...[0].topologyKey"
```

and accepts only the newly-nested form:

```yaml
preferredDuringSchedulingIgnoredDuringExecution:
- weight: 100
  podAffinityTerm:            # <-- required from 1.37
    topologyKey: kubernetes.io/hostname
    labelSelector: { matchLabels: { ... } }
```

That nested spelling is **1.37-only** — the field did not exist before — so
writing it here would fail on any older API server, and writing the old inlined
spelling fails on this one. The spread constraint sidesteps the trade-off
entirely, which is why the databases have one and no affinity term.

`requiredDuringSchedulingIgnoredDuringExecution` is **unaffected** and still
takes the flat form, because `PodAffinityTerm` is not a wrapper there. That
asymmetry is why this is safe rather than a latent problem in the 11 stateless
workloads.

Reproduce either spelling in ten seconds:

```bash
kubectl apply --dry-run=server -f pod-with-that-affinity.yaml
```

---

## 7. PodDisruptionBudgets

`minAvailable: 50%` on all 11 stateless workloads.

A PDB constrains the **eviction API only**. It does nothing about a node
failing, a pod being OOMKilled, or a bad image. It governs `kubectl drain`, node
upgrades, and cluster-autoscaler scale-down. Everything involuntary is handled by
having more than one replica.

The percentage form is used rather than an integer on purpose: 50% of 3 (after a
scale-up) is 2, and a percentage keeps meaning "half" as the replica count moves
under the HPA. A hardcoded `minAvailable: 1` stops protecting anything the moment
the HPA takes a service above 2.

**No PDB for the databases.** At 1 replica, any PDB that protects the pod blocks
every drain forever and `kubectl drain` hangs at the end waiting for a budget
that can never be satisfied. `maxUnavailable: 1` would be the honest value — it
permits the disruption and accepts the downtime — but encoding a confusing
object is worse than stating the consequence here: **a node drain takes the
databases down.**

---

## 8. HorizontalPodAutoscalers

`autoscaling/v2`, on the gateway and the 9 services. Not on `discovery-server`
(split-brain, section 10) and not on the databases (section 3).

### ⚠ These are inert without metrics-server

The kind cluster has **no metrics-server** (verified: `kube-system` contains only
coredns, etcd, kindnet, kube-apiserver, kube-controller-manager, kube-proxy,
kube-scheduler). Without a metrics provider every HPA here reports
`ScalingActive=False  ScalingLimited=False  <unknown>/<unknown>` and never
scales.

That is a **silent no-op**, which is worse than a failure: the manifests look like
they are working. Install it before trusting any of this:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl -n kube-system rollout status deploy/metrics-server
kubectl top pods -n csph-gpl          # must print numbers, not "Metrics API not available"
```

`kubectl top` erroring is the one-line check that tells you the ten HPAs are
decorative.

### Why two metrics, not one

A single CPU target is the usual example and the wrong default for a
request-serving service. CPU-only scaling cannot see a service saturated on
**connection pools**, on a **downstream database**, or on **thread contention**
while sitting at 40% CPU — which is exactly the shape of the failure this
platform has, where the only symptom is "the gateway takes four seconds". So
every HPA targets CPU at 70% **and** memory at 75%.

`behavior` is explicit on every HPA. The defaults (scale up 100% every 15s,
scale down 50% every 15s) are a fork bomb during a spike: a service needing 3
more pods can create 6, each starting a JVM and hammering the databases during
its own cold start.

| | value | why |
|---|---|---|
| `scaleUp` policies | +2 pods / 60s | bounded |
| `scaleUp` stabilization | 0s | react now; 60s of waiting is 60s of 503s |
| `scaleDown` policies | −1 pod / 120s | one at a time |
| `scaleDown` stabilization | 300s | a brief dip must not delete a pod that is about to be needed; on a 2-replica service that is the difference between a blip and an outage |

`terminationGracePeriodSeconds` is 30 in the base, so a 120s scale-down cooldown
is arithmetic, not caution.

---

## 9. Will this overlay schedule on the current cluster? No.

Read this before trying.

| Requirement | This cluster | Result |
|---|---|---|
| 2 replicas, `DoNotSchedule` spread, `required` anti-affinity | **1 node, 1 hostname** | the 2nd replica of all 11 workloads is **unschedulable, Pending forever** |
| 20 JVM pods @ 500m/1Gi + 9 PG @ 500m/1Gi | 1 node | needs **~12 vCPU / 30 GiB** of allocatable requests |

The failure is loud and specific, which is the good news. `kubectl describe` will
say:

```
0/1 nodes are available: 1 node(s) had untolerated taint ...
pod anti-affinity rules not satisfied
```
or `topology spread constraint not satisfied`.

Three ways to work around it, in order of preference:

1. **Use a separate namespace or cluster.** Correct: dev and prod must not share
   a namespace anyway (see section 10).
2. **`kubectl apply` only part of it** — the ConfigMaps, the NetworkPolicies, the
   PDBs and the HPAs are all fine on one node:
   ```bash
   kubectl apply -k k8s/overlays/prod   # then scale the Deployments back to 1
   kubectl -n csph-gpl scale deploy --all --replicas=1
   ```
3. **Temporarily relax the constraints** by commenting out the
   `topologySpreadConstraints` and `affinity` blocks in
   `patches/jvm-workloads-prod.yaml`. Do not do this in a real cluster, and do
   not commit the change.

`debug.sh` will report the Pending replicas clearly. `k8s/scripts/status.sh`
reports them too, and exits non-zero.

---

## 10. ⚠ The two highest-risk items in this overlay

### (a) `discovery-server` at 2 replicas is a split-brain registry

The brief asks for 2 replicas on discovery. **That is not safe as configured,**
and it is the most dangerous thing in this directory.

`discovery-server/src/main/resources/application.yml` is nine lines long:

```yaml
server: { port: 8761 }
eureka:
  client: { register-with-eureka: false, fetch-registry: false }
  server:  { wait-time-in-ms-when-sync-empty: 0 }
```

There is **no `eureka.cluster.peer.*` configuration and no peer Service.** Two
replicas are therefore two completely independent registries:

* the 9 clients round-robin their registrations across both
* the gateway fetches the registry from one of them
* a route resolves on one request and 503s on the next

The symptom is **intermittent, unreproducible 503s** — the hardest kind of bug to
diagnose, and `debug.sh` section 5 would show all 10 apps "registered" while the
platform is failing.

Fixing it properly means configuring peer replication
(`eureka.cluster.peer.enabled=true`, `eureka.cluster.peer.replicas`, a headless
Service for stable per-pod DNS, and
`eureka.server.enable-self-preservation=false` so a replica cannot silently serve
a stale registry) — which is a change to `application.yml`, explicitly out of
scope here.

**Until that is done, either run one `discovery-server` replica, or accept that
you have two registries.** The safest single-line change today is:

```yaml
# patches/jvm-workloads-prod.yaml
  replicas: 1        # was 2 — see section 10(a) of NOTES.md
```

on the `discovery-server` document only.

### (b) `spec.selector` is immutable, so prod cannot be applied over dev

`kustomization.yaml` uses `labels:` with `includeSelectors: true`, which writes
`app.kubernetes.io/environment: prod` into every `spec.selector.matchLabels`.
**`spec.selector` is immutable on both Deployment and StatefulSet.** Applying prod
into a namespace where dev has already been applied fails on all 21 workloads:

```
Error from server (Invalid): error when applying patch:
Deployment.apps "api-gateway" is invalid:
  [spec.selector: Invalid value: {...}: field is immutable]
```

This is not a defect in the manifests — it is the API correctly refusing to
change a selector in place, because a Deployment's selector determines which
pods it owns and changing it silently orphans ReplicaSets.

Before the first prod apply, choose:

* **(a) a separate namespace or cluster** — correct, and the answer;
* **(b) `kubectl delete -k k8s/overlays/dev`** first. This removes the 21
  workloads; the PVCs survive, so the databases are untouched.

Verified (section 12): with the 21 workloads dry-run under collision-free names,
**all 122 objects pass full API-server schema validation with zero errors.** The
immutability collision is the only thing that fails.

### (c) Service type: NodePort, deliberately, and no Ingress

The brief is explicit and the reasoning holds: the application `yml` files
hard-code 8080-8089, and changing them is out of scope. An Ingress would need the
gateway to listen on 80/443 and the SPA to follow, which is an application
change.

So: **NodePort is kept, no Ingress is added.** Consequences, stated rather than
discovered:

* the gateway is on host **8080**, plain HTTP, no TLS;
* `k8s/kind-cluster.yaml` maps the host ports, so this overlay still depends on
  that file even on a real cluster (a real cluster has no kind node — the
  equivalent is a LoadBalancer Service, which is its own piece of work);
* before real production: a cloud LoadBalancer or an Ingress controller with
  TLS termination, then `allow-kubelet-probes` narrowed to the load-balancer's
  CIDR instead of `0.0.0.0/0`.

---

## 11. NetworkPolicy

`default-deny-ingress` (namespace-wide) plus 15 allow rules. Read the header of
`networkpolicies.yaml` for the per-rule reasoning; the three things that are not
obvious from the YAML:

### ⚠ The CNI must enforce them, and kindnetd does not

The kind cluster runs **kindnetd**, which has no policy engine. The API server
accepts every NetworkPolicy object, stores it, and kindnetd **ignores it**. Every
rule below will look applied and protect nothing.

Prove it rather than assuming:

```bash
kubectl -n csph-gpl exec deploy/auth-service -- \
  wget -qO- --timeout=3 http://audit-service:8084/v3/api-docs | head -1
```

With policies enforced this must **fail** (auth-service may not reach
audit-service). On kindnetd it will **succeed**, and these policies are
documentation, not enforcement.

Enforcing CNIs: Calico, Cilium, Antrea, kube-router. Managed clusters have it by
default. To make kind enforce it, replace kindnetd with Calico — which needs a
change to `k8s/kind-cluster.yaml` (`disableDefaultCNI: true`), out of scope here.

### `allow-kubelet-probes` is mandatory, and it is the expensive one

A namespace-wide default-deny **silently breaks every health probe**, because
probe traffic comes from the kubelet on the node, whose source is the node IP —
which no pod selector matches. The symptom is deeply misleading: every pod goes
`0/1 Ready` with `Readiness probe failed: connect: connection refused`, while the
container is listening perfectly well. An operator will spend an hour looking for
a Spring Boot misconfiguration that does not exist.

**What it costs, plainly:** these services probe with `tcpSocket` on the *same*
port they serve, so there is no way to admit the kubelet on 8081-8089 without
also admitting anything else that can reach the node on those ports. The 9
services' NodePorts therefore stay network-reachable.

**What stays closed, and this is the win:** `5432` and `1025`/`8025` are *not* in
that rule's port list and need no entry, because the database probes are `exec`
probes (`pg_isready`) which run inside the container and are not network traffic.
So under this overlay:

* **the 9 database host ports (5500-5508) are closed** — psql/DBeaver from the
  host no longer works, and that is correct;
* **the mailpit web UI (8026) is closed** — a web UI for a mail sink has no
  business being network-reachable in production.

`k8s/scripts/debug.sh` reports those 11 ports as unreachable on this overlay.
**That is the security posture working, not a fault.**

To close 8081-8089 as well, delete the nine `<svc>-nodeport` Services — they are
in `k8s/base/services.yaml`, out of scope for this overlay — or firewall the
node. **This is the single most important unresolved item in this directory.**

The CIDRs in the rule (`10.244.0.0/16` pod, `10.96.0.0/12` service) are this
cluster's. Find yours:

```bash
kubectl get nodes -o jsonpath='{.items[*].spec.podCIDR}'
kubectl get svc kubernetes -n default -o jsonpath='{.spec.clusterIP}'
```

### `allow-dns-egress` is inert today, on purpose

Only ingress is default-denied, so egress is unrestricted and the DNS rule
changes nothing. It is there so that adding `- policyTypes: [Egress]` to
`default-deny-ingress` later is a one-line change rather than an outage — without
it, enabling egress default-deny black-holes DNS and every hostname lookup in the
platform fails at once, including `discovery-server` and `postgres-user`, which
are reached **by name**.

---

## 12. How this overlay was validated

```bash
# renders clean
kubectl kustomize k8s/overlays/prod > /dev/null          # exit 0, 122 objects

# client-side dry-run (no schema validation)
kubectl apply --dry-run=client -k k8s/overlays/prod -o name   # exit 0, 122 names

# SERVER-side dry-run: full API schema + admission. Non-mutating.
#   against the live namespace: 101/122, the 21 failures are the
#   immutable-selector collision of section 10(b) and nothing else
#   with the workloads renamed to collision-free names:
#   122/122, exit 0, empty stderr
```

**The server-side dry-run is the one that matters.** The client-side dry-run
validates no schemas at all and would happily accept the misplaced-probe bug
described in section 4. `kubectl kustomize` validates nothing either.

---

## 13. What must change before a real production cluster

None of this is optional, and none of it is in this overlay.

### Blocking

1. **Databases.** Managed Postgres, or CloudNativePG/Patroni, or at minimum a
   pooler in front of a real replica set. Nine `replicas: 1` StatefulSets on
   node-local volumes is a development convenience. (Section 3.)
2. **A real StorageClass.** `standard` here is `rancher.io/local-path`:
   `reclaimPolicy: Delete`, `volumeBindingMode: WaitForFirstConsumer`, and
   **`allowVolumeExpansion: false`**. Node-local, deleted with the node, and
   cannot be resized — which is why the volumes had to be sized correctly at
   creation. Needs a replicated block store (EBS/Ceph/Longhorn).
3. **Secrets.** `k8s/base/secrets.yaml` is committed plain text with
   `stringData`, and the values are the same throwaway dev credentials from
   `docker-compose.yml`. Replace with External Secrets / SOPS / SealedSecrets.
   `GPL_JWT_SECRET` in particular is the key that signs every token, and rotating
   it invalidates every session.
4. **TLS.** Nothing is encrypted in transit. NodePort 8080 is plain HTTP.
5. **discovery-server HA, or 1 replica.** Section 10(a). As configured, 2
   replicas is a split-brain registry.
6. **A CNI that enforces NetworkPolicy**, and the `allow-kubelet-probes` rule
   narrowed. Section 11.
7. **metrics-server**, or the ten HPAs are decorative. Section 8.
8. **The replicas/HPA conflict resolved.** Section 3.

### Strongly recommended

9. **Backup and restore, and a restore test.** Nine databases with
   `reclaimPolicy: Delete` and no backups is data loss waiting for a bad day.
   `pg_dump` per database, off-cluster, encrypted, with retention — and a
   rehearsed restore. An untested backup is not a backup.
10. **Pod Security.** `k8s/base/namespace.yaml` sets
    `pod-security.kubernetes.io/enforce: privileged` because the temurin and
    mailpit images run as root. Move to `restricted`: add `runAsNonRoot`,
    `readOnlyRootFilesystem`, `allowPrivilegeEscalation: false`,
    `drop: [ALL]`, and non-root `securityContext` on every container.
11. **Monitoring and alerting.** Prometheus + `kube-prometheus-stack`, plus
    alerts on: HPA at max, PDB blocking, pod restart rate, Postgres connection
    count and disk, Eureka registration count, and gateway 5xx rate. Right now
    the only health signal on this platform is a human running `debug.sh`.
12. **Log aggregation.** The logs are on stdout and vanish with the pod. Loki or
    Elasticsearch, with retention.
13. **Resource limits reviewed against real usage.** The numbers here are derived
    from the dev numbers, not from a load test. Run a load test, then set
    requests from the observed p99 and limits above it.
14. **The `X-User-Permissions` header.** `SERVER_MAX_HTTP_REQUEST_HEADER_SIZE`
    is 64 KB because the gateway forwards 187 permission codes (~5 KB) per
    request. That is a wide, unauthenticated-in-itself header on every request
    to every service. On a real network, a JWT with permissions *in the token*
    is better than a header the gateway asserts.
15. **A staging overlay.** Section 1.
16. **Namespace separation.** Dev and prod must not share `csph-gpl`, which also
    resolves the immutable-selector problem (section 10(b)).

### Nice to have

17. Multi-AZ node pools, so `topologySpreadConstraints` over
    `kubernetes.io/hostname` actually spreads across zones and not just across
    boxes in one rack.
18. Read-only root filesystems and a `distroless` base image for the JVM
    services, which also removes the `privileged` Pod Security requirement.
19. `Service` type `LoadBalancer` for the gateway, replacing NodePort 8080.
