/**
 * MIS-stub contract test (issue #297).
 *
 * Drives tests/mis-stub/server.cjs over real HTTP and asserts the live MIS
 * envelope contract that MisApiClient/MisAuthService/MisServiceImpl rely on
 * (reference: backend/.../integration/MisRealHttpChainTest.java):
 * form-encoded token call, {name, params[], installationId} procedure
 * envelopes, wrapper keys, PatientID param, Bearer auth, and served
 * document URLs for the DocumentUrlAvailability probe.
 *
 * Runner: `node --test tests/mis-stub/` (plain Node, no Playwright).
 * NOTE: the filename intentionally avoids the `.test.` infix so the
 * Playwright runner (testDir '.') never picks this file up; node --test
 * discovers it via the `*-test.cjs` pattern.
 */

'use strict';

const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { createServer } = require('./server.cjs');

const LOGIN = 'e2e-stub';
const INSTALLATION_GUID = 'e2e-stub-guid';

let server;
let base;

async function postForm(path, fields) {
  const body = new URLSearchParams(fields).toString();
  return fetch(`${base}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  });
}

async function runProcedure(name, params, token = 'e2e-stub-token') {
  return fetch(`${base}/api/run`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ name, params, installationId: INSTALLATION_GUID }),
  });
}

const loginParams = [{ name: 'Login', value: LOGIN }];

before(async () => {
  server = createServer({ port: 0 });
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  base = `http://127.0.0.1:${server.address().port}`;
});

after(async () => {
  await new Promise((resolve) => server.close(resolve));
});

describe('mis-stub health', () => {
  it('answers GET /health with 200 ok', async () => {
    const res = await fetch(`${base}/health`);
    assert.equal(res.status, 200);
    assert.equal((await res.json()).status, 'ok');
  });
});

describe('mis-stub token endpoint', () => {
  it('issues a bearer token for a well-formed password grant', async () => {
    const res = await postForm('/token', {
      grant_type: 'password',
      username: `${LOGIN}@@@${INSTALLATION_GUID}`,
      password: 'e2e-stub',
    });
    assert.equal(res.status, 200);
    const body = await res.json();
    assert.equal(body.token_type, 'Bearer');
    assert.ok(typeof body.access_token === 'string' && body.access_token.length > 0);
    assert.ok(Number(body.expires_in) > 0);
  });

  it('rejects a malformed grant with 400 and no secrets', async () => {
    const res = await postForm('/token', { grant_type: 'password', username: LOGIN });
    assert.equal(res.status, 400);
  });
});

describe('mis-stub patient procedure', () => {
  it('returns the roster wrapped under the procedure name', async () => {
    const res = await runProcedure('spiPatientProsthesCheck', loginParams);
    assert.equal(res.status, 200);
    const patients = (await res.json()).spiPatientProsthesCheck;
    assert.ok(Array.isArray(patients) && patients.length > 0);

    const byId = new Map(patients.map((p) => [p.id, p]));
    // Seed-joined prosthetics patients (mirror data-prosth.sql 900001/900002).
    assert.equal(byId.get(900001).departmentId, 19);
    assert.equal(byId.get(900001).fullName, 'Сніжко Іван Петрович');
    assert.equal(byId.get(900002).departmentId, 19);
    // Stub-only anchors.
    assert.equal(byId.get(13373).departmentId, 19);
    assert.equal(byId.get(10101).departmentId, 37);
    // Terminal stay status (excluded from treatment views by MisService).
    assert.equal(byId.get(10401).patientStatus, 'MOV');
    // Non-prosthetics department (present in raw pool, ineligible).
    assert.equal(byId.get(10501).departmentId, 2);

    // Exact 14-field contract parsed by MisServiceImpl.parsePatientList.
    for (const p of patients) {
      for (const field of [
        'id', 'fullName', 'birthDate', 'sexCode', 'address', 'phone', 'email',
        'bloodGroup', 'rhFactor', 'departmentId', 'room', 'bed', 'doctorName',
        'patientStatus',
      ]) {
        assert.ok(p[field] !== undefined, `patient ${p.id} missing ${field}`);
      }
    }
  });
});

describe('mis-stub medicine procedure', () => {
  it('returns the catalog with one disabled entry', async () => {
    const res = await runProcedure('spiMedicineItemKindDetails', loginParams);
    assert.equal(res.status, 200);
    const catalog = (await res.json()).medicineItemKindDetails;
    assert.ok(Array.isArray(catalog) && catalog.length > 0);
    const names = catalog.map((m) => m.itemKindName);
    assert.ok(names.includes('Paracetamol 500 mg'));
    const disabled = catalog.filter((m) => m.itemKindIsDisabled === true);
    assert.equal(disabled.length, 1);
    const enabled = catalog.find(
      (m) => typeof m.itemKindName === 'string' && m.itemKindIsDisabled !== true,
    );
    assert.ok(enabled);
  });
});

describe('mis-stub document procedure', () => {
  it('returns 120/121 documents with live URLs for patient 13373', async () => {
    const res = await runProcedure('spiDocumentProsthesCheck', [
      { name: 'PatientID', value: '13373' },
      ...loginParams,
    ]);
    assert.equal(res.status, 200);
    const docs = (await res.json()).spiDocumentProsthesCheck;
    assert.equal(docs.length, 2);
    assert.deepEqual(
      docs.map((d) => d.documentTemplateId).sort(),
      [120, 121],
    );
    for (const d of docs) {
      assert.ok(String(d.documentUrl).startsWith(`${base}/doc/`), 'URL points at this stub');
      assert.equal(d.patientId, 13373);
    }
  });

  it('returns documents for seed patient 900002 (TP-LL-02 prefill)', async () => {
    const res = await runProcedure('spiDocumentProsthesCheck', [
      { name: 'PatientID', value: '900002' },
      ...loginParams,
    ]);
    assert.equal(res.status, 200);
    const docs = (await res.json()).spiDocumentProsthesCheck;
    assert.equal(docs.length, 1);
    assert.equal(docs[0].documentTemplateId, 121);
    assert.ok(docs[0].patientFullName);
    assert.ok(docs[0].orderNumber);
  });

  it('returns an empty array for patients without documents', async () => {
    const res = await runProcedure('spiDocumentProsthesCheck', [
      { name: 'PatientID', value: '10101' },
      ...loginParams,
    ]);
    assert.equal(res.status, 200);
    assert.deepEqual((await res.json()).spiDocumentProsthesCheck, []);
  });
});

describe('mis-stub strictness (drift detection)', () => {
  it('rejects calls without bearer auth with 401', async () => {
    const res = await fetch(`${base}/api/run`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: 'spiPatientProsthesCheck',
        params: loginParams,
        installationId: INSTALLATION_GUID,
      }),
    });
    assert.equal(res.status, 401);
  });

  it('rejects unknown procedures with 400', async () => {
    const res = await runProcedure('spzIBSomeFutureProcedure', loginParams);
    assert.equal(res.status, 400);
  });

  it('rejects envelopes missing the Login param or installationId', async () => {
    const noLogin = await runProcedure('spiPatientProsthesCheck', []);
    assert.equal(noLogin.status, 400);
    const noInstall = await fetch(`${base}/api/run`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: 'Bearer e2e-stub-token' },
      body: JSON.stringify({ name: 'spiPatientProsthesCheck', params: loginParams }),
    });
    assert.equal(noInstall.status, 400);
  });
});

describe('mis-stub served documents', () => {
  it('serves known document URLs with 200 for GET and HEAD', async () => {
    const docsRes = await runProcedure('spiDocumentProsthesCheck', [
      { name: 'PatientID', value: '13373' },
      ...loginParams,
    ]);
    const docs = (await docsRes.json()).spiDocumentProsthesCheck;
    for (const d of docs) {
      assert.equal((await fetch(d.documentUrl)).status, 200);
      assert.equal((await fetch(d.documentUrl, { method: 'HEAD' })).status, 200);
    }
  });

  it('answers 404 for unknown document ids (real portal semantics)', async () => {
    assert.equal((await fetch(`${base}/doc/999999`)).status, 404);
  });
});
