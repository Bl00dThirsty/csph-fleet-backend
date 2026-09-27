# PM2 supervision for the CSPH GPL Fleet frontend

The 11 Spring Boot services and 9 databases are Kubernetes pods, and the
scheduler restarts them. The Vite SPA is **not containerised** — it is a
directory of static files plus two small Node processes — and on a Linux server
it is the one long-running user-space process that needs supervising. That is
what PM2 is for.

| File | What it is |
|---|---|
| `ecosystem.config.cjs` | the two process definitions (CommonJS, so plain `node` can load it) |
| `server.js` | static file server for `apps/web/dist` + SPA fallback. Zero dependencies. |
| `proxy.js` | `/api/*` reverse proxy to the gateway. Zero dependencies. |
| `install.sh` | check deps, start, `pm2 save`, `pm2 startup` (systemd), verify |
| `uninstall.sh` | stop, deregister, remove the systemd unit, optionally the logs |

Both Node files use only the standard library — `node:http`, `node:fs`,
`node:path`, `node:url`. No express, no http-proxy, no dotenv. A dependency
here is a dependency to audit and patch forever, in exchange for something the
runtime already does.

---

## Why two processes, and why two ports

```
   browser
      │  http://<server>:3000/
      ▼
 ┌──────────────────────┐
 │ csph-frontend  :3000 │  serves apps/web/dist  (static files)
 └──────────────────────┘
      │  browser also calls :3001/api/*  (same host, same origin)
      ▼
 ┌────────────────────────────────┐
 │ csph-frontend-api-proxy  :3001 │  /api/* -> http://localhost:8080
 └────────────────────────────────┘
                                  │  the kind NodePort, from k8s/kind-cluster.yaml
                                  ▼
                            api-gateway :8080
```

**Separate processes** so a proxy fault takes down only the API surface. The
already-loaded SPA keeps serving its shell, the browser shows a working page
with failing data calls — diagnosable — instead of a connection-refused that
looks like the whole site is down.

**Separate ports** so `/api/*` can be routed by port. Putting both on one port
means writing a router, which is a third process to maintain.

### Want one port instead?

The usual approach is nginx in front of both, which is a better answer for a
real deployment (it also terminates TLS and can serve `/api` and `/` from one
origin without two ports). If you go that way, PM2 supervises the two Node
processes on 3000/3001 and nginx proxies 80 → them. Do **not** stop PM2
supervising them: nginx is not a process manager, and a crashed Node process
should restart itself rather than wait for someone to notice.

---

## ⚠ `VITE_API_BASE_URL` is a BUILD-TIME variable. This is the trap.

**`VITE_API_BASE_URL` is inlined into the JavaScript bundle at `npm run build`
time. It is not read at runtime. Changing the environment variable on the server,
restarting PM2, or reloading the browser changes nothing.**

This is not a Vite quirk to work around; it is how every bundler handles
`import.meta.env`, and it is the single most common way a production SPA
silently points at `localhost` and every request fails at the browser with
`ERR_CONNECTION_REFUSED` while the server logs look completely healthy.

Where it is used in this repo:

* `packages/config/src/index.ts` — `API_BASE_URL = getEnv('VITE_API_BASE_URL', '/api')`
* `packages/api-client/src/http-adapter.ts` — `resolveBaseURL()`, default
  `'http://localhost:8080/api/v1'`
* `apps/web/.env.example` — sets `VITE_API_BASE_URL=http://localhost:8080/api/v1`

The two sensible values:

| Value | Result | When |
|---|---|---|
| `/api` | relative, same-origin, proxied by `csph-frontend-api-proxy` on :3001 | **production behind this proxy** — the recommended default |
| `http://<host>:8080/api/v1` | absolute, cross-origin | only if the SPA and the gateway are on different hosts, and then the gateway's CORS config must be correct for that origin |

Note the default in `http-adapter.ts` is an **absolute localhost URL**. If you
build without setting the variable at all, you ship a bundle that hard-codes
`http://localhost:8080/api/v1` — in the browser of every user, on every machine.
That is the failure. So:

```bash
# CORRECT for this setup: same-origin, proxied, no CORS
cd ../csph-fleet-frontend
echo 'VITE_API_BASE_URL=/api' > apps/web/.env.production
npm ci && npm run build

# Verify what actually got baked in, before deploying it:
grep -ro 'http://localhost:8080[^"]*' apps/web/dist/assets/*.js | head
#   -> MUST print nothing. If it prints a line, the bundle points at localhost
#      and no amount of restarting PM2 will fix it.
grep -ro '"/api[^"]*"' apps/web/dist/assets/*.js | head
```

That `grep` is the whole point of this section. It takes two seconds and it is
the difference between finding this in CI and finding it for a customer.

Other build-time variables in `.env.example` that behave the same way:
`VITE_API_MODE`, `VITE_ARCGIS_API_KEY`, `VITE_CLERK_PUBLISHABLE_KEY`. The
ArcGIS key in particular is required to render the map, and it is baked in —
so it must be set at build time even though it is "just" an API key, and it will
be visible to anyone who opens the bundle.

---

## Install

Requirements: node 18+ (the `node:` prefix imports need it), npm, and the SPA
already built.

```bash
# 1. build the SPA, with the correct API base URL baked in
cd ../../../csph-fleet-frontend
echo 'VITE_API_BASE_URL=/api' > apps/web/.env.production
npm ci
npm run build            # -> apps/web/dist

# 2. install and supervise
cd ../csph-fleet-backend/k8s/scripts/pm2
npm install -g pm2        # or: CSPH_AUTO_INSTALL=1 ./install.sh
./install.sh
```

`install.sh` will:

1. verify node ≥ 18, npm and pm2, and refuse to continue with a clear message
   if any is missing;
2. verify `apps/web/dist/index.html` exists — starting PM2 against an empty
   `dist/` is the most common way every route 404s;
3. create `csph-fleet-frontend/.pm2-logs/`;
4. `pm2 start ecosystem.config.cjs --env production`;
5. `pm2 save`;
6. install the systemd unit (`sudo sh -c "<the command pm2 startup printed>"`);
7. print the status of both processes.

If your frontend repo is **not** a sibling of the backend repo:

```bash
CSPH_FRONTEND_ROOT=/opt/csph-fleet-frontend ./install.sh
```

The resolved path is printed at boot by both Node processes, so a wrong guess is
visible in the first second of the log rather than at the first request.

### Check it works

```bash
pm2 status
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:3000/            # 200
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:3000/transporters # 200 (SPA fallback)
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:3001/api/v1/auth/login  # 401/403 from the gateway
```

That last one is the important one. It should return the **gateway's own status
code** — 401 or 403 for an unauthenticated call, never 502. A 502 means the
proxy cannot reach the gateway, and the body names `API_TARGET`:

```json
{"success":false,"message":"The API gateway is unreachable.",
 "detail":"ECONNREFUSED (ECONNREFUSED: connect ECONNREFUSED ::1:8080) (…)",
 "hint":"Is api-gateway running, and is API_TARGET correct? (currently http://localhost:8080)"}
```

The proxy deliberately **passes the gateway's status code through unchanged**,
including 503 "Unable to find instance for auth-service" from a service that has
not registered with Eureka. Rewriting it to 502 would destroy the single most
useful diagnostic the SPA has. If you see 503s in the browser, run
`k8s/scripts/debug.sh` — not a PM2 problem.

---

## Everyday commands

```bash
pm2 status                                  # both processes, pid, uptime, restarts
pm2 logs csph-frontend                      # follow
pm2 logs csph-frontend-api-proxy --lines 100
pm2 logs --err                              # errors from all apps
pm2 monit                                   # live CPU/memory

pm2 restart csph-frontend --update-env      # after changing env
pm2 restart ecosystem.config.cjs --env production --update-env   # both
pm2 reload csph-frontend                    # zero-downtime; falls back to restart
pm2 stop csph-frontend                      # stop, keep registered
pm2 start csph-frontend
```

Logs are in `csph-fleet-frontend/.pm2-logs/`, rotated at 10 MB × 5 files per
process. The rotation is not optional: a proxy in a restart loop with no cap
fills a server disk overnight, and a full disk takes the databases with it.

---

## Surviving a reboot: `pm2 save` vs `pm2 startup`

These are two different things and confusing them is the standard PM2 mistake.

```
  pm2 save      writes the process list to ~/.pm2/dump.pm2
  pm2 startup   installs a systemd unit that runs `pm2 resurrect` on boot
  pm2 resurrect reads that dump and starts the apps
```

* **`pm2 save` alone** → nothing runs after a reboot. The dump exists; nothing
  reads it.
* **`pm2 startup` alone** → a reboot resurrects whatever was in the dump at the
  time, and any `pm2 delete` you do is silently undone at the next boot.
* **Both** → correct.

`pm2 startup` *prints* a `sudo` command; it does not run it. `install.sh`
captures and executes that line for you, via `sudo sh -c` — not `sudo eval`,
because `eval` is a shell builtin and `sudo` cannot run builtins.

After changing the process list, run `pm2 save` again. The systemd unit calls
`pm2 resurrect`, which reads the dump, **not** the ecosystem file.

Verify after a reboot:

```bash
sudo systemctl status "pm2-$(id -un)"
```

---

## The restart policy, and what each number is for

Both apps share:

```js
restart: 'always',
max_restarts: 10,
expire_restart_delay: 90,   // seconds
restart_delay: 2000,        // ms between restarts
max_memory_restart: '512M',
```

* **`restart: 'always'`** — come back after a crash, an OOM kill, or an
  operator's SIGTERM. A process that stays down is worse than one that
  restarts.
* **`max_restarts: 10` + `expire_restart_delay: 90`** — the pair is what makes
  PM2 useful rather than harmful. Ten restarts in ninety seconds is a boot
  loop: something is structurally wrong (a missing `dist/`, a port already in
  use, a bad `API_TARGET`) and restarting an eleventh time only hides it inside
  a loop that never surfaces. PM2 gives up, the process is marked `errored`, and
  it shows up in `pm2 status` — which is the point.
* **`restart_delay: 2000`** — 2s between restarts so a crash-on-start is not a
  hot loop that starves the rest of the box.
* **`max_memory_restart: 512M`** — a hard ceiling. The static server holds one
  file stream at a time, so 512M is already generous; exceeding it means a
  symlink loop or a runaway cache, and restarting is the right response rather
  than letting the OOM killer take out an unrelated process.

`wait_ready: true` + `listen_timeout: 20000` means PM2 does not consider a
process online until the app calls `process.send('ready')`, which both do
**after** `server.listen()` has actually bound. The 20s ceiling means a process
that hangs during boot is killed and restarted, rather than sitting in
"starting" forever.

**What readiness deliberately does NOT mean:** that the gateway is reachable. If
`API_TARGET` is wrong, the proxy is still a *healthy* proxy — it listens and
answers 502 with a body naming the target. Restarting it cannot fix an
unreachable gateway; it would only produce a loop of processes that cannot do
their job. Use `k8s/scripts/debug.sh` for gateway problems.

---

## The two Node programs

### `server.js` — static files

* Serves `apps/web/dist` on :3000.
* **SPA fallback.** A bookmarked `/transporters` reload asks for a path that does
  not exist on disk. Without a fallback to `index.html` the server 404s and the
  user gets a blank page on every refresh, deep link and back-button press. It
  presents as a broken app; it is a missing branch.
* **Path traversal guard.** `dist/../../../../etc/passwd` is rejected with 400
  and logged, *after* `decodeURIComponent` — because `%2e%2e%2f` decodes to
  `../`. Verified against four encodings.
* **Caching.** `/assets/*` filenames are content-hashed by Vite, so they get
  `max-age=31536000, immutable`. `index.html` gets `no-cache`, or a deploy is
  invisible until somebody hard-refreshes.
* Fails fast at boot if `index.html` is missing, with the build command in the
  message, instead of 500ing every request.

### `proxy.js` — `/api/*` to the gateway

* Streams both directions, so a large export is never held in memory.
* Strips hop-by-hop headers (`connection`, `transfer-encoding`, …) per RFC 7230;
  forwarding them makes Node believe in a keep-alive socket that does not exist,
  which produces truncated responses that are extremely hard to debug.
* **Passes the gateway's status code through unchanged**, 503 included.
* Fails fast with 502 on an unreachable upstream instead of hanging.
* Unwraps Node's `AggregateError` in the 502 body. When `API_TARGET` names
  `localhost`, Node resolves both `::1` and `127.0.0.1` and the resulting error
  has an **empty** `.message` — so the most useful field would be blank exactly
  when the gateway is down. It now names both addresses.

**What it deliberately does not do:** TLS, retries, circuit breaking, rate
limiting, compression. Those belong at the gateway or in a real ingress. A
"just a little" proxy that retries silently is worse than one that does not: it
turns a fast, obvious 502 into a slow, confusing timeout.

---

## Uninstall

```bash
./uninstall.sh                    # stop, deregister, remove the systemd unit
./uninstall.sh --purge-logs       # also delete .pm2-logs/
```

Order matters, and getting it wrong leaves a machine that resurrects what you
just deleted: `pm2 delete` → **`pm2 save`** (overwrite the dump) → remove the
systemd unit → `pm2 kill`.

It removes only the two CSPH apps **by name**, never `pm2 delete all` — a shared
server may be running unrelated Node applications.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| every route 404s in the browser | `dist/` missing or empty | `npm run build`; check the `FATAL no index.html` line in the log |
| page loads, every API call fails, browser console shows `ERR_CONNECTION_REFUSED` to `localhost` | the bundle has `VITE_API_BASE_URL` baked in as an absolute localhost URL | it is **build time** — see the top of this file. Rebuild. |
| 503 "Unable to find instance" | a service is not registered with Eureka | `k8s/scripts/debug.sh`, section 5. Not a PM2 problem. |
| 502 with `ECONNREFUSED` in the body | the gateway is not reachable on `API_TARGET` | `k8s/scripts/status.sh`; check host port 8080 |
| `csph-frontend` offline, restart count climbing | usually port 3000 already in use | `ss -lntp | grep 3000` |
| works, then nothing after a reboot | `pm2 startup` was never installed | `./install.sh`, or run the `sudo` line it prints |
| `EADDRINUSE` on boot | a stale process from a previous run | `pm2 kill && pm2 resurrect` |
| process exits 1 immediately, log says `no index.html` | the SPA was never built | `cd csph-fleet-frontend && npm run build` |
