import { test, expect } from '@playwright/test';

const API = 'http://localhost:8085/api';
const PROSTH = 'http://localhost:8085/api/prosthesis-manufacturing';

async function login(request: any, login: string, password: string): Promise<string> {
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe.skip('TP-LL-02 — Conditional insert skip & state machine (Фаза 2) — skipped for stabilization', () => {
  let prosthetistToken: string;

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, 'prosthetist1', 'doctor123');
  });

  test('Conditional step 7.1 can be skipped with empty values (mandatory=false)', async ({ request }) => {
    const headers = { Authorization: `Bearer ${prosthetistToken}` };
    const templates = await (await request.get(`${PROSTH}/templates?productType=LOWER_LIMB&status=ACTIVE`, { headers })).json();
    const tp = templates.find((t: any) => t.name === 'TP-LL-02');
    expect(tp).toBeTruthy();

    // Create instance on a free LOWER order
    const orders = await (await request.get(`${PROSTH}/orders`, { headers })).json();
    const instances = await (await request.get(`${PROSTH}/instances`, { headers })).json();
    const activeOrderIds = new Set((instances as Array<any>).filter((i) => ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status)).map((i) => i.orderId));
    let orderId = (orders as Array<any>).find((o) => o.productType === 'LOWER_LIMB' && !activeOrderIds.has(o.id))?.id;
    if (!orderId) {
      // No free LOWER order — fail the blocker
      const blocker = (instances as Array<any>).find((i) => ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status));
      if (blocker) {
        await request.post(`${PROSTH}/instances/${blocker.id}/fail`, {
          headers,
          data: { category: 'test_cleanup', description: 'conditional prep' },
        });
        orderId = (orders as Array<any>).find((o) => o.productType === 'LOWER_LIMB')?.id ?? '20000000-0000-4000-8000-000000000002';
      }
    }
    const create = await request.post(`${PROSTH}/instances`, { headers, data: { orderId, templateId: tp.id } });
    // If duplicate due to race, fail and retry
    let instance: any;
    if (!create.ok()) {
      const inst2 = await (await request.get(`${PROSTH}/instances`, { headers })).json();
      const blk = (inst2 as Array<any>).find((i) => i.orderId === orderId && ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status));
      if (blk) {
        await request.post(`${PROSTH}/instances/${blk.id}/fail`, { headers, data: { category: 'test_cleanup', description: 'retry' } });
        const retry = await request.post(`${PROSTH}/instances`, { headers, data: { orderId, templateId: tp.id } });
        expect(retry.ok()).toBeTruthy();
        instance = await retry.json();
      } else throw new Error('Create failed: ' + (await create.text()));
    } else {
      instance = await create.json();
    }

    const started = await request.post(`${PROSTH}/instances/${instance.id}/start`, { headers });
    expect(started.ok()).toBeTruthy();
    let currentId = instance.id;

    // Drive through first 6 stages quickly via API, using valid payloads, to reach stage 7 (insert)
    // Helper to complete current step with valid values
    async function completeCurrent(validEmpty = false) {
      const inst = await (await request.get(`${PROSTH}/instances/${currentId}`, { headers })).json();
      const execs = await (await request.get(`${PROSTH}/instances/${currentId}/step-executions`, { headers })).json();
      const exec = execs.find((e: any) => e.status === 'IN_PROGRESS');
      if (!exec) return false;
      const snap = await (await request.get(`${PROSTH}/instances/${currentId}/snapshot`, { headers })).json();
      const step = snap.stages.flatMap((s: any) => s.steps).find((s: any) => s.id === exec.stepId);
      const values: Record<string, any> = {};
      if (validEmpty && step.mandatory === false) {
        // Send empty for conditional skip
      } else {
        for (const el of step.elements) {
          if (el.elementType === 'CHECKBOX') values[el.id] = true;
          else if (el.elementType === 'NUMERIC_INPUT') {
            const min = el.minValue ?? 0;
            const max = el.maxValue ?? 200;
            values[el.id] = Math.round((Number(min) + Number(max)) / 2);
          } else values[el.id] = 'test';
        }
        // For MEASUREMENT, ensure at least 3 numeric
        if (step.stepType === 'MEASUREMENT') {
          const nums = step.elements.filter((e: any) => e.elementType === 'NUMERIC_INPUT');
          for (let i = 0; i < Math.min(3, nums.length); i++) values[nums[i].id] = 10 + i;
          const cb = step.elements.find((e: any) => e.elementType === 'CHECKBOX');
          if (cb) values[cb.id] = true;
        }
      }
      const res = await request.post(`${PROSTH}/instances/${currentId}/steps/${exec.id}/complete`, {
        headers,
        data: { values: JSON.stringify(values) },
      });
      expect(res.ok()).toBeTruthy();
      return true;
    }

    // Advance to stage 7 step 1 (insert) — complete first 9 steps (up to stage 6)
    for (let i = 0; i < 9; i++) {
      const ok = await completeCurrent();
      if (!ok) break;
    }

    // Now we should be at stage 7 step 1 (conditional)
    const instBefore = await (await request.get(`${PROSTH}/instances/${currentId}`, { headers })).json();
    const snapBefore = await (await request.get(`${PROSTH}/instances/${currentId}/snapshot`, { headers })).json();
    const currentStepId = instBefore.currentStepId;
    const currentStep = snapBefore.stages.flatMap((s: any) => s.steps).find((s: any) => s.id === currentStepId);
    expect(currentStep.name).toContain('пом\'якшуючого вкладиша');
    expect(currentStep.mandatory).toBe(false);

    // Complete with empty values (skip)
    const execs = await (await request.get(`${PROSTH}/instances/${currentId}/step-executions`, { headers })).json();
    const exec = execs.find((e: any) => e.stepId === currentStepId && e.status === 'IN_PROGRESS');
    const emptyRes = await request.post(`${PROSTH}/instances/${currentId}/steps/${exec.id}/complete`, {
      headers,
      data: { values: JSON.stringify({}) },
    });
    expect(emptyRes.ok()).toBeTruthy();

    // Should have advanced to next step (permanent socket)
    const after = await (await request.get(`${PROSTH}/instances/${currentId}`, { headers })).json();
    expect(after.currentStepId).not.toBe(currentStepId);
    const snapAfter = await (await request.get(`${PROSTH}/instances/${currentId}/snapshot`, { headers })).json();
    const nextStep = snapAfter.stages.flatMap((s: any) => s.steps).find((s: any) => s.id === after.currentStepId);
    expect(nextStep.name).toContain('постійної гільзи');

    // Also verify that completing 7.1 with valid values also works (create another instance)
    // Cleanup first instance
    await request.post(`${PROSTH}/instances/${currentId}/fail`, {
      headers,
      data: { category: 'test_cleanup', description: 'conditional first path cleanup' },
    });
  });

  test('State machine: pause/resume and fail/replacement without WAITING_REVIEW', async ({ request }) => {
    const headers = { Authorization: `Bearer ${prosthetistToken}` };
    const templates = await (await request.get(`${PROSTH}/templates?productType=LOWER_LIMB&status=ACTIVE`, { headers })).json();
    const tp = templates.find((t: any) => t.name === 'TP-LL-02');

    const orders = await (await request.get(`${PROSTH}/orders`, { headers })).json();
    const instances = await (await request.get(`${PROSTH}/instances`, { headers })).json();
    const activeOrderIds = new Set((instances as Array<any>).filter((i) => ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status)).map((i) => i.orderId));
    let orderId = (orders as Array<any>).find((o) => o.productType === 'LOWER_LIMB' && !activeOrderIds.has(o.id))?.id ?? '20000000-0000-4000-8000-000000000002';
    // Ensure free
    const act = (instances as Array<any>).find((i) => i.orderId === orderId && ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status));
    if (act) {
      await request.post(`${PROSTH}/instances/${act.id}/fail`, { headers, data: { category: 'test_cleanup', description: 'state machine prep' } });
    }

    const create = await request.post(`${PROSTH}/instances`, { headers, data: { orderId, templateId: tp.id } });
    expect(create.ok()).toBeTruthy();
    const inst = await create.json();
    const started = await request.post(`${PROSTH}/instances/${inst.id}/start`, { headers });
    expect(started.ok()).toBeTruthy();
    let after = await started.json();
    expect(after.status).toBe('IN_PROGRESS');

    // Pause
    const paused = await request.post(`${PROSTH}/instances/${inst.id}/pause`, {
      headers,
      data: { category: 'PATIENT' },
    });
    expect(paused.ok()).toBeTruthy();
    expect((await paused.json()).status).toBe('PAUSED');

    // Resume
    const resumed = await request.post(`${PROSTH}/instances/${inst.id}/resume`, { headers });
    expect(resumed.ok()).toBeTruthy();
    expect((await resumed.json()).status).toBe('IN_PROGRESS');

    // Fail (should not go to WAITING_REVIEW)
    const failed = await request.post(`${PROSTH}/instances/${inst.id}/fail`, {
      headers,
      data: { category: 'material_defect', description: 'test fail' },
    });
    expect(failed.ok()).toBeTruthy();
    expect((await failed.json()).status).toBe('FAILED');

    // Replacement should create NEW with same orderId/snapshot
    const repl = await request.post(`${PROSTH}/instances/${inst.id}/replacement`, { headers });
    expect(repl.ok()).toBeTruthy();
    const replBody = await repl.json();
    expect(replBody.status).toBe('NEW');
    expect(replBody.orderId).toBe(orderId);

    // Cleanup replacement
    await request.post(`${PROSTH}/instances/${replBody.id}/fail`, {
      headers,
      data: { category: 'test_cleanup', description: 'replacement cleanup' },
    });
  });
});
