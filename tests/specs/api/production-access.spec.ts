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
// Self-sufficient: provisions two fresh MIS orders (one per owner) so the spec
// never contends over seed orders with the parallel prosthetics specs.

const API = 'http://localhost:8085/api';
const PROSTH = `${API}/prosthesis-manufacturing`;

test.describe.configure({ mode: 'serial' });

let h7: Record<string, string>;
let h9: Record<string, string>;
let hNurse: Record<string, string>;
let hHod: Record<string, string>;
let me7: number;
let me9: number;
let inst7: string;
let inst9: string;

async function provisionOrder(
  request: APIRequestContext,
  headers: Record<string, string>,
  patientId: string,
  documentId: number,
): Promise<string> {
  const res = await request.post(`${PROSTH}/orders/provision`, {
    headers,
    data: { patientId, documentId },
  });
  expect(res.ok(), `provision failed: ${res.status()}: ${await res.text()}`).toBeTruthy();
  return ((await res.json()) as { id: string }).id;
}

async function createInstance(
  request: APIRequestContext,
  headers: Record<string, string>,
  orderId: string,
  templateId: string,
): Promise<string> {
  const res = await request.post(`${PROSTH}/instances`, {
    headers,
    data: { orderId, templateId },
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

  const me7Res = await request.get(`${API}/users/me`, { headers: h7 });
  expect(me7Res.ok()).toBeTruthy();
  me7 = ((await me7Res.json()) as { id: number }).id;
  expect(typeof me7).toBe('number');
  const me9Res = await request.get(`${API}/users/me`, { headers: h9 });
  me9 = ((await me9Res.json()) as { id: number }).id;

  // Two distinct MIS documents → two independent orders (no seed contention).
  const cands = (await (await request.get(`${PROSTH}/patients/candidates`, { headers: h9 })).json()) as Array<{
    patient: { id: string };
    documents: Array<{ documentId: number }>;
  }>;
  const pairs: Array<{ patientId: string; documentId: number }> = [];
  for (const c of cands ?? []) {
    for (const d of c.documents ?? []) {
      pairs.push({ patientId: c.patient.id, documentId: d.documentId });
      if (pairs.length === 2) break;
    }
    if (pairs.length === 2) break;
  }
  expect(pairs.length, 'need 2 MIS order documents for the production spec').toBe(2);

  const templateId = await findTemplateByIdName(request, h9, 'TP-UL-01');
  const order7 = await provisionOrder(request, h9, pairs[0].patientId, pairs[0].documentId);
  const order9 = await provisionOrder(request, h9, pairs[1].patientId, pairs[1].documentId);
  inst7 = await createInstance(request, h7, order7, templateId);
  inst9 = await createInstance(request, h9, order9, templateId);
});

test.afterAll(async ({ request }) => {
  // Free the provisioned orders; failures here must not fail the suite run.
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
});
