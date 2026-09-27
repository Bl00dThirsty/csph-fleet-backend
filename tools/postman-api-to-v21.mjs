/**
 * Postman API-format (.request.yaml) -> Collection v2.1 JSON.
 *
 * The `postman/` tree in this repo is in the new "Postman API" layout: one
 * `<name>.request.yaml` per request, folders carrying a `.resources/
 * definition.yaml`, and collection-level scripts in the collection definition.
 * newman's classic CLI only reads v2.1 JSON, so this flattens that tree into
 * something `newman run` can execute.
 *
 * Everything is driven by what is actually on disk:
 *   - `$kind: http-request`  -> one item
 *   - `order`                -> sort key inside a folder
 *   - `scripts[].type`       -> afterResponse = test, beforeRequest = prerequest
 *   - on a *definition.yaml* the same field uses the collection-level names
 *     (`http:beforeRequest` / `http:afterResponse`)
 *
 * Usage:  node tools/postman-api-to-v21.mjs
 * Output: postman/build/<name>.collection.json  +  csph-local.environment.json
 */

import { readFileSync, readdirSync, existsSync, writeFileSync, mkdirSync } from 'node:fs'
import { join, dirname, basename, relative } from 'node:path'
import { fileURLToPath } from 'node:url'
import { parse as parseYaml } from 'yaml'

const HERE = dirname(fileURLToPath(import.meta.url))
const POSTMAN_DIR = join(HERE, '..', 'postman')
const BUILD_DIR = join(POSTMAN_DIR, 'build')

const TEST_LISTENS = new Set(['afterresponse', 'http:afterresponse', 'test'])
const PREREQ_LISTENS = new Set(['beforerequest', 'http:beforerequest', 'prerequest'])

function readDoc(path) {
  const text = readFileSync(path, 'utf8')
  // `|-` block scalars are used for the JS test scripts; they contain `:` and
  // `#` freely, which is exactly why a real parser is required here.
  return parseYaml(text)
}

function toHeaderArray(headers) {
  if (!headers || typeof headers !== 'object') return []
  return Object.entries(headers).map(([key, value]) => ({
    key,
    value: value == null ? '' : String(value),
    type: 'text',
  }))
}

function splitUrl(rawUrl) {
  const [base, search] = String(rawUrl).split('?')
  const query = (search || '')
    .split('&')
    .filter(Boolean)
    .map((pair) => {
      const eq = pair.indexOf('=')
      return eq === -1
        ? { key: pair, value: '' }
        : { key: pair.slice(0, eq), value: pair.slice(eq + 1) }
    })

  // `path` must be a FLAT array of segments: Postman joins it with "/", so a
  // nested array silently yields a URL of "[object Object]".
  //
  // The leading `{{gateway}}` is an ORIGIN, not a path segment. Left inside
  // `path`, Postman resolves it to `http://localhost:8080` and then treats the
  // whole thing as relative — every request fails with "Invalid protocol:
  // /http:". So the first placeholder becomes the `host`, exactly as the
  // classic v2.1 form of this suite did.
  const placeholderOrigin = base.match(/^(\{\{[^}]+\}\})(\/.*)?$/)
  if (placeholderOrigin) {
    return {
      host: [placeholderOrigin[1]],
      path: (placeholderOrigin[2] || '/').split('/'),
      query,
    }
  }

  const absolute = base.match(/^(https?:\/\/[^/]+)(\/.*)?$/)
  if (absolute) {
    return { host: [absolute[1]], path: (absolute[2] || '/').split('/'), query }
  }

  return { host: [], path: base.split('/'), query }
}

function toEvents(scripts) {
  const events = []
  for (const s of scripts || []) {
    if (!s || !s.code) continue
    const type = String(s.type || '').toLowerCase()
    let listen = null
    if (TEST_LISTENS.has(type)) listen = 'test'
    else if (PREREQ_LISTENS.has(type)) listen = 'prerequest'
    if (listen) {
      events.push({
        listen,
        script: { type: s.language || 'text/javascript', exec: s.code },
      })
    }
  }
  return events
}

function requestToItem(doc) {
  const { host, path, query } = splitUrl(doc.url)

  const request = {
    method: doc.method || 'GET',
    header: toHeaderArray(doc.headers),
    url: {
      raw: String(doc.url),
      host,
      path,
      ...(query.length ? { query } : {}),
    },
  }
  if (doc.description) request.description = doc.description

  if (doc.body && doc.body.content) {
    request.body = {
      mode: doc.body.type || 'raw',
      raw: doc.body.content,
      ...(doc.body.type === 'json'
        ? { options: { raw: { language: 'json' } } }
        : {}),
    }
  }

  const item = { name: doc.name || basename(String(doc.url)) || 'request', request }
  const events = toEvents(doc.scripts)
  if (events.length) item.event = events
  return item
}

function readDefinition(dir) {
  const p = join(dir, '.resources', 'definition.yaml')
  return existsSync(p) ? readDoc(p) : null
}

function buildCollection(root) {
  const name = basename(root)
  const def = readDefinition(root)

  const collection = {
    info: {
      name,
      description: def?.description || '',
      schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
    },
    item: [],
  }
  const events = toEvents(def?.scripts)
  if (events.length) collection.event = events
  if (def?.auth) collection.auth = def.auth

  for (const entry of readdirSync(root, { withFileTypes: true })) {
    if (!entry.isDirectory() || entry.name === '.resources') continue
    const sub = join(root, entry.name)
    const subDef = readDefinition(sub)

    const ordered = readdirSync(sub)
      .filter((f) => f.endsWith('.request.yaml'))
      .map((f) => ({ file: f, doc: readDoc(join(sub, f)) }))
      // `order` is the suite's own sequencing (folders chain through captured
      // ids), so honouring it is what makes the run reproducible.
      .sort((a, b) => {
        const oa = a.doc.order ?? 0
        const ob = b.doc.order ?? 0
        return oa === ob ? a.file.localeCompare(b.file) : oa - ob
      })
      .map((x) => requestToItem(x.doc))

    const folder = { name: entry.name, item: ordered }
    if (subDef?.description) folder.description = subDef.description
    const subEvents = toEvents(subDef?.scripts)
    if (subEvents.length) folder.event = subEvents
    collection.item.push(folder)
  }

  return collection
}

function yamlEnvToJson(doc) {
  return {
    id: 'csph-local-newman',
    name: doc.name || 'csph-local',
    values: (doc.values || []).map((v) => ({
      key: v.key,
      value: v.value == null ? '' : String(v.value),
      type: 'default',
      enabled: true,
    })),
    _postman_variable_scope: 'environment',
    _postman_exported_using: 'postman-api-to-v21',
  }
}

// ── main ──────────────────────────────────────────────────────────────────

mkdirSync(BUILD_DIR, { recursive: true })

let totalRequests = 0
const summary = []

for (const entry of readdirSync(POSTMAN_DIR, { withFileTypes: true })) {
  if (!entry.isDirectory() || entry.name === 'build' || entry.name === '.postman') continue
  const root = join(POSTMAN_DIR, entry.name)
  if (!existsSync(join(root, '.resources', 'definition.yaml'))) continue

  const collection = buildCollection(root)
  const out = join(BUILD_DIR, `${entry.name}.collection.json`)
  writeFileSync(out, JSON.stringify(collection, null, 2))

  const requests = collection.item.reduce((acc, f) => acc + f.item.length, 0)
  const withTests = collection.item.reduce(
    (acc, f) => acc + f.item.filter((i) => i.event?.some((e) => e.listen === 'test')).length,
    0,
  )
  totalRequests += requests
  summary.push({ name: entry.name, folders: collection.item.length, requests, withTests })
  console.log(
    `  ${entry.name}: ${collection.item.length} folders, ${requests} requests (${withTests} with test scripts)`,
  )
}

const envYaml = join(POSTMAN_DIR, 'csph-local.environment.yaml')
if (existsSync(envYaml)) {
  const env = yamlEnvToJson(readDoc(envYaml))
  writeFileSync(join(BUILD_DIR, 'csph-local.environment.json'), JSON.stringify(env, null, 2))
  console.log(`  csph-local: ${env.values.length} environment variables`)
}

console.log(`\n${summary.length} collection(s), ${totalRequests} requests total.`)
writeFileSync(
  join(BUILD_DIR, 'build-summary.json'),
  JSON.stringify({ generatedFrom: relative(HERE, POSTMAN_DIR), summary }, null, 2),
)
