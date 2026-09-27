/**
 * proxy.js — a ~90-line, zero-dependency reverse proxy for /api/*.
 *
 * WHY THIS EXISTS
 *   The Vite SPA (csph-fleet-frontend) talks to the api-gateway. In local dev
 *   `vite dev` proxies /api -> http://localhost:8080 (see the frontend's
 *   vite.config.ts), so the browser sees one origin and CORS never enters the
 *   picture. In production there is no Vite server: the built `dist/` is static
 *   files, and something has to serve them AND proxy /api.
 *
 *   If the SPA is pointed straight at http://<host>:8080, every request becomes
 *   cross-origin and needs CORS, preflights, and a gateway CORS configuration
 *   that has to be correct for every origin the SPA is served from. Serving both
 *   from one Node process removes the problem entirely: the browser sees one
 *   origin, `/api/*` is same-origin, and no CORS configuration is needed at all.
 *
 * WHY NODE'S BUILT-IN http AND NOT A LIBRARY
 *   This runs on a production server as a long-lived, supervised process. A
 *   dependency here is a dependency to audit, patch and update forever, for
 *   something `node:http` does natively. It is deliberately boring: no
 *   express, no http-proxy, no node-fetch.
 *
 * WHAT IT DOES AND DOES NOT DO
 *   DOES  stream request and response bodies, so a 200 MB export does not have
 *         to fit in memory
 *   DOES  forward status code and the headers that matter
 *   DOES  fail fast and loudly if the gateway is down, rather than hanging
 *   DOES NOT add TLS, retries, circuit breaking, rate limiting, auth or
 *         compression. Those belong at the gateway or in a real ingress. A
 *         "just a little" proxy that does retries silently is worse than one
 *         that does not: it turns a fast, obvious 502 into a slow, confusing
 *         timeout.
 *
 * ENV
 *   PORT         listen port                        default 3000
 *   API_TARGET   gateway origin                     default http://localhost:8080
 *   API_PREFIX   path prefix to proxy               default /api
 *   PROXY_TIMEOUT_MS  socket/idle timeout           default 120000
 *   LOG_REQUESTS      log one line per request      default false
 */

'use strict'

const http = require('node:http')
const { URL } = require('node:url')

const PORT = Number(process.env.PORT || 3000)
const API_TARGET = process.env.API_TARGET || 'http://localhost:8080'
const API_PREFIX = process.env.API_PREFIX || '/api'
const TIMEOUT_MS = Number(process.env.PROXY_TIMEOUT_MS || 120000)
const LOG_REQUESTS = process.env.LOG_REQUESTS === 'true'

/**
 * Headers that describe THIS hop's connection, not the end-to-end message.
 * Forwarding them would make Node think the client is still connected over a
 * keep-alive socket that does not exist, which produces truncated responses
 * that are extremely hard to debug. hop-by-hop per RFC 7230 §6.1.
 */
const HOP_BY_HOP = new Set([
  'connection',
  'keep-alive',
  'proxy-authenticate',
  'proxy-authorization',
  'te',
  'trailer',
  'transfer-encoding',
  'upgrade',
])

function log(level, msg) {
  const line = `[${new Date().toISOString()}] ${level} ${msg}\n`
  if (level === 'ERROR') process.stderr.write(line)
  else process.stdout.write(line)
}

/**
 * Turn a request error into something a human can act on.
 *
 * This exists because of a real, measured failure: when API_TARGET names
 * `localhost`, Node resolves it to BOTH ::1 and 127.0.0.1 and attempts both.
 * If the port is closed, the resulting error is an AggregateError whose
 * `.message` is the EMPTY STRING — verified:
 *
 *     API_TARGET=http://localhost:9999   ->  detail: ""        (useless)
 *     API_TARGET=http://127.0.0.1:9999   ->  detail: "connect ECONNREFUSED …"
 *
 * So the field that matters most is blank in precisely the case an operator
 * will hit it — the gateway being down — and the log line reads
 * "ERROR GET /api/v1/x -> " with nothing after the arrow. Unwrapping the
 * aggregate restores the per-address reasons, which is the difference between
 * "it is down" and "it is down and here is which address refused".
 */
function describeError(err) {
  const parts = []
  if (err && err.code) parts.push(err.code)
  if (err && err.message) parts.push(err.message)
  if (err && Array.isArray(err.errors)) {
    for (const sub of err.errors) {
      const bits = [sub.code, sub.message].filter(Boolean)
      if (bits.length) parts.push(`(${bits.join(': ')})`)
    }
  }
  return parts.join(' ').trim() || 'unknown error'
}

/** Parse API_TARGET once, at boot, so a typo fails immediately and loudly. */
let target
try {
  target = new URL(API_TARGET)
} catch (err) {
  log('ERROR', `API_TARGET is not a valid URL: ${API_TARGET} (${err.message})`)
  process.exit(1)
}

const server = http.createServer((req, res) => {
  const started = Date.now()

  if (!req.url || !req.url.startsWith(API_PREFIX)) {
    res.writeHead(404, { 'content-type': 'application/json' })
    res.end(
      JSON.stringify({
        success: false,
        message: `This process only proxies ${API_PREFIX}/*. Static assets are served by csph-frontend.`,
      }) + '\n'
    )
    return
  }

  // target is a validated URL, so this cannot throw.
  const upstream = new URL(req.url, target)
  const headers = {}
  for (const [key, value] of Object.entries(req.headers)) {
    if (!HOP_BY_HOP.has(key.toLowerCase()) && value !== undefined) {
      headers[key] = value
    }
  }
  // The gateway is reached over plain HTTP on localhost; make that explicit so
  // a future TLS-terminating hop does not silently get an https request.
  headers.host = target.host

  const proxyReq = http.request(
    {
      protocol: target.protocol,
      hostname: target.hostname,
      port: target.port || 80,
      method: req.method,
      path: upstream.pathname + upstream.search,
      headers,
    },
    (proxyRes) => {
      const outHeaders = {}
      for (const [key, value] of Object.entries(proxyRes.headers)) {
        if (!HOP_BY_HOP.has(key.toLowerCase()) && value !== undefined) {
          outHeaders[key] = value
        }
      }
      // A gateway that answers 503 ("Unable to find instance for x") is a real,
      // specific answer and must reach the browser unchanged. Rewriting it to
      // 502 would destroy the only diagnostic the SPA has.
      res.writeHead(proxyRes.statusCode || 502, outHeaders)
      // Streamed, never buffered: a large export must not be held in memory.
      proxyRes.pipe(res)
      proxyRes.on('end', () => {
        if (LOG_REQUESTS) {
          log('INFO', `${req.method} ${req.url} -> ${proxyRes.statusCode} in ${Date.now() - started}ms`)
        }
      })
    }
  )

  // An idle upstream is almost always a hung gateway. Failing fast surfaces a
  // 502 in seconds instead of a browser spinner that never resolves.
  proxyReq.setTimeout(TIMEOUT_MS, () => {
    proxyReq.destroy(new Error(`upstream ${API_TARGET} did not respond within ${TIMEOUT_MS}ms`))
  })

  proxyReq.on('error', (err) => {
    const detail = describeError(err)
    log('ERROR', `${req.method} ${req.url} -> ${detail}`)
    if (res.headersSent) {
      res.destroy()
      return
    }
    res.writeHead(502, { 'content-type': 'application/json' })
    res.end(
      JSON.stringify({
        success: false,
        message: 'The API gateway is unreachable.',
        detail,
        hint: `Is api-gateway running, and is API_TARGET correct? (currently ${API_TARGET})`,
      }) + '\n'
    )
  })

  // Streaming the request body in both directions means this also works for
  // multipart uploads, not just JSON.
  req.pipe(proxyReq)
})

server.on('clientError', (err, socket) => {
  if (socket.writable) {
    socket.end('HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n')
  }
  log('ERROR', `client error: ${err.message}`)
})

server.listen(PORT, () => {
  log('INFO', `csph-frontend-api-proxy listening on :${PORT}, ${API_PREFIX}/* -> ${API_TARGET}`)
  log('INFO', `timeout ${TIMEOUT_MS}ms, request logging ${LOG_REQUESTS ? 'on' : 'off'}`)
  // Readiness signal for PM2's wait_ready. Sent only now, once the socket is
  // accepting. Deliberately NOT conditional on the gateway being up: if
  // API_TARGET is wrong, this process is still a healthy proxy that answers 502
  // with a body naming the target. Restarting it would not fix an unreachable
  // gateway, it would just produce a loop of processes that cannot do their job.
  // The gateway's health is a different question, answered by the gateway's own
  // logs and by k8s/scripts/debug.sh.
  if (typeof process.send === 'function') process.send('ready')
})

// Ask PM2 to stop this cleanly on `pm2 restart`, so in-flight requests finish
// instead of being cut off mid-response.
process.on('SIGINT', () => {
  log('INFO', 'SIGINT: draining connections')
  server.close(() => process.exit(0))
  setTimeout(() => process.exit(0), 5000).unref()
})
process.on('SIGTERM', () => {
  log('INFO', 'SIGTERM: draining connections')
  server.close(() => process.exit(0))
  setTimeout(() => process.exit(0), 5000).unref()
})
