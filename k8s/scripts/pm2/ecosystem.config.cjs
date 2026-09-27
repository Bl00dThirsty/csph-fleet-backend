/**
 * ecosystem.config.cjs — PM2 process definitions for the CSPH GPL Fleet frontend.
 *
 * CommonJS (`.cjs`) on purpose: PM2 loads this with `require()`, and the
 * frontend repo is `"type": "module"`, so a plain `.js` in a project with a
 * package.json would be parsed as ESM and fail. `.cjs` is unambiguous
 * regardless of what any neighbouring package.json says.
 *
 * WHAT PM2 IS FOR HERE, SPECIFICALLY
 *   The 11 Spring Boot services and the 9 databases are Kubernetes pods; the
 *   scheduler restarts them. The Vite SPA is NOT containerised — it is a
 *   directory of static files plus a small Node process, and on a Linux server
 *   it is the one long-running user-space process that needs supervising. PM2
 *   gives it: restart on crash, restart on OOM, a restart budget, a memory
 *   ceiling, log files with rotation, and survival across reboots via systemd.
 *
 * TWO PROCESSES, AND WHY NOT ONE
 *   csph-frontend               serves apps/web/dist on :3000
 *   csph-frontend-api-proxy     proxies /api/* -> the gateway on :3001
 *
 *   They are separate so that a proxy fault (an unreachable gateway, a hung
 *   upstream, a bad API_TARGET) takes down only the API surface and leaves the
 *   already-loaded SPA serving its shell. The browser then shows a working page
 *   with failing data calls — diagnosable — instead of a connection-refused
 *   that looks like the whole site is down.
 *
 *   They are on separate ports so that a request for /api/* can be routed by
 *   port. If you put both on one port you need a router, and then you have
 *   written a third process. See the "one port instead" note in README.md.
 *
 * PATHS
 *   The frontend repo is a SIBLING of this one:
 *       <parent>/csph-fleet-backend/k8s/scripts/pm2/ecosystem.config.cjs
 *       <parent>/csph-fleet-frontend/apps/web/dist
 *   Override CSPH_FRONTEND_ROOT if your layout differs — the resolved path is
 *   printed at boot precisely so a wrong guess is obvious in the first second
 *   rather than at the first request.
 *
 * USAGE
 *   pm2 start ecosystem.config.cjs --env production
 *   pm2 save
 *   pm2 startup            # prints a sudo line; run it
 */

'use strict'

const path = require('node:path')

// k8s/scripts/pm2 -> k8s/scripts -> k8s -> csph-fleet-backend -> <parent>
const PARENT = path.resolve(__dirname, '..', '..', '..', '..')

const FRONTEND_ROOT = process.env.CSPH_FRONTEND_ROOT
  ? path.resolve(process.env.CSPH_FRONTEND_ROOT)
  : path.join(PARENT, 'csph-fleet-frontend')

const DIST = path.join(FRONTEND_ROOT, 'apps', 'web', 'dist')
const LOG_DIR = path.join(FRONTEND_ROOT, '.pm2-logs')

// Same log directory for both processes, distinct files, so a crash in one does
// not scroll the other out of view. max_size 10M x 5 files = 50 MB per process;
// without a cap, a chatty SPA in a loop fills a server disk overnight.
const logOpts = (name) => ({
  out_file: path.join(LOG_DIR, `${name}.out.log`),
  error_file: path.join(LOG_DIR, `${name}.err.log`),
  log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
  merge_logs: false,
  max_size: '10M',
  retain_files: 5,
  compress: false,
})

// ---------------------------------------------------------------------------
// Shared restart policy.
//
// `max_restarts: 10` with `expire_restart_delay` is the combination that makes
// PM2 useful rather than harmful:
//
//   * restart: 'always'        — the process must come back after a crash, a
//                                SIGTERM from an operator, or an OOM kill.
//   * max_restarts: 10         — but not forever. A process that dies 10 times
//                                in the window below is broken, and restarting
//                                it an 11th time just hides the failure inside
//                                a restart loop that never surfaces.
//   * expire_restart_delay: 90 — and the counting window is 90 SECONDS, not 10
//                                restarts ever. This is the difference between
//                                "crashed 10 times today" (fine, self-healing)
//                                and "crashed 10 times in 90 seconds" (a boot
//                                loop; stop and read the log).
//
// `restart_delay: 2000` adds 2s between restarts so a crash-on-start does not
// become a hot loop that starves everything else on the box.
//
// max_memory_restart: 512M is a hard ceiling. The static server holds one file
// stream at a time, so 512M is already generous; exceeding it means something
// is wrong (a symlink loop, a runaway cache) and a restart is the correct
// response rather than the OOM killer taking out an unrelated process.
// ---------------------------------------------------------------------------
const restartPolicy = {
  autorestart: true,
  restart: 'always',
  max_restarts: 10,
  expire_restart_delay: 90,
  restart_delay: 2000,
  max_memory_restart: '512M',
}

module.exports = {
  apps: [
    // ======================================================================
    // 1. The static SPA
    // ======================================================================
    {
      name: 'csph-frontend',
      script: path.join(__dirname, 'server.js'),
      // cwd is the app root, not the script directory: a relative path in an
      // error message or a stack trace should be readable as a project path.
      cwd: DIST,
      interpreter: 'node',
      exec_mode: 'fork',

      // wait_ready + listen_timeout: PM2 does not consider the process online
      // until server.js calls process.send('ready'), which it does only after
      // server.listen() has actually bound. listen_timeout is the ceiling on
      // that wait — 20s is generous for opening a socket, and it means a
      // process that hangs during boot is killed and restarted rather than
      // sitting there in a permanently "starting" state.
      wait_ready: true,
      listen_timeout: 20000,

      restart: restartPolicy,
      env: {
        NODE_ENV: 'production',
        PORT: '3000',
        STATIC_ROOT: DIST,
        SPA_FALLBACK: 'index.html',
      },
      env_production: {
        NODE_ENV: 'production',
        PORT: '3000',
        STATIC_ROOT: DIST,
        SPA_FALLBACK: 'index.html',
      },
      out_file: logOpts('csph-frontend').out_file,
      error_file: logOpts('csph-frontend').error_file,
      log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
      max_size: '10M',
      retain_files: 5,
      merge_logs: false,
    },

    // ======================================================================
    // 2. The /api/* proxy to the gateway
    // ======================================================================
    {
      name: 'csph-frontend-api-proxy',
      script: path.join(__dirname, 'proxy.js'),
      cwd: path.join(FRONTEND_ROOT, 'apps', 'web'),
      interpreter: 'node',
      exec_mode: 'fork',

      // Same readiness contract as the static server, and it is meaningful
      // here too. Note WHAT readiness does not mean: it does not mean the
      // gateway is reachable. If API_TARGET is wrong, this process is still a
      // healthy proxy — it listens, and it answers 502 with a JSON body naming
      // the target it could not reach. Restarting it would not fix an
      // unreachable gateway; it would only produce a loop of processes that
      // cannot do their job. The gateway's health is a separate question, and
      // k8s/scripts/debug.sh is the tool for it.
      wait_ready: true,
      listen_timeout: 20000,

      restart: restartPolicy,
      env: {
        NODE_ENV: 'production',
        PORT: '3001',
        // The gateway is the kind NodePort, published on the host by
        // k8s/kind-cluster.yaml. On the Linux server that mapping is what
        // makes localhost:8080 the right answer.
        API_TARGET: 'http://localhost:8080',
        API_PREFIX: '/api',
        PROXY_TIMEOUT_MS: '120000',
        LOG_REQUESTS: 'false',
      },
      env_production: {
        NODE_ENV: 'production',
        PORT: '3001',
        API_TARGET: process.env.GATEWAY_URL || 'http://localhost:8080',
        API_PREFIX: '/api',
        PROXY_TIMEOUT_MS: '120000',
        // true in production is a real decision: one line per request in the
        // out log is what makes "the SPA works but login 401s" answerable
        // without attaching a debugger. Set false if the volume is a problem.
        LOG_REQUESTS: 'true',
      },
      out_file: logOpts('csph-frontend-api-proxy').out_file,
      error_file: logOpts('csph-frontend-api-proxy').error_file,
      log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
      max_size: '10M',
      retain_files: 5,
      merge_logs: false,
    },
  ],
}
