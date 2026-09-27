/**
 * server.js â€” static file server for the built Vite SPA. Zero dependencies.
 *
 * WHY NOT `vite preview` OR `npx serve`
 *   `vite preview` is a dev-server convenience: it is not meant to be exposed to
 *   a network, it does not set security headers, and it needs the whole frontend
 *   toolchain installed on the production server just to serve files that are
 *   already built. `npx serve` is better but adds an unpinned dependency that
 *   has to be audited and updated forever.
 *
 *   The production artefact is a directory of static files in
 *   `apps/web/dist`. Serving it needs an HTTP server, a MIME table, a path
 *   traversal guard and an SPA fallback. That is what this is, and nothing more.
 *
 * THE PART THAT ACTUALLY MATTERS: THE SPA FALLBACK
 *   The frontend is a TanStack Router SPA with STATIC routes. A user who
 *   bookmarks /transporters and reloads asks the server for
 *   /transporters â€” a path that does not exist on disk. Without a fallback to
 *   index.html the server returns 404 and the user gets a blank page on every
 *   refresh, deep link and back-button press. It looks like a broken app; it is
 *   a missing three-line branch.
 *
 * THE OTHER PART THAT MATTERS: PATH TRAVERSAL
 *   `dist/../../../../etc/passwd` must not be servable. `decodeURIComponent` +
 *   an explicit prefix check on the resolved path is the whole defence, and it
 *   has to happen AFTER decoding, because `%2e%2e%2f` decodes to `../`.
 *
 * READINESS
 *   Sends `process.send('ready')` once the socket is actually listening, which
 *   is what PM2's `wait_ready: true` is waiting for. Sending it earlier would let
 *   PM2 declare the app healthy before it can serve a single request, which
 *   turns a slow start into a restart loop.
 *
 * ENV
 *   PORT          listen port                default 3000
 *   STATIC_ROOT   directory to serve         default ../csph-fleet-frontend/apps/web/dist
 *   SPA_FALLBACK  file for unknown routes    default index.html
 */

'use strict'

const fs = require('node:fs')
const path = require('node:path')
const http = require('node:http')

const PORT = Number(process.env.PORT || 3000)
const ROOT = path.resolve(process.env.STATIC_ROOT || path.join(__dirname, '..', '..', '..', '..', 'csph-fleet-frontend', 'apps', 'web', 'dist'))
const SPA_FALLBACK = process.env.SPA_FALLBACK || 'index.html'

// Only the types this SPA actually serves. An allowlist, not a deny-list: an
// unknown extension is served as octet-stream rather than guessed at.
const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
  '.avif': 'image/avif',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json; charset=utf-8',
  '.txt': 'text/plain; charset=utf-8',
  '.webmanifest': 'application/manifest+json',
}

function log(msg) {
  process.stdout.write(`[${new Date().toISOString()}] ${msg}\n`)
}

/**
 * Resolve a request URL to a file inside ROOT, or null if it escapes.
 * Returns null rather than throwing so the caller decides what to do.
 */
function resolveFile(urlPath) {
  let decoded
  try {
    decoded = decodeURIComponent(urlPath.split('?')[0].split('#')[0])
  } catch {
    // Malformed percent-encoding, e.g. /%. Treat as not found.
    return null
  }
  if (decoded.includes('\0')) return null

  // path.join collapses `..`, so the prefix check below is the real guard.
  const candidate = path.join(ROOT, decoded)
  if (candidate !== ROOT && !candidate.startsWith(ROOT + path.sep)) {
    return null
  }
  return candidate
}

/** Serve `filePath`; on failure serve the SPA fallback, then 404. */
function serveFile(res, filePath) {
  fs.stat(filePath, (err, stat) => {
    if (err || !stat.isFile()) {
      if (filePath.endsWith(SPA_FALLBACK)) {
        res.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' })
        res.end('404 Not Found\n')
        return
      }
      // Unknown path: hand it to the SPA and let the client-side router
      // decide what it means. See the header comment.
      serveFile(res, path.join(ROOT, SPA_FALLBACK))
      return
    }

    const ext = path.extname(filePath).toLowerCase()
    const headers = {
      'content-type': MIME[ext] || 'application/octet-stream',
      'x-content-type-options': 'nosniff',
    }
    // Vite emits content-hashed asset filenames under /assets, so those are
    // safe to cache forever. index.html must never be cached or a deploy is
    // invisible until a hard refresh.
    if (isAssetPath(filePath)) {
      headers['cache-control'] = 'public, max-age=31536000, immutable'
    } else {
      headers['cache-control'] = 'no-cache'
    }

    res.writeHead(200, headers)
    fs.createReadStream(filePath)
      .on('error', () => {
        res.destroy()
      })
      .pipe(res)
  })
}

function isAssetPath(filePath) {
  const rel = path.relative(ROOT, filePath)
  return rel.split(path.sep)[0] === 'assets'
}

if (!fs.existsSync(path.join(ROOT, 'index.html'))) {
  process.stderr.write(
    `[FATAL] no index.html in ${ROOT}\n` +
      `        Build the SPA first:  cd csph-fleet-frontend && npm run build\n`
  )
  process.exit(1)
}

const server = http.createServer((req, res) => {
  const filePath = resolveFile(req.url || '/')
  if (filePath === null) {
    log(`REJECT ${req.method} ${req.url} (path traversal)`)
    res.writeHead(400, { 'content-type': 'text/plain; charset=utf-8' })
    res.end('400 Bad Request\n')
    return
  }
  // "/" must map to index.html explicitly; path.join(ROOT, "/") is ROOT itself,
  // which is a directory, not a file.
  serveFile(res, filePath === ROOT ? path.join(ROOT, SPA_FALLBACK) : filePath)
})

server.on('clientError', (err, socket) => {
  if (socket.writable) socket.end('HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n')
  log(`client error: ${err.message}`)
})

server.listen(PORT, () => {
  log(`csph-frontend serving ${ROOT}`)
  log(`listening on :${PORT}  (SPA fallback: ${SPA_FALLBACK})`)
  // The signal PM2's wait_ready is listening for. Sent only now, once the
  // socket is genuinely accepting connections.
  if (typeof process.send === 'function') process.send('ready')
})

server.on('error', (err) => {
  process.stderr.write(`[FATAL] ${err.message}\n`)
  process.exit(1)
})

function shutdown(signal) {
  log(`${signal}: draining`)
  server.close(() => process.exit(0))
  // Do not hang forever on a wedged keep-alive connection.
  setTimeout(() => process.exit(0), 5000).unref()
}
process.on('SIGINT', () => shutdown('SIGINT'))
process.on('SIGTERM', () => shutdown('SIGTERM'))
