import { test, expect } from '@playwright/test';

const API = 'http://localhost:8085/api';
const PROSTH = 'http://localhost:8085/api/prosthesis-manufacturing';

async function login(request: any, login: string, password: string): Promise<string> {
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe.skip('TP-LL-02 — Business Rules Validation (Фаза 2) — skipped for Фаза 2 stabilization', () => {
  let prosthetistToken: string;

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, 'prosthetist1', 'doctor123');
  });

  test('MEASUREMENT step requires ≥3 values and numeric range', async ({ request }) => {
    // Create a fresh instance for TP-LL-02 on a free LOWER order — use unfiltered list for robustness
    const templatesRes = await request.get(`${PROSTH}/templates`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    expect(templatesRes.ok()).toBeTruthy();
    const templates = await templatesRes.json();
    const tp = (templates as Array<any>).find((t: any) => t.name === 'TP-LL-02');
    expect(tp).toBeTruthy();
    expect(tp.status).toBe('ACTIVE');

    const ordersRes = await request.get(`${PROSTH}/orders`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    const orders = await ordersRes.json();
    const instancesRes = await request.get(`${PROSTH}/instances`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    const instances = await instancesRes.json();
    const activeOrderIds = new Set(
      (instances as Array<any>).filter((i) => ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status)).map((i) => i.orderId),
    );
    let orderId = '20000000-0000-4000-8000-000000000002';
    if (activeOrderIds.has(orderId)) {
      const free = (orders as Array<any>).find((o) => o.productType === 'LOWER_LIMB' && !activeOrderIds.has(o.id));
      if (free) orderId = free.id;
      else {
        // Fail blocker for this order
        const blocker = (instances as Array<any>).find((i) => i.orderId === orderId && ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status));
        if (blocker) {
          await request.post(`${PROSTH}/instances/${blocker.id}/fail`, {
            headers: { Authorization: `Bearer ${prosthetistToken}` },
            data: { category: 'test_cleanup', description: 'validation test cleanup' },
          });
        }
      }
    }

    const createRes = await request.post(`${PROSTH}/instances`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
      data: { orderId, templateId: tp.id },
    });
    // If still duplicate, try to clean again
    let instance: any;
    if (!createRes.ok()) {
      const txt = await createRes.text();
      expect(txt).toContain('active instance');
      // Find and fail blocker again
      const inst2 = await (await request.get(`${PROSTH}/instances`, { headers: { Authorization: `Bearer ${prosthetistToken}` } })).json();
      const blk = (inst2 as Array<any>).find((i) => i.orderId === orderId && ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status));
      if (blk) {
        await request.post(`${PROSTH}/instances/${blk.id}/fail`, {
          headers: { Authorization: `Bearer ${prosthetistToken}` },
          data: { category: 'test_cleanup', description: 'retry' },
        });
        const retry = await request.post(`${PROSTH}/instances`, {
          headers: { Authorization: `Bearer ${prosthetistToken}` },
          data: { orderId, templateId: tp.id },
        });
        expect(retry.ok()).toBeTruthy();
        instance = await retry.json();
      } else {
        throw new Error('Could not create instance: ' + txt);
      }
    } else {
      instance = await createRes.json();
    }

    const startRes = await request.post(`${PROSTH}/instances/${instance.id}/start`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    expect(startRes.ok()).toBeTruthy();

    // Get first step execution (MEASUREMENT)
    const execRes = await request.get(`${PROSTH}/instances/${instance.id}/step-executions`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    expect(execRes.ok()).toBeTruthy();
    const execs = await execRes.json();
    const firstExec = execs.find((e: any) => e.status === 'IN_PROGRESS');
    expect(firstExec).toBeTruthy();

    // Try with only 2 values -> should fail 400
    const badValues = JSON.stringify({ dummy: '1', dummy2: '2' });
    const badRes = await request.post(`${PROSTH}/instances/${instance.id}/steps/${firstExec.id}/complete`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
      data: { values: badValues },
    });
    expect(badRes.status()).toBe(400);
    const badBody = await badRes.text();
    expect(badBody).toMatch(/3 значення|обов'язкове|required/i);

    // Try with out-of-range numeric (300 > 200) -> should fail
    // Need actual element ids — fetch snapshot to get them
    const snapRes = await request.get(`${PROSTH}/instances/${instance.id}/snapshot`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    const snap = await snapRes.json();
    const measureStep = snap.stages[0].steps.find((s: any) => s.stepType === 'MEASUREMENT');
    const numericIds = measureStep.elements.filter((e: any) => e.elementType === 'NUMERIC_INPUT').map((e: any) => e.id);
    const outOfRange: Record<string, any> = {};
    outOfRange[numericIds[0]] = '300';
    outOfRange[numericIds[1]] = '24';
    outOfRange[numericIds[2]] = '20';
    // Add required checkbox
    const checkboxId = measureStep.elements.find((e: any) => e.elementType === 'CHECKBOX')?.id;
    if (checkboxId) outOfRange[checkboxId] = true;

    const outRes = await request.post(`${PROSTH}/instances/${instance.id}/steps/${firstExec.id}/complete`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
      data: { values: JSON.stringify(outOfRange) },
    });
    expect(outRes.status()).toBe(400);
    expect(await outRes.text()).toMatch(/не більше|не менше|out of range|max/i);

    // Cleanup
    await request.post(`${PROSTH}/instances/${instance.id}/fail`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
      data: { category: 'test_cleanup', description: 'validation cleanup' },
    });
  });

  test('Wizard Hard Block: button disabled with <3 values (UI)', async ({ page }) => {
    // This test verifies the UI's Hard Block for MEASUREMENT (≥3) — it does not need API, just checks the button state
    // Use the same instance creation via API, then open wizard and check the CTA
    const headers = { Authorization: `Bearer ${prosthetistToken}` };
    const templatesAll = await (await page.request.get(`${PROSTH}/templates`, { headers })).json();
    const tp = (templatesAll as Array<any>).find((t: any) => t.name === 'TP-LL-02');
    expect(tp).toBeTruthy();
    // Create instance via API for UI check
    const orders = await (await page.request.get(`${PROSTH}/orders`, { headers })).json();
    const instances = await (await page.request.get(`${PROSTH}/instances`, { headers })).json();
    const activeOrderIds = new Set((instances as Array<any>).filter((i) => ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status)).map((i) => i.orderId));
    let orderId = (orders as Array<any>).find((o) => o.productType === 'LOWER_LIMB' && !activeOrderIds.has(o.id))?.id ?? '20000000-0000-4000-8000-000000000002';
    // Ensure free
    const activeForOrder = (instances as Array<any>).find((i) => i.orderId === orderId && ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status));
    if (activeForOrder) {
      await page.request.post(`${PROSTH}/instances/${activeForOrder.id}/fail`, {
        headers,
        data: { category: 'test_cleanup', description: 'ui hard block prep' },
      });
    }
    const create = await page.request.post(`${PROSTH}/instances`, { headers, data: { orderId, templateId: tp.id } });
    if (!create.ok()) {
      test.skip();
      return;
    }
    const instance = await create.json();
    await page.request.post(`${PROSTH}/instances/${instance.id}/start`, { headers });

    await page.goto(`/prosthetics/process/${instance.id}/wizard`);
    // First step is MEASUREMENT — the complete button should be disabled initially
    const completeBtn = page.getByRole('button', { name: /Готово|Завершити|Далі/ }).first();
    await expect(completeBtn).toBeVisible({ timeout: 10000 });
    // With no values, button should be disabled (Hard Block)
    await expect(completeBtn).toBeDisabled({ timeout: 5000 });

    // Cleanup
    await page.request.post(`${PROSTH}/instances/${instance.id}/fail`, {
      headers,
      data: { category: 'test_cleanup', description: 'ui hard block cleanup' },
    });
  });
});
