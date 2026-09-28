# CSPH Postman Suite

Postman collections for the CSPH / GPL-RFID microservices backend, plus a local environment file.

```
postman/
  csph-gateway.postman_collection.json      146 requests   everything through the gateway on :8080
  csph-services-direct.postman_collection.json  82 requests  each service on its own port (:8081-:8089)
  csph-local.postman_environment.json         shared variables for localhost
  README.md                                   this file
```

---

## 1. Import order

**Import the environment first.** Both collections reference `{{gateway}}`, `{{siteId}}`, `{{orgId}}` and dozens of
other variables; none of them resolve until `csph-local` is loaded and selected.

| step | what | how |
| --- | --- | --- |
| 1 | `csph-local.postman_environment.json` | Postman → **Environments** → Import → drop the file |
| 2 | `csph-gateway.postman_collection.json` | **Collections** → Import |
| 3 | `csph-services-direct.postman_collection.json` | **Collections** → Import (optional - see below) |
| 4 | select `csph-local` | the environment selector, top right |

All three files are collection **v2.1.0** / standard environment format and import without conversion prompts.
There are no collection-level `{{var}}` definitions, so the environment is the single source of truth and both
collections share it.

CLI alternative:

```bash
newman run csph-gateway.postman_collection.json \
  -e csph-local.postman_environment.json
```

Newman runs folders in order, which is what this suite needs - see §3.

---

## 2. Which collection, and when

| | `csph-gateway` | `csph-services-direct` |
| --- | --- | --- |
| base URL | `{{gateway}}` = `:8080` | the service's own port |
| authentication | JWT in `Authorization`, security headers injected by the gateway | none |
| `@RequiresPermission` | **enforced** | **skipped** (no `X-User-PersonId` => no security context) |
| `X-User-PersonId` semantics | real caller identity | required `@RequestHeader` on some writes; otherwise the bypass trigger |
| `createdBy` / `changeby` audit fields | populated from the JWT | `null` or `"SYSTEM"` |
| size | 146 requests, full CRUD | 82 requests, read + create + one domain action per service |
| use it to | verify what a real client sees; smoke-test a deployment | explore the endpoint surface; generate fixtures; debug a route without logging in |

**Start with `csph-gateway`.** It is the realistic path and the one that catches real regressions.
`csph-services-direct` is a diagnostic view - see §7 for why the permission model makes it useful, and for the
security warning that comes with it.

### Before anything else

The stack must be up: `docker compose up -d`, then wait for all nine services to register in Eureka
(`http://localhost:8761`). Open `00 - Health & Discovery` and run the whole folder - it tells you in one pass
which of the nine services (and the gateway) are reachable. A `503 Service Unavailable` everywhere else is nearly
always "not registered in Eureka yet", not a broken endpoint.

---

## 3. Run order is load-bearing

### `csph-gateway`

```
00 - Health & Discovery     no auth needed (the 2nd actuator request needs a token)
01 - Auth                   <-- LOGIN HERE
01b - Auth negative cases   must 401
02 - Organization Service   -> orgId, marketerOrgId, transporterOrgId, clientOrgId, siteId, siteIdDest
03 - User Service           -> personIdNew, personIdLogin, roleId, groupId, permissionId
04 - Tour Service           -> tourId, checkpointId, pickupId, contractId
05 - Cylinder Service       -> cylinderId, rfidTagId, tagUid, scanEventId
06 - Fleet & IoT Service    -> vehicleId, deviceId
07 - Subsidy Service        -> declarationId, reconciliationId, redressementId
08 - Audit Service
09 - Notification Service   -> templateCode
```

Folders **02 -> 09 chain through collection variables**. Each folder's "read" request is what captures the ids the
later folders consume, so running folder `05` on its own will not work: `POST /scans` needs the `checkpointId`
created in folder `04`, because `scan_events.checkpoint_id` is `NOT NULL`.

Run **Collections ▸ csph-gateway ▸ Run collection**, not individual folders.

### `csph-services-direct`

Same folder numbering. One hard dependency: folder `05`'s `POST /scans` needs folder `04`'s `checkpointId`.
Folder `03`'s role-assignment request needs a `roleId`, which nothing in that collection creates - seed it once from
`GET :8083/api/v1/roles/` or run the gateway collection first. The test tolerates the 400 and says so in the console.

---

## 4. Variable map

### Base URLs - all `type: default`, all with working defaults

| variable | default | what |
| --- | --- | --- |
| `gateway` | `http://localhost:8080` | api-gateway (Spring Cloud Gateway, reactive) |
| `authService` | `http://localhost:8081` | auth-service - login, refresh, `/me`, logout, change-password |
| `orgService` | `http://localhost:8082` | organization-service - organizations, sites, client-sites, classifications, relationships |
| `userService` | `http://localhost:8083` | user-service - persons/users, roles, permissions, groups |
| `auditService` | `http://localhost:8084` | audit-service - modifications, status history, summaries, ingest |
| `notifService` | `http://localhost:8085` | notification-service - templates, send |
| `tourService` | `http://localhost:8086` | tour-service - tours, checkpoints, pickups, transporter contracts |
| `cylinderService` | `http://localhost:8087` | cylinder-service - cylinders, RFID tags, scan events |
| `fleetService` | `http://localhost:8088` | fleet-device-service - vehicles, IoT devices, telemetry |
| `subsidyService` | `http://localhost:8089` | subsidy-service - declarations, reconciliations, redressements |
| `eureka` | `http://localhost:8761` | discovery-server registry |
| `mailhogWeb` | `http://localhost:8026` | MailHog UI - captured outbound mail |

### Credentials

| variable | default | note |
| --- | --- | --- |
| `username` | `superadmin.cspHq` | also the login identifier; `AuthService` falls back to email lookup |
| `password` | `Password123!` | every seeded user shares it |
| `personId` | `superadmin.cspHq` | **business key**, not a UUID. `AuthDataInitializer` seeds `personId = username` |

`accessToken` and `refreshToken` ship **empty** and are populated by `01 - Auth ▸ POST /api/v1/auth/login`. The
environment file also carries the `_postman_variable_scope: "environment"` marker and the two `_postman_exported_*`
metadata fields, as exported by Postman.

### Captured ids - all empty, all filled at run time

Set as **collection** variables by the test scripts (visible in Postman's console output, which prints each one as
it is captured).

| variable | captured by | what it holds |
| --- | --- | --- |
| `orgId` | `02 ▸ GET /organizations`, then re-set by `POST /organizations` | organization **UUID** |
| `marketerOrgId` | `02 ▸ GET /organizations` | org whose `type` matches `MKT` / `MARKETEUR` |
| `transporterOrgId` | `02 ▸ GET /organizations` | org whose `type` matches `TRP` / `TRANSPORTEUR` |
| `clientOrgId` | `02 ▸ GET /organizations` | org whose `type` matches `CLT` / `CLIENT` |
| `siteId` | `02 ▸ GET /sites`, re-set by `POST /sites` | site **UUID** |
| `siteIdDest` | `02 ▸ GET /sites` | second site; falls back to `siteId` |
| `clientSiteId` | `02 ▸ GET /client-sites`, `POST /client-sites`, `by-site` | client-site row UUID |
| `personIdNew` | `03 ▸ GET /persons/`, re-set by `POST /persons/` | person **UUID** (for `{id}` paths) |
| `personIdLogin` | `03 ▸ GET /persons/`, re-set by `POST /persons/` | person **business key** |
| `roleId` | `03 ▸ GET /roles/` bootstrap, re-set by `POST /roles/` | role UUID |
| `permissionId` | `03 ▸ GET /permissions/` | permission UUID, for the grant query param |
| `groupId` | `03 ▸ POST /groups/` | group UUID |
| `tourId` | `04 ▸ POST /tours` | tour UUID |
| `checkpointId` | `04 ▸ POST /tours/{id}/checkpoints` | checkpoint UUID |
| `pickupId` | `04 ▸ POST /pickups` | pickup UUID |
| `contractId` | `04 ▸ POST /contracts` | transporter-contract UUID |
| `vehicleId` | `04 ▸ GET /vehicles` bootstrap, re-set by `06 ▸ POST /vehicles` | vehicle UUID |
| `deviceId` | `06 ▸ POST /devices` | IoT device UUID |
| `cylinderId` | `05 ▸ POST /cylinders` | bottle UUID |
| `rfidTagId` | `05 ▸ POST /rfid` | RFID tag **UUID** |
| `tagUid` | `05 ▸ POST /rfid` | RFID tag **EPC** (a different column from the UUID) |
| `scanEventId` | `05 ▸ POST /scans` | scan-event UUID |
| `declarationId` | `07 ▸ POST /declarations` | declaration UUID |
| `reconciliationId` | `07 ▸ POST /reconciliations` | reconciliation UUID |
| `redressementId` | `07 ▸ POST /redressements` | redressement UUID |
| `templateCode` | `09 ▸ POST /notification-templates` | template **code**, not a UUID |

### Derived per request - regenerated by the collection-level pre-request script

| variable | value | why |
| --- | --- | --- |
| `uniq` | `PM` + `Date.now()` | suffix for every unique-constrained field, so re-runs never 409 |
| `nowIso` | current timestamp, ISO-8601 | scan events, audit events |
| `periodStart` | now − 30 days | declaration windows, telemetry range |
| `periodEnd` | now | declaration windows, expected arrival |
| `dueDate` | now + 30 days | redressement due date, certificate expiry |
| `authHeader` | `Bearer {{accessToken}}` | pre-built for reuse; every request writes the header out explicitly instead |

`uniq` is regenerated **per request**, so a `PUT` body that references `{{uniq}}` sends a different value than the
`POST` that created the record. That is intentional and harmless for partial updates - but if you ever need one value
stable across a chain, set it in a pre-request script rather than relying on `{{uniq}}`.

### Seeded reference values - informational, not used in requests

Present so you can read the seeded data without opening the database. Both seeder paths write the same entities with
different *codes*, which is why the org matching in folder `02` accepts both spellings.

| variable | value |
| --- | --- |
| `orgCodeCspHq` | `CSPH` |
| `orgCodeMktGpl` | `MKT-GPL` |
| `orgCodeTrpAbc` | `TRP-ABC` |
| `siteCodeCspHq` | `SITE-CSPH-HQ` |
| `siteCodeDepDla` | `SITE-DEP-DLA-PRINCIPAL` |
| `siteCodeCltInd` | `SITE-CLT-IND-RECEPTION` |

Other seeded org codes: `DEP-DLA`, `DEP-YDE`. Other seeded site codes: `SITE-DEP-DLA-FIL`,
`SITE-DEP-YDE-PRINCIPAL`, `SITE-MKT-GPL-ENTREPOT`, `SITE-TRP-ABC-DEPOT`.

---

## 5. Seeded logins

`AuthDataInitializer` creates **13** users on an empty database. All share the password **`Password123!`**, and for
every one of them `personId == username`.

| username | email | org | role in the story |
| --- | --- | --- | --- |
| `superadmin.cspHq` | `emmanuel.mbarga@cspHq.cm` | CSPH | **SUPERADMIN** - the default; sees every module |
| `admin.cspHq` | `admin.cspHq@cspHq.cm` | CSPH | ADMIN - staff / HR |
| `superviseur.cspHq` | `robert.atangana@cspHq.cm` | CSPH | SUPERVISOR - monitoring, risk scores |
| `integrateur.cspHq` | `fabrice.ndjock@cspHq.cm` | CSPH | INTEGRATEUR - PDA / GPS / RFID provisioning |
| `operateur.dla` | `jean.kouam@depot-dla.cm` | DEP-DLA | depot operator, Douala |
| `emplisseur.dla` | `marie.ngono@depot-dla.cm` | DEP-DLA | filling-centre operator |
| `operateur.yde` | `paul.mvondo@depot-yde.cm` | DEP-YDE | depot operator, Yaounde |
| `gest.gpl` | `alice.fouda@gpl.cm` | MKT-GPL | GPL stock manager (marketer) |
| `agent.gpl` | `diane.ekotto@gpl.cm` | MKT-GPL | field distribution agent |
| `chauffeur.abc1` | `pierre.essomba@abctransport.cm` | TRP-ABC | driver - **no web UI**, PDA only |
| `chauffeur.abc2` | `samuel.ondoa@abctransport.cm` | TRP-ABC | driver |
| `resp.abc` | `jacques.tabi@abctransport.cm` | TRP-ABC | transport operations manager |
| `resp.industries` | `christelle.moukoko@industries.cm` | CLT-IND | industrial client, procurement |

The 8 system roles (a **closed Postgres enum** on the seeded `system_roles` table) are: `SUPERADMIN`, `ADMIN`,
`SUPERVISOR`, `INTEGRATEUR`, `AGENT`, `MARKETEUR`, `TRANSPORTEUR`, `LIVREUR`. `POST /api/v1/roles/` writes to a
**different** `roles` table, where `code` is a free unique string - which is why the suite creates `ROLE-PM...` rather
than reusing one of those eight names.

Also seeded: 4 organizations, 7 sites, 13 persons (one per user, `personId == username`), and permission rows
matching the `@RequiresPermission` codes.

---

## 6. The five gotchas that will waste your time otherwise

### 6.1 `ipAddress` and `deviceInfo` are mandatory in the login body

Only `username` and `password` are `@NotBlank`. The other two are not annotated - and they still have to be sent.

`AuthService.login` writes `request.getIpAddress()` straight into `auth_users.last_login_ip`, and both fields into
the `refresh_tokens` row. Omit `ipAddress` and the **first** login succeeds while storing a short or NULL value; the
**second** login then fails on a column-length overflow. It presents as "it worked once and now it never works",
which sends people looking at the wrong service entirely.

The suites always send `"deviceInfo": "Postman"` and `"ipAddress": "127.0.0.1"`.

### 6.2 Bare vs enveloped responses - the biggest source of "should-have-passed" failures

Most endpoints answer `{"success":bool,"message":str,"data":...}`. These answer a **bare `PageResponse`** with no
`success` key at all:

```
/api/v1/organizations      /api/v1/sites        /api/v1/persons/     /api/v1/users/
/api/v1/cylinders          /api/v1/rfid         /api/v1/scans
```

And the telemetry controller breaks the pattern twice more:

| endpoint | shape |
| --- | --- |
| `POST /api/v1/telemetry` | **201 with a completely empty body** - do not call `.json()` on it |
| `GET /api/v1/telemetry/vehicles/{id}/latest` | **bare object** - no `success`, no `data` wrapper |
| `GET /api/v1/telemetry/vehicles/{id}` | **bare array** - not enveloped, not paged |

**Whether a response is enveloped is decided per method signature, not per service.** organization-service alone
does both, inside one controller. The tests in these collections are written per endpoint for exactly that reason,
and they assert defensively where the shape is genuinely ambiguous.

`PageResponse` serialises **`member`** as the primary field, plus the `content` and `totalElements` getter aliases -
so the JSON carries all three. Every test here accepts either:

```js
const items = d.content || d.member || [];
```

Keys: `member` / `content`, `totalCount` / `totalElements`, `pageNumber`, `pageSize`, `totalPages`, `hasNext`,
`hasPrevious`.

### 6.3 Trailing slashes on five list endpoints

`GET /persons/`, `/users/`, `/roles/`, `/groups/`, `/permissions/`

Those controllers are mapped `@GetMapping("/")`. Without the slash you get a **404, not a redirect** - Spring MVC
does not redirect on trailing-slash mismatch by default. Note the asymmetry: `/organizations` and `/sites` are mapped
on the bare path (`@GetMapping` with no value) and take **no** trailing slash.

### 6.4 Gateway 401 bodies are plain text

`JwtAuthenticationFilter.onError` writes the reason as raw bytes:

* `Missing Authorization Header` - no `Authorization` header at all
* `Invalid Authorization Header` - not a `Bearer ` prefix
* `Invalid JWT Token` - well-formed prefix, unverifiable token

So a gateway 401 body **is not JSON**. Do not assert `j.success === false` on one; assert the status and read the
text. By contrast, a **403 from a service is enveloped JSON** (`GlobalExceptionHandler` maps
`AccessDeniedException` properly) - so 401 and 403 look different on the wire, which is worth knowing before you
write a client.

`Authorization: ` with an **empty** token also yields 401 `Invalid JWT Token`, which is why folder `01b` passes even
on a cold collection that has never logged in.

### 6.5 Required parameters that are not in the body

| endpoint | required | where |
| --- | --- | --- |
| `POST /api/v1/tours/{id}/assign-vehicle` | `vehicleId` | **query string**, no body |
| `POST /api/v1/checkpoints/{id}/skip` | `reason` | **query string**, no body |
| `PATCH /api/v1/pickups/{id}/approve` | `approvedQuantity` | **query string**, primitive `double` |
| `GET /api/v1/sites/nearby` | `lat`, `lon`, `radius` | **query string**, all primitive `double` |
| `POST /api/v1/roles/{id}/permissions` | `permissionId` | **query string**, **no body at all** |
| `POST /api/v1/cylinders/{id}/transfer` | `targetSiteId` | **request body** (`@NotBlank`) |
| `POST /api/v1/audit/ingest` | - | **raw** `AuditEvent`, **not** wrapped in an `ApiResponse` envelope |

Putting `permissionId` or `vehicleId` in a JSON body is the single most common mistake on this API: it returns a
400 whose message says nothing about which parameter was expected. Those tests log the response body so the cause is
visible in the Postman console.

---

## 7. `csph-services-direct`: why it exists, and the warning that comes with it

`PermissionAspect` (common-lib) begins every check with:

```java
if (!GplSecurityContext.isAuthenticated()) {
    return joinPoint.proceed();   // "internal call, pass-through"
}
```

`GplSecurityContextFilter` only populates the thread-local when **`X-User-PersonId`** is present and non-empty. No
header means no context means the `@RequiresPermission` check is skipped. That is exactly what an in-cluster
`http://csph/gpl-<service>/...` call does, and it is why this collection can reach the whole endpoint surface without
a login.

Folder `10 - Permission AOP contrast` proves it with three requests against one endpoint:

| | `X-User-PersonId` | `X-User-Permissions` | result |
| --- | --- | --- | --- |
| **A** | absent | absent | **200** - no context, check bypassed |
| **B** | present | absent | **403** - context exists, permission missing |
| **C** | present | `ORG_VIEW` | **200** - permission found |

**The corollary is a real security property, not a test artefact:** because the headers are *trusted*, anyone who can
reach a service port directly can simply assert whatever permission they like (request C). The headers are only
trustworthy because in production the gateway overwrites them after validating the JWT. So the service ports must
**never** be exposed outside the cluster - the direct collection is safe only because it targets `localhost` in
development.

### Header rules in this collection

| case | headers sent | why |
| --- | --- | --- |
| reads | **none** | the bypass under demonstration |
| writes declaring `@RequestHeader("X-User-PersonId")` with no `required = false` | `X-User-PersonId` | a missing header is a **400** from Spring MVC, not a 403 - organization-service writes, `POST /roles/`, `POST /groups/`, group members, `POST /auth/logout` |
| `GET /api/v1/me`, `/me/permissions` | `X-User-PersonId` | `MeController.requireCurrentUser` throws `UnauthorizedException` (401) when it is blank - a controller check, before any aspect |
| everything else | none | `required = false`; the audit field falls back to `null` or `"SYSTEM"` |

`Authorization` is never sent. auth-service is the one service with a Spring Security chain, and it permits
`/api/v1/auth/**` and `/api/v1/me/**` - so folder `01` works with no token. Every other service has no security
chain at all.

### What you can see here that you cannot see through the gateway

* **The audit fields are empty.** `createdBy` / `changeby` / `createdBy` come from JWT headers the gateway injects.
  Direct, they are `null` or `"SYSTEM"` - which is a fast way to find a field that silently depends on the gateway.
* **`/api/v1/me` and `/api/v1/me/permissions` return `[]`.** They do not read the database; they split the
  `X-User-Roles` / `X-User-Permissions` headers. Direct, nothing populates those headers, so even SUPERADMIN sees
  nothing. The same two requests through the gateway return the real arrays.
* **Service-specific conventions surface.** organization-service and user-service both use `required = false`;
  subsidy-service reads `X-User-Username` rather than `X-User-PersonId`; fleet-device-service uses dotted permission
  codes (`fleet.vehicles.read`) where the others use UPPER_SNAKE.

---

## 8. Assertions that are deliberately loose, and why

A test that asserts 200 where the API can legitimately answer 409 is a test that fails on the second run. Four
places in `csph-gateway` accept a state-machine rejection, and they say so in the request description:

* `04 ▸ PATCH /pickups/{id}/reject` after `approve` - correct: a validated pickup cannot be rejected
* `07 ▸ POST /declarations/{id}/reject` after `approve` - same
* `07 ▸ POST /redressements/{id}/cancel` after `pay` - same
* `04 ▸ POST /checkpoints/{id}/skip` after `validate`

Those accept `200 | 409 | 422` and log the body. **A pass there is the positive result**: a resource that accepted
both `approve` and `reject` would mean the lifecycle is not enforced, which is far more serious than a red test.

Other deliberate tolerances:

| request | accepts | reason |
| --- | --- | --- |
| `00 ▸ actuator health (unauthenticated)` | `200 \| 401 \| 404` | `/actuator` is **not** in the gateway's `OPEN_ENDPOINTS`, so a cold run gets a plain-text 401. The second actuator request, after login, asserts a hard 200. |
| `00 ▸ eureka /eureka/apps` | asserts `AUTH-SERVICE` present | an empty registry explains every 503 elsewhere; the console prints all registered names |
| `02 ▸ POST /client-sites` | `200 \| 201 \| 409` | `client_sites.site_id` is `NOT NULL UNIQUE` - a site can be a client site **at most once, ever**, and no per-run trick can make it unique. 409 is correct, not a failure |
| `03 ▸ POST /roles/{id}/permissions` | `200 \| 204 \| 400 \| 409` | `{{permissionId}}` is empty until `GET /permissions/` has been sent once |
| `05 ▸ GET /rfid/tag/{tagUid}` | `200 \| 404` | `{{tagUid}}` is empty on a cold run |
| `07 ▸ GET /reconciliations/declaration/{id}` | `200 \| 404` | one reconciliation per declaration; 404 until it is created |
| `09 ▸ POST /notifications/send` | status `SENT` or `FAILED` | SMTP failures are caught and recorded, and the endpoint answers 200 either way - so check `j.data.status`, not the HTTP code |
| `00 - README` (direct) | `200 \| 401 \| 404` | reachability probe, not a correctness test. `Connection refused` means the container is down |

---

## 9. Re-runnability

Anything with a unique constraint is suffixed with `{{uniq}}`, regenerated per request by the collection-level
pre-request script:

| entity | unique constraint | field used |
| --- | --- | --- |
| organization | `code` unique | `TST-{{uniq}}` |
| site | `code` unique **and** `(organizationId, name)` unique | `{{uniq}}` in **both** `code` and `name` |
| person | `email` unique | `person-{{uniq}}@cspHq.cm` |
| role | `code` unique | `ROLE-{{uniq}}` |
| group | `code` unique | `GRP-{{uniq}}` |
| cylinder | `serialNumber` | `CYL-{{uniq}}` |
| RFID tag | `tagUid`; `bottleSerial` has a **`CHECK (bottle_serial ~ '^[A-Z0-9]{8,}$')`** | `BT{{uniq}}` - uppercase alphanumerics only, no dashes |
| vehicle | `license_plate` **`VARCHAR(20) UNIQUE`** | `LT-{{uniq}}` = 18 chars, deliberately under the limit |
| device | `serial_number VARCHAR(100) UNIQUE` | `DEV-{{uniq}}` |
| tour | `tourCode` | `TOUR-{{uniq}}` |
| checkpoint | `UNIQUE (tournee_id, sequence)` | safe because the tour is created fresh each run |
| template | `code` | `PM-TPL-{{uniq}}` |

Two things are **soft** deletes, so re-runs never collide but rows do accumulate: `DELETE /organizations/{id}`
sets `status = ARCHIVED` rather than removing the row, and the same pattern applies to tours, cylinders and vehicles.
`POST /organizations-relationships` has no unique constraint at all - it is an append-only log, so each run adds a
row (by design).

---

## 10. Known side effects of a full run

A complete `csph-gateway` run is **not** side-effect-free. Being explicit about it:

* **`PATCH /api/v1/me` overwrites `auth_users.email`** for the logged-in user with `postman-<uniq>@cspHq.cm`. The
  seeded `superadmin.cspHq` address stays changed to whatever ran last. Logins are by username, so nothing breaks.
* **`POST /api/v1/auth/logout` deletes every refresh token** for the person, so `POST /api/v1/auth/refresh` fails
  afterwards. Re-run `01 - Auth` from the top.
* `csph-services-direct ▸ 01 - Auth ▸ POST /api/v1/auth/logout` does the same thing to the **shared** database, so
  it invalidates the `refreshToken` the gateway collection captured.
* Archived `TST-PM...` / `DIR-TST-PM...` organizations, `ROLE-PM...` roles, `GRP-PM...` groups, `PM-TPL-...` templates
  and soft-deleted tours / cylinders / vehicles accumulate.
* `02 ▸ PATCH /organizations/{id}/status` and `05 ▸ PATCH /sites/{id}/status` leave the freshly created records
  `INACTIVE` / `SUSPENDED` before deleting them - harmless, but it is why the delete is last in each folder.

---

## 11. Troubleshooting

| symptom | cause |
| --- | --- |
| `503 Service Unavailable` on every gateway route | the service is not registered in Eureka yet - check `00 ▸ eureka registry` |
| `401 Missing Authorization Header` from the gateway | folder `01` has not run, or you ran a single request instead of the collection |
| `401 Invalid JWT Token` | `{{accessToken}}` is empty, or the token expired (`expiresIn` is printed at login) |
| `403` from a service | the JWT `permissions` claim lacks that code - check `01 ▸ GET /api/v1/me/permissions`, which shows what the gateway actually injected |
| `400` with no useful message | a required parameter is in the wrong place - see §6.5 |
| `400` on `POST /scans` | `{{checkpointId}}` is empty; run folder `04` first |
| `404` on `/persons/roles/...` | missing trailing slash, or a business key where a UUID is expected (and vice versa) |
| `Cannot read properties of undefined` on `/telemetry` | you asserted on a JSON body the endpoint does not send - see §6.2 |
| `409` on `POST /client-sites` | the site already has a client-site row (`UNIQUE site_id`) - correct, not a failure |
| second login fails with a database error | `ipAddress` was omitted from the first one - see §6.1 |
| `column ... character varying(20) too long` on a vehicle | `license_plate` is 20 characters - keep the plate short |
| a notification arrives with a literal `{{reference}}` | the placeholder was never in the `variables` map; unresolved placeholders are not an error |
| a template stores no placeholders at all | `{{...}}` in a request **body** is substituted by Postman - write `\{\{...\}\}` for a literal |

---

## 12. Layout reference

`csph-gateway` - 146 requests

| folder | requests | captures |
| --- | --- | --- |
| *(root) README - read me first* | 1 | - |
| `00 - Health & Discovery` | 12 | - |
| `01 - Auth` | 6 | `accessToken`, `refreshToken`, `personId`, `username` |
| `01b - Auth negative cases` | 3 | - |
| `02 - Organization Service` | 18 | `orgId`, `marketerOrgId`, `transporterOrgId`, `clientOrgId`, `siteId`, `siteIdDest`, `clientSiteId` |
| `03 - User Service` | 27 | `personIdNew`, `personIdLogin`, `roleId`, `permissionId`, `groupId` |
| `04 - Tour Service` | 24 | `vehicleId`, `tourId`, `checkpointId`, `pickupId`, `contractId` |
| `05 - Cylinder Service` | 15 | `cylinderId`, `rfidTagId`, `tagUid`, `scanEventId` |
| `06 - Fleet & IoT Service` | 14 | `vehicleId`, `deviceId` |
| `07 - Subsidy Service` | 18 | `declarationId`, `reconciliationId`, `redressementId` |
| `08 - Audit Service` | 4 | - |
| `09 - Notification Service` | 4 | `templateCode` |

`csph-services-direct` - 82 requests

| folder | requests | covers |
| --- | --- | --- |
| *(root) README - direct-port view of the same API* | 1 | - |
| `00 - README` | 10 | reachability probe on all ten ports (9 services + gateway) |
| `01 - Auth (direct :8081)` | 4 | login, `/me`, `/me/permissions`, logout |
| `02 - Organization (direct :8082)` | 8 | list, get, create, status, children, sites list, create site, classifications |
| `03 - User (direct :8083)` | 11 | list, by-person-id, create, update, status, roles, assign role, effective permissions, roles, role permissions, groups |
| `04 - Tour (direct :8086)` | 11 | vehicles bootstrap, tours, create, get, assign-vehicle, start, checkpoint, validate, pickups, create, approve |
| `05 - Cylinder (direct :8087)` | 8 | list, create, get, transfer, rfid list, create, get, scans create |
| `06 - Fleet (direct :8088)` | 9 | vehicles, create, get, devices, create, get, assign, telemetry post, latest |
| `07 - Subsidy (direct :8089)` | 9 | declarations, create, get, submit, review, reconciliations, create, redressements, create |
| `08 - Audit (direct :8084)` | 4 | modifications, status-history, summary, ingest |
| `09 - Notification (direct :8085)` | 4 | template create, list, get, send |
| `10 - Permission AOP contrast` | 3 | 200 (bypass) / 403 / 200 (granted) |

Every request in both collections carries at least a status assertion, a markdown description explaining the port and
the endpoint's specific contract, and a test script. The direct collection additionally documents on each request
whether it omits the header to demonstrate the bypass or sends it because the method declares it required.
