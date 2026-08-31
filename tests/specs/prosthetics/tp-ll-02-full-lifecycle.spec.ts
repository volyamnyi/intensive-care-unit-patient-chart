import { test, expect } from '@playwright/test';
import {
  PROSTH,
  API,
  login,
  headersFor,
  findTemplateByIdName,
  createFreeLowerInstance,
  completeOneStep,
  completeToCompleted,
  instanceStatus,
} from '../../helpers/tp-ll-02-flow';

test.describe('TP-LL-02 — Full Lifecycle (Фаза 5)', () => {
  let prosthetistToken: string;
  let adminToken: string;

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, 'prosthetist1', 'doctor123');
    adminToken = await login(request, 'admin', 'admin123');
  });

  test('NEW → COMPLETED → PDF + audit of FlowInstance/StepExecution mutations', async ({ request }) => {
    const headers = headersFor(prosthetistToken);
    const templateId = await findTemplateByIdName(request, headers, 'TP-LL-02');

    const instance = await createFreeLowerInstance(request, headers, templateId);
    expect(instance.status).toBe('NEW');
    expect(instance.templateName).toBe('TP-LL-02');
    const instanceId = instance.id;

    const startRes = await request.post(`${PROSTH}/instances/${instanceId}/start`, { headers });
    expect(startRes.ok()).toBeTruthy();
    expect((await startRes.json()).status).toBe('IN_PROGRESS');

    // Complete one step (capturing its execution for the audit assertion), then drive to COMPLETED.
    const lastStep = await completeOneStep(request, headers, instanceId);
    await completeToCompleted(request, headers, instanceId);
    expect(await instanceStatus(request, headers, instanceId)).toBe('COMPLETED');

    // PDF export renders for the finished process.
    const pdfRes = await request.get(`${PROSTH}/instances/${instanceId}/pdf`, { headers });
    expect(pdfRes.ok()).toBeTruthy();
    expect(pdfRes.headers()['content-type']).toContain('application/pdf');
    expect((await pdfRes.body()).length).toBeGreaterThan(0);

    // Admin (AUDIT_ACCESS) can read the instance / step audit trails.
    const instanceAudit = await (await request.get(`${API}/audit?entity=FlowInstance&entityId=${instanceId}`, {
      headers: headersFor(adminToken),
    }).json()) as any;
    const instanceActions = instanceAudit.content.map((e: any) => e.action);
    expect(instanceActions).toContain('CREATE');
    expect(instanceActions).toContain('START');

    const stepAudit = await (await request.get(`${API}/audit?entity=StepExecution&entityId=${lastStep.id}`, {
      headers: headersFor(adminToken),
    }).json()) as any;
    expect(stepAudit.content.map((e: any) => e.action)).toContain('COMPLETE');
  });
});
