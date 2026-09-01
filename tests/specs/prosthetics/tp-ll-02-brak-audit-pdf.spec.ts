import { test, expect, type APIRequestContext } from '@playwright/test';
import {
  PROSTH,
  API,
  login,
  headersFor,
  findTemplateByIdName,
  createFreeLowerInstance,
  terminateInstance,
  completeToStep,
  createBrakViaApi,
} from '../../helpers/tp-ll-02-flow';

/**
 * Issue #210 — Brak (Bpak) verification & acceptance: HTTP security, audit trail,
 * and branch PDF. Runs under the serial `prosthetics-chromium` project (API-driven).
 *
 * RBAC facts under test:
 *  - POST /instances/{id}/brak  → PROSTHETICS_STEP_COMPLETE (PROSTHETIST/PROSTHETICS_ADMINISTRATOR)
 *  - GET  /instances/{id}/pdf   → PROSTHETICS_DASHBOARD or MODULE_PROSTHETICS_ACCESS
 *  - GET  /audit                → AUDIT_ACCESS or AUDITOR
 *  - an unauthenticated (invalid-JWT) request is never authorized → 401
 *  - a NURSE lacks every code above → 403 (valid body proves the role passed the security gate)
 *
 * A single brak writes three audit records (BrakEvent/CREATE, FlowInstance/BRANCH on the
 * original, FlowInstance/CREATE_BRANCH on the fresh branch); the branch PDF is generated
 * from its IN_PROGRESS state via the owner-scoped GET /pdf.
 */

const STEP_E0028 = 'e0000028-0000-0000-0000-000000000028';
const STAGE_D12 = 'd0000012-0000-0000-0000-000000000012'; // «Виготовлення гіпсового негатива»

/** Start a NEW instance → IN_PROGRESS (both POST /brak and completeToStep require IN_PROGRESS). */
async function startInstance(request: APIRequestContext, headers: Record<string, string>, instanceId: string) {
  const res = await request.post(`${PROSTH}/instances/${instanceId}/start`, { headers });
  expect(res.ok(), `start failed: ${res.status()}: ${await res.text()}`).toBeTruthy();
  expect((await res.json()).status).toBe('IN_PROGRESS');
}

test.describe('TP-LL-02 — Brak audit trail, branch PDF & HTTP security (Issue #210)', () => {
  test('invalid JWT (unauthenticated) POST /brak with a valid body → 401', async ({ request }) => {
    const p1Headers = headersFor(await login(request, 'prosthetist1', 'doctor123'));
    const templateId = await findTemplateByIdName(request, p1Headers, 'TP-LL-02');
    const instance = await createFreeLowerInstance(request, p1Headers, templateId);
    await startInstance(request, p1Headers, instance.id);
    await completeToStep(request, p1Headers, instance.id, STEP_E0028);

    const res = await request.post(`${PROSTH}/instances/${instance.id}/brak`, {
      headers: { Authorization: 'Bearer invalid-token-invalid' },
      data: {
        returnStageId: STAGE_D12,
        softTissueMisalignment: false,
        painDiscomfort: false,
        note: 'unauthenticated',
      },
    });
    expect(res.status()).toBe(401);

    await terminateInstance(request, p1Headers, instance.id);
  });

  test('nurse (lacks PROSTHETICS_STEP_COMPLETE) POST /brak (valid body) → 403', async ({ request }) => {
    const p1Headers = headersFor(await login(request, 'prosthetist1', 'doctor123'));
    const nurseHeaders = headersFor(await login(request, 'nurse1', 'nurse123'));
    const templateId = await findTemplateByIdName(request, p1Headers, 'TP-LL-02');
    const instance = await createFreeLowerInstance(request, p1Headers, templateId);
    await startInstance(request, p1Headers, instance.id);
    await completeToStep(request, p1Headers, instance.id, STEP_E0028);

    // A valid (in-bounds) body so the 403 proves the role failed the permission gate, not validation.
    const res = await request.post(`${PROSTH}/instances/${instance.id}/brak`, {
      headers: nurseHeaders,
      data: {
        returnStageId: STAGE_D12,
        softTissueMisalignment: true,
        painDiscomfort: false,
        note: 'nurse must not brak',
      },
    });
    expect(res.status()).toBe(403);

    await terminateInstance(request, p1Headers, instance.id);
  });

  test('owner PDF for a fresh BRANCHED branch (IN_PROGRESS) returns 200 application/pdf', async ({ request }) => {
    const p1Headers = headersFor(await login(request, 'prosthetist1', 'doctor123'));
    const templateId = await findTemplateByIdName(request, p1Headers, 'TP-LL-02');
    const instance = await createFreeLowerInstance(request, p1Headers, templateId);
    await startInstance(request, p1Headers, instance.id);
    await completeToStep(request, p1Headers, instance.id, STEP_E0028);
    const branch = await createBrakViaApi(request, p1Headers, instance.id, {
      returnStageId: STAGE_D12,
      softTissueMisalignment: false,
    });
    const branchId = branch.newInstanceId as string;
    expect(branchId).toBeTruthy();

    // The branch is a fresh IN_PROGRESS instance; GET /pdf must produce a report for it.
    const res = await request.get(`${PROSTH}/instances/${branchId}/pdf`, { headers: p1Headers });
    expect(res.status()).toBe(200);
    expect((res.headers()['content-type'] ?? '')).toContain('application/pdf');
    expect((await res.body()).length).toBeGreaterThan(0);

    // Free the shared order: the original is BRANCHED (excluded from the active-order unique
    // index), so fail the fresh IN_PROGRESS branch to clear the order.
    await terminateInstance(request, p1Headers, branchId);
  });

  test('admin (AUDIT_ACCESS) reads a brak\'s audit trail: BrakEvent/CREATE, FlowInstance/BRANCH (original), FlowInstance/CREATE_BRANCH (branch)', async ({ request }) => {
    const p1Headers = headersFor(await login(request, 'prosthetist1', 'doctor123'));
    const adminHeaders = headersFor(await login(request, 'admin', 'admin123'));
    const templateId = await findTemplateByIdName(request, p1Headers, 'TP-LL-02');
    const instance = await createFreeLowerInstance(request, p1Headers, templateId);
    await startInstance(request, p1Headers, instance.id);
    await completeToStep(request, p1Headers, instance.id, STEP_E0028);
    const branch = await createBrakViaApi(request, p1Headers, instance.id, {
      returnStageId: STAGE_D12,
      painDiscomfort: true,
      note: 'audit trail E2E',
    });
    const eventId = branch.brakEventId as string;
    expect(eventId).toBeTruthy();

    // The admin holds AUDIT_ACCESS and can read the three records a single brak writes.
    const brakRes = await request.get(`${API}/audit?entity=BrakEvent&entityId=${eventId}&pageSize=20`, {
      headers: adminHeaders,
    });
    expect(brakRes.status()).toBe(200);
    expect(((await brakRes.json()) as any).content.map((e: any) => e.action)).toContain('CREATE');

    const branchedRes = await request.get(`${API}/audit?entity=FlowInstance&entityId=${instance.id}&pageSize=20`, {
      headers: adminHeaders,
    });
    expect(branchedRes.status()).toBe(200);
    expect(((await branchedRes.json()) as any).content.map((e: any) => e.action)).toContain('BRANCH');

    const createdRes = await request.get(`${API}/audit?entity=FlowInstance&entityId=${branch.newInstanceId}&pageSize=20`, {
      headers: adminHeaders,
    });
    expect(createdRes.status()).toBe(200);
    expect(((await createdRes.json()) as any).content.map((e: any) => e.action)).toContain('CREATE_BRANCH');

    // A NURSE (no AUDIT_ACCESS) is denied the same read.
    const nurseRes = await request.get(`${API}/audit?entity=BrakEvent&entityId=${eventId}`, {
      headers: headersFor(await login(request, 'nurse1', 'nurse123')),
    });
    expect(nurseRes.status()).toBe(403);

    // Free the shared order: the original is BRANCHED, so fail the fresh IN_PROGRESS branch.
    await terminateInstance(request, p1Headers, branch.newInstanceId as string);
  });
});
