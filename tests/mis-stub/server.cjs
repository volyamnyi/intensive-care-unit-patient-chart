/**
 * MIS sidecar-stub for E2E (issue #297).
 *
 * Speaks the live MIS envelope contract byte-for-byte (see
 * backend/common/.../mis/MisApiClient.java + MisAuthService.java and the
 * reference chain test MisRealHttpChainTest.java):
 *   POST {base}/token   (application/x-www-form-urlencoded:
 *                        grant_type=password, username={login}@@@{guid},
 *                        password={password})
 *     -> {access_token, token_type, expires_in, login}
 *   POST {base}/api/run (application/json:
 *                        {name, params:[{name,value}], installationId},
 *                        Authorization: Bearer <token>)
 *     -> procedure envelope, e.g. {"spiPatientProsthesCheck":[...]}
 *
 * Data comes exclusively from ./fixtures.json (hand-crafted, no PII).
 * Binds 127.0.0.1 only. Logs method + path + procedure name — never bodies.
 *
 * Usage:
 *   node tests/mis-stub/server.cjs            # MIS_STUB_PORT=9099 default
 *   MIS_STUB_PORT=0 node tests/mis-stub/server.cjs   # ephemeral (prints port)
 *
 * Health: GET /health -> 200 {"status":"ok"}.
 */

'use strict';

const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const querystring = require('node:querystring');

const SPI_PATIENT_PROCEDURE = 'spiPatientProsthesCheck';
const SPI_DOCUMENT_PROCEDURE = 'spiDocumentProsthesCheck';
const SPI_MEDICINE_PROCEDURE = 'spiMedicineItemKindDetails';

const KNOWN_PROCEDURES = new Set([
  SPI_PATIENT_PROCEDURE,
  SPI_DOCUMENT_PROCEDURE,
  SPI_MEDICINE_PROCEDURE,
]);

const MAX_BODY_BYTES = 1024 * 1024;

function loadFixtures() {
  const raw = fs.readFileSync(path.join(__dirname, 'fixtures.json'), 'utf8');
  return JSON.parse(raw);
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on('data', (chunk) => {
      size += chunk.length;
      if (size > MAX_BODY_BYTES) {
        reject(new Error('body too large'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

function sendJson(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
  });
  res.end(body);
}

/** Fixtures store documentUrl with a {STUB_BASE} placeholder (port-agnostic). */
function withBase(value, base) {
  if (typeof value === 'string') return value.replaceAll('{STUB_BASE}', base);
  if (Array.isArray(value)) return value.map((v) => withBase(v, base));
  if (value !== null && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, withBase(v, base)]));
  }
  return value;
}

function paramValue(params, name) {
  if (!Array.isArray(params)) return undefined;
  const wanted = String(name).toLowerCase();
  const hit = params.find((p) => p && String(p.name).toLowerCase() === wanted);
  return hit ? String(hit.value) : undefined;
}

function createServer({ port = 9099, fixtures = loadFixtures() } = {}) {
  const knownDocIds = new Set();
  for (const docs of Object.values(fixtures.documentsByPatientId || {})) {
    for (const d of docs || []) knownDocIds.add(String(d.documentId));
  }

  const server = http.createServer(async (req, res) => {
    const url = new URL(req.url || '/', 'http://127.0.0.1');
    const base = `http://127.0.0.1:${server.address() ? server.address().port : port}`;
    const log = (extra) =>
      console.log(`[mis-stub] ${req.method} ${url.pathname}${extra ? ` ${extra}` : ''}`);

    try {
      // ---- Health (CI readiness probe) ----
      if (req.method === 'GET' && url.pathname === '/health') {
        log();
        return sendJson(res, 200, { status: 'ok', stub: 'mis' });
      }

      // ---- Served document bytes (availability probe + review iframe src).
      // Mirrors the real portal semantics relied upon by
      // DocumentUrlAvailability: anything but 404 keeps the document.
      if ((req.method === 'GET' || req.method === 'HEAD') && url.pathname.startsWith('/doc/')) {
        const id = url.pathname.slice('/doc/'.length);
        log();
        if (!knownDocIds.has(id)) return sendJson(res, 404, { error: 'unknown-document' });
        const body = `MIS stub document ${id}`;
        res.writeHead(200, {
          'Content-Type': 'text/plain; charset=utf-8',
          'Content-Length': Buffer.byteLength(body),
        });
        return res.end(req.method === 'GET' ? body : undefined);
      }

      // ---- Token endpoint (form-encoded, mirrors the real contract) ----
      if (req.method === 'POST' && url.pathname === '/token') {
        log();
        const form = querystring.parse(await readBody(req));
        const ok =
          form.grant_type === 'password' &&
          typeof form.username === 'string' &&
          form.username.includes('@@@') &&
          typeof form.password === 'string' &&
          form.password.length > 0;
        if (!ok) return sendJson(res, 400, { error: 'invalid-grant' });
        return sendJson(res, 200, {
          access_token: 'e2e-stub-token',
          token_type: 'Bearer',
          expires_in: 3600,
          login: String(form.username).split('@@@')[0],
        });
      }

      // ---- Procedure endpoint ----
      if (req.method === 'POST' && url.pathname === '/api/run') {
        let envelope;
        try {
          envelope = JSON.parse(await readBody(req));
        } catch {
          log('bad-json');
          return sendJson(res, 400, { error: 'malformed-envelope' });
        }
        const name = envelope && envelope.name;
        log(typeof name === 'string' ? `procedure=${name}` : 'no-procedure');

        const auth = req.headers.authorization || '';
        if (!auth.startsWith('Bearer ') || auth.slice('Bearer '.length).trim() === '') {
          return sendJson(res, 401, { error: 'missing-bearer' });
        }
        if (typeof name !== 'string' || !KNOWN_PROCEDURES.has(name)) {
          return sendJson(res, 400, { error: 'unknown-procedure' });
        }
        if (!Array.isArray(envelope.params)) {
          return sendJson(res, 400, { error: 'missing-params' });
        }
        if (paramValue(envelope.params, 'Login') === undefined) {
          return sendJson(res, 400, { error: 'missing-login-param' });
        }
        if (typeof envelope.installationId !== 'string' || envelope.installationId === '') {
          return sendJson(res, 400, { error: 'missing-installation-id' });
        }

        if (name === SPI_PATIENT_PROCEDURE) {
          return sendJson(res, 200, { [SPI_PATIENT_PROCEDURE]: fixtures.patients });
        }
        if (name === SPI_MEDICINE_PROCEDURE) {
          // NOTE: unlike patients/documents, the medicine wrapper key is NOT
          // the procedure name (see MisServiceImpl.parseMedicineList and the
          // MisRealHttpChainTest MEDICINE_ENVELOPE reference).
          return sendJson(res, 200, { medicineItemKindDetails: fixtures.medicines });
        }
        const patientId = paramValue(envelope.params, 'PatientID');
        const docs = (fixtures.documentsByPatientId || {})[patientId] || [];
        return sendJson(res, 200, {
          [SPI_DOCUMENT_PROCEDURE]: withBase(structuredClone(docs), base),
        });
      }

      log();
      return sendJson(res, 404, { error: 'not-found' });
    } catch (err) {
      console.error(`[mis-stub] handler error: ${err && err.message}`);
      if (!res.headersSent) return sendJson(res, 500, { error: 'stub-failure' });
      return res.end();
    }
  });

  return server;
}

if (require.main === module) {
  const port = Number(process.env.MIS_STUB_PORT || 9099);
  const server = createServer({ port });
  server.listen(port, '127.0.0.1', () => {
    console.log(`[mis-stub] listening on http://127.0.0.1:${server.address().port}`);
  });
}

module.exports = { createServer };
