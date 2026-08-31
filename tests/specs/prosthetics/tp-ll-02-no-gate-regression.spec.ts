import { test, expect } from '@playwright/test';
import {
  PROSTH,
  login,
  headersFor,
  findTemplateByIdName,
  findOrderIdByProductType,
  createFreeLowerInstance,
  createFreeInstanceOnOrder,
  completeToCompleted,
  instanceStatus,
  terminateInstance,
} from '../../helpers/tp-ll-02-flow';

const UPPER_ORDER = '20000000-0000-4000-8000-000000000001';

test.describe('TP-LL-02 — No Quality Gate (v2.1 regression, Фаза 5)', () => {
  let prosthetistToken: string;
  let adminToken: string;

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, 'prosthetist1', 'doctor123');
    adminToken = await login(request, 'prosthetics_admin1', 'doctor123');
  });

  test('TP-LL-02 snapshot has NO quality_gates / rework_loops / WAITING_REVIEW', async ({ request }) => {
    const headers = headersFor(prosthetistToken);
    const templateId = await findTemplateByIdName(request, headers, 'TP-LL-02');
    const detail = await (await request.get(`${PROSTH}/templates/${templateId}`, { headers })).json();

    // The template definition must carry no gate/rework fields.
    expect(JSON.stringify(detail)).not.toContain('quality_gates');
    expect(JSON.stringify(detail)).not.toContain('rework_loops');
    for (const stage of detail.stages ?? []) {
      expect(stage.requiresApproval, `stage ${stage.name} must require no approval`).toBeFalsy();
    }

    // The instance-level immutable snapshot must not reference WAITING_REVIEW.
    const instance = await createFreeLowerInstance(request, headers, templateId);
    const snapshot = await (await request.get(`${PROSTH}/instances/${instance.id}/snapshot`, { headers })).json();
    expect(JSON.stringify(snapshot)).not.toContain('WAITING_REVIEW');

    await terminateInstance(request, headers, instance.id);
  });

  test('get gate-decisions returns an empty list (no gates on TP-LL-02)', async ({ request }) => {
    const headers = headersFor(prosthetistToken);
    const templateId = await findTemplateByIdName(request, headers, 'TP-LL-02');
    const instance = await createFreeLowerInstance(request, headers, templateId);

    const gateDecisions = await (await request.get(`${PROSTH}/instances/${instance.id}/gate-decisions`, { headers })).json();
    expect(gateDecisions).toHaveLength(0);

    await terminateInstance(request, headers, instance.id);
  });

  test('TP-UL-01 regression — linear new-to-completed with no gate degradation', async ({ request }) => {
    const headers = headersFor(prosthetistToken);
    const templateId = await findTemplateByIdName(request, headers, 'TP-UL-01');
    const orderId = await findOrderIdByProductType(request, headers, 'UPPER_LIMB', UPPER_ORDER);
    const instance = await createFreeInstanceOnOrder(request, headers, orderId, templateId);

    // p1 owns its own instance; walk the linear flow — it must never touch WAITING_REVIEW.
    await request.post(`${PROSTH}/instances/${instance.id}/start`, { headers });
    await completeToCompleted(request, headers, instance.id);
    expect(await instanceStatus(request, headers, instance.id)).toBe('COMPLETED');
  });
});
