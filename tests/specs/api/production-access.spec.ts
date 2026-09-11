import { test, expect, type APIRequestContext } from '@playwright/test';
import { testUser } from '../../helpers/test-users';
import {
  login,
  headersFor,
  findTemplateByIdName,
  terminateInstance,
} from '../../helpers/tp-ll-02-flow';

// API contract for the production monitoring endpoints (issue #274, epic #271):
//   GET /api/prosthesis-manufacturing/production            (PROSTHETICS_PRODUCTION_VIEW)
//   GET /api/prosthesis-manufacturing/production/team       (PROSTHETICS_PRODUCTION_VIEW_ALL)
//   GET /api/prosthesis-manufacturing/production/{id}       (VIEW; masked without PATIENT_VIEW)
// Without VIEW_ALL the caller sees only their own items (assignee filter is
// forced); strangers get 404 on detail (same contract as requireOwner).
//
// Self-sufficient and retry-safe: provisions ONE fresh MIS order (never a seed
// order, so no contention with the parallel prosthetics specs) and hands the
// order from the prosthetist instance to the admin instance mid-file.

const API = 'http://localhost:8085/api';
const PROSTH = `${API}/prosthesis-manufacturing`;

const ACTIVE = ['NEW', 'IN_PROGRESS', 'PAUSED', 'BLOCKED_PATIENT', 'BLOCKED_MATERIAL'];

test.describe.configure({ mode: 'serial' });

let h7: Record<string, string>;
let h9: Record<string, string>;
let hNurse: Record<string, string>;
let hHod: Record<string, string>;
let me7: number;
let me9: number;
let orderA: string;
let templateId: string;
let inst7: string;
let inst9: string;

type InstanceRow = { id: string; orderId: string; status: string; assignedUserId: number };

async function activeOnOrder(
  request: APIRequestContext,
  headers: Record<string, string>,
  orderId: string,
): Promise<InstanceRow | undefined> {
  const list = (await (await request.get(`${PROSTH}/instances`, { headers })).json()) as Array<InstanceRow>;
  return list.find((i) => i.orderId === orderId && ACTIVE.includes(i.status));
}

function headersForAssignee(assignee: number): Record<string, string> {
  return assignee === me7 ? h7 : h9;
}

/** Find-or-create an ACTIVE instance on orderA owned by the given user. */
async function ensureInstance(
  request: APIRequestContext,
  ownerHeaders: Record<string, string>,
  ownerMe: number,
): Promise<string> {
  const active = await activeOnOrder(request, h9, orderA);
  if (active) {
    if (active.assignedUserId === ownerMe) return active.id;
    await terminateInstance(request, headersForAssignee(active.assignedUserId), active.id);
  }
  const res = await request.post(`${PROSTH}/instances`, {
    headers: ownerHeaders,
    data: { orderId: orderA, templateId },
  });
  expect(res.ok(), `create instance failed: ${res.status()}: ${await res.text()}`).toBeTruthy();
  return ((await res.json()) as { id: string }).id;
}

test.beforeAll(async ({ request }) => {
  const t7 = await login(request, testUser(7).login, testUser(7).password);
  const t9 = await login(request, testUser(9).login, testUser(9).password);
  const tNurse = await login(request, testUser(3).login, testUser(3).password);
  const tHod = await login(request, testUser(5).login, testUser(5).password);
  h7 = headersFor(t7);
  h9 = headersFor(t9);
  hNurse = headersFor(tNurse);
  hHod = headersFor(tHod);

  me7 = ((await (await request.get(`${API}/users/me`, { headers: h7 })).json()) as { id: number }).id;
  expect(typeof me7).toBe('number');
  me9 = ((await (await request.get(`${API}/users/me`, { headers: h9 })).json()) as { id: number }).id;

  // One live MIS document is enough: the order is handed from inst7 to inst9.
  // Live-MIS data is outside our control (dept 19/27/37 + docs 120/121): when
  // no candidate carries documents, skip honestly instead of failing on
  // absent data (same contract as prosthetics-e2e.spec.ts, see #267).
  const cands = (await (await request.get(`${PROSTH}/patients/candidates`, { headers: h9 })).json()) as Array<{
    patient: { id: string };
    documents: Array<{ documentId: number }>;
    documentsUnknown?: boolean;
  }>;
  const withDocs = (cands ?? []).filter((c) => (c.documents ?? []).length > 0).length;
  const unknown = (cands ?? []).filter((c) => c.documentsUnknown === true).length;
  console.log(`[production-access] candidates=${(cands ?? []).length} withDocs=${withDocs} documentsUnknown=${unknown}`);
  const pair = (cands ?? []).flatMap((c) =>
    (c.documents ?? []).map((d) => ({ patientId: c.patient.id, documentId: d.documentId })),
  )[0];
  test.skip(!pair, 'need 1 MIS order document for the production spec (live MIS has none right now)');
  expect(pair, 'need 1 MIS order document for the production spec').toBeTruthy();

  templateId = await findTemplateByIdName(request, h9, 'TP-UL-01');
  const prov = await request.post(`${PROSTH}/orders/provision`, {
    headers: h9,
    data: { patientId: pair.patientId, documentId: pair.documentId },
  });
  expect(prov.ok(), `provision failed: ${prov.status()}: ${await prov.text()}`).toBeTruthy();
  orderA = ((await prov.json()) as { id: string }).id;

  inst7 = await ensureInstance(request, h7, me7);
});

test.afterAll(async ({ request }) => {
  // Free the provisioned order; cleanup failures must not fail the suite.
  if (inst7) await terminateInstance(request, h7, inst7).catch(() => undefined);
  if (inst9) await terminateInstance(request, h9, inst9).catch(() => undefined);
});

test.describe('Production monitoring API access controls', () => {
  test('nurse is rejected (403) on list/team/detail', async ({ request }) => {
    expect((await request.get(`${PROSTH}/production`, { headers: hNurse })).status()).toBe(403);
    expect((await request.get(`${PROSTH}/production/team`, { headers: hNurse })).status()).toBe(403);
    expect((await request.get(`${PROSTH}/production/${inst7}`, { headers: hNurse })).status()).toBe(403);
  });

  test('prosthetist list shows only own items (assignee filter forced)', async ({ request }) => {
    const res = await request.get(`${PROSTH}/production`, { headers: h7 });
    expect(res.ok()).toBeTruthy();
    const body = (await res.json()) as { content: Array<{ prosthetistUserId: number }>; totalElements: number };
    expect(Array.isArray(body.content)).toBeTruthy();
    expect(typeof body.totalElements).toBe('number');
    expect(body.content.length).toBeGreaterThan(0);
    for (const row of body.content) expect(row.prosthetistUserId).toBe(me7);

    // Explicit foreign assignee is overridden to self without VIEW_ALL.
    const forced = await request.get(`${PROSTH}/production?assigneeId=${me9}`, { headers: h7 });
    expect(forced.ok()).toBeTruthy();
    const forcedBody = (await forced.json()) as { content: Array<{ prosthetistUserId: number }> };
    for (const row of forcedBody.content) expect(row.prosthetistUserId).toBe(me7);
  });

  test('list supports filters/sort/pagination shape', async ({ request }) => {
    const res = await request.get(`${PROSTH}/production?status=IN_PROGRESS&sort=LONGEST&page=0&size=5`, {
      headers: h9,
    });
    expect(res.ok()).toBeTruthy();
    const body = (await res.json()) as {
      content: Array<{ status: string }>;
      totalElements: number;
      totalPages: number;
    };
    expect(Array.isArray(body.content)).toBeTruthy();
    expect(body.content.length).toBeLessThanOrEqual(5);
  });

  test('prosthetist team is forbidden; admin and HOD may read', async ({ request }) => {
    expect((await request.get(`${PROSTH}/production/team`, { headers: h7 })).status()).toBe(403);

    const adminRes = await request.get(`${PROSTH}/production/team`, { headers: h9 });
    expect(adminRes.ok()).toBeTruthy();
    const team = (await adminRes.json()) as Array<{ userId: number; fullName: string; inWork: number }>;
    expect(Array.isArray(team)).toBeTruthy();
    expect(team.some((r) => r.userId === me7 && typeof r.fullName === 'string')).toBeTruthy();

    expect((await request.get(`${PROSTH}/production/team`, { headers: hHod })).status()).toBe(200);
  });

  test('prosthetist own detail is masked (PIB only, no documents)', async ({ request }) => {
    const res = await request.get(`${PROSTH}/production/${inst7}`, { headers: h7 });
    expect(res.ok()).toBeTruthy();
    const body = (await res.json()) as {
      workItem: { instanceId: string };
      patient: { pib: string; birthDate?: string | null };
      patientDetailsVisible: boolean;
      documents: unknown[];
      matchedDocument: unknown;
      timeline: unknown[];
    };
    expect(body.workItem.instanceId).toBe(inst7);
    expect(typeof body.patient.pib).toBe('string');
    expect(body.patientDetailsVisible).toBe(false);
    expect(body.patient.birthDate ?? null).toBeNull();
    expect(body.documents).toEqual([]);
    expect(body.matchedDocument).toBeNull();
    expect(Array.isArray(body.timeline)).toBeTruthy();
  });

  test('hand off the order: terminate prosthetist instance, create admin instance', async ({ request }) => {
    await terminateInstance(request, h7, inst7);
    inst9 = await ensureInstance(request, h9, me9);
    expect(inst9).not.toBe(inst7);
  });

  test('admin detail is full (patient details + documents shape)', async ({ request }) => {
    const res = await request.get(`${PROSTH}/production/${inst7}`, { headers: h9 });
    expect(res.ok()).toBeTruthy();
    const body = (await res.json()) as {
      patientDetailsVisible: boolean;
      documents: unknown[];
      documentsUnknown: boolean;
      matchedDocument: { documentId: number } | null;
    };
    expect(body.patientDetailsVisible).toBe(true);
    expect(Array.isArray(body.documents)).toBeTruthy();
    expect(typeof body.documentsUnknown).toBe('boolean');
    if (body.documents.length > 0) {
      expect(body.matchedDocument).not.toBeNull();
      expect(typeof body.matchedDocument?.documentId).toBe('number');
    }
  });

  test('prosthetist foreign detail is 404; unknown id is 404', async ({ request }) => {
    expect((await request.get(`${PROSTH}/production/${inst9}`, { headers: h7 })).status()).toBe(404);
    expect(
      (await request.get(`${PROSTH}/production/00000000-0000-0000-0000-000000000000`, { headers: h9 })).status(),
    ).toBe(404);
  });

  test('normative settings: admin roundtrip, prosthetist forbidden, invalid rejected', async ({ request }) => {
    expect((await request.get(`${PROSTH}/production/settings/normative`, { headers: h7 })).status()).toBe(403);
    expect(
      (await request.put(`${PROSTH}/production/settings/normative`, {
        headers: h7,
        data: { overdueMultiplier: 2, staleDays: 3 },
      })).status(),
    ).toBe(403);

    const before = await request.get(`${PROSTH}/production/settings/normative`, { headers: h9 });
    expect(before.ok()).toBeTruthy();
    const beforeBody = (await before.json()) as { overdueMultiplier: number; staleDays: number };
    expect(typeof beforeBody.overdueMultiplier).toBe('number');
    expect(typeof beforeBody.staleDays).toBe('number');

    const updated = await request.put(`${PROSTH}/production/settings/normative`, {
      headers: h9,
      data: { overdueMultiplier: 2, staleDays: 3 },
    });
    expect(updated.ok()).toBeTruthy();
    expect(((await updated.json()) as { overdueMultiplier: number }).overdueMultiplier).toBe(2);

    expect(
      (await request.put(`${PROSTH}/production/settings/normative`, {
        headers: h9,
        data: { overdueMultiplier: 0.5, staleDays: 3 },
      })).status(),
    ).toBe(400);

    // Restore defaults so later specs see a clean matrix.
    const restored = await request.put(`${PROSTH}/production/settings/normative`, {
      headers: h9,
      data: { overdueMultiplier: 1.5, staleDays: 7 },
    });
    expect(restored.ok()).toBeTruthy();
  });
});
