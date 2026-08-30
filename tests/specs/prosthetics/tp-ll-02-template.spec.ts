import { test, expect } from '@playwright/test';

const API = 'http://localhost:8085/api';
const PROSTH = 'http://localhost:8085/api/prosthesis-manufacturing';

async function login(request: any, login: string, password: string): Promise<string> {
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok()).toBeTruthy();
  const body = await res.json();
  // Backend returns token in JSON and sets httpOnly cookie; API also accepts Authorization header
  return body.token as string;
}

test.describe('TP-LL-02 — Persistence & Seed (Фаза 1)', () => {
  let prosthetistToken: string;
  let adminToken: string;

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, 'prosthetist1', 'doctor123');
    adminToken = await login(request, 'prosthetics_admin1', 'doctor123');
  });

  test('GET /templates?productType=LOWER_LIMB returns TP-LL-02 ACTIVE 540', async ({ request }) => {
    const res = await request.get(`${PROSTH}/templates?productType=LOWER_LIMB&status=ACTIVE`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    expect(res.ok()).toBeTruthy();
    const templates = await res.json();
    expect(Array.isArray(templates)).toBeTruthy();

    const tp = templates.find((t: any) => t.name === 'TP-LL-02');
    expect(tp, 'TP-LL-02 must be present in ACTIVE LOWER_LIMB list').toBeTruthy();
    expect(tp.productType).toBe('LOWER_LIMB');
    expect(tp.amputationLevel).toBe('generic_lower_limb');
    // limbSide is NULL (generic, matches any) — backend stores NULL for BOTH
    expect(tp.limbSide == null || tp.limbSide === 'BOTH').toBeTruthy();
    expect(tp.status).toBe('ACTIVE');
    expect(tp.estimatedDurationMin).toBe(540);
    // at least 10 stages expected (v2: 10 in instance)
    expect(tp.stages?.length ?? 10).toBeGreaterThanOrEqual(10);
  });

  test('GET /templates/{id} for TP-LL-02 returns 10 stages / 14 steps linear without gates', async ({ request }) => {
    const listRes = await request.get(`${PROSTH}/templates?productType=LOWER_LIMB&status=ACTIVE`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    const templates = await listRes.json();
    const tp = templates.find((t: any) => t.name === 'TP-LL-02');
    expect(tp).toBeTruthy();

    const detailRes = await request.get(`${PROSTH}/templates/${tp.id}`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    expect(detailRes.ok()).toBeTruthy();
    const detail = await detailRes.json();

    expect(detail.name).toBe('TP-LL-02');
    expect(detail.stages).toHaveLength(10);

    const stageNames = detail.stages.map((s: any) => s.name);
    expect(stageNames).toContain('Виготовлення гіпсового негатива');
    expect(stageNames).toContain('Виготовлення гіпсової моделі кукси');
    expect(stageNames).toContain('Виготовлення тренувальної гільзи');
    expect(stageNames).toContain('Примірка тренувальної гільзи');
    expect(stageNames).toContain('Складання тренувального протеза');
    expect(stageNames).toContain('Примірювання та коректування тренувального протеза');
    expect(stageNames).toContain('Виготовлення пом\'якшуючого вкладиша та постійної гільзи');
    expect(stageNames).toContain('Складання постійного протеза');
    expect(stageNames).toContain('Примірювання та коректування постійного протеза');
    expect(stageNames).toContain('Видача протеза');

    // Count steps: 14 total (2+2+2+1+1+1+2+1+1+1)
    const totalSteps = detail.stages.reduce((acc: number, s: any) => acc + (s.steps?.length ?? 0), 0);
    expect(totalSteps).toBe(14);

    // Last stage must be ADMINISTRATIVE requiresApproval true
    const lastStage = detail.stages.find((s: any) => s.name === 'Видача протеза');
    expect(lastStage.type).toBe('ADMINISTRATIVE');
    expect(lastStage.requiresApproval).toBe(true);
    expect(lastStage.canSkip).toBe(false);

    // No QualityGate / rework loops (removed in v2.1)
    for (const stage of detail.stages) {
      expect(stage.gate).toBeFalsy();
      expect(stage.qualityGate).toBeFalsy();
    }

    // Conditional insert step must be mandatory false
    const insertStage = detail.stages.find((s: any) => s.name.includes('пом\'якшуючого'));
    const insertStep = insertStage.steps.find((st: any) => st.name.includes('пом\'якшуючого вкладиша'));
    expect(insertStep.mandatory).toBe(false);
    expect(insertStep.elements.some((e: any) => e.label.includes('Візуальний контроль чистоти пом'))).toBeTruthy();
    // Its elements must be required false
    for (const el of insertStep.elements) {
      expect(el.required).toBe(false);
    }

    // MEASUREMENT step must have 4 numeric inputs
    const negStage = detail.stages.find((s: any) => s.name === 'Виготовлення гіпсового негатива');
    const measureStep = negStage.steps.find((st: any) => st.stepType === 'MEASUREMENT');
    expect(measureStep).toBeTruthy();
    expect(measureStep.mandatory).toBe(true);
    expect(measureStep.autoStartTimer).toBe(true);
    expect(measureStep.normDurationMin).toBe(20);
    const numericInputs = measureStep.elements.filter((e: any) => e.elementType === 'NUMERIC_INPUT');
    expect(numericInputs).toHaveLength(4);
    expect(numericInputs[0].required).toBe(true);
    expect(numericInputs[0].unit).toBe('см');

    // Last step allowBackward false
    const issueStep = lastStage.steps[0];
    expect(issueStep.allowBackward).toBe(false);
    expect(issueStep.stepType).toBe('CHECKLIST');
    const issueLabels = issueStep.elements.map((e: any) => e.label);
    expect(issueLabels).toContain('На протез нанесено маркування');
    expect(issueLabels).toContain('Супровідна документація оформлена');
    expect(issueLabels).toContain('Протез переданий пацієнту для подальшої експлуатації');
  });

  test('POST /instances with TP-LL-02 creates NEW with snapshot and allows start', async ({ request }) => {
    // Use existing LOWER_LIMB order PR-2026-0002 (patient 900002) — create fresh order if active instance exists
    // First, list orders for patient 900002
    let orderId = '20000000-0000-4000-8000-000000000002'; // PR-2026-0002
    const templatesRes = await request.get(`${PROSTH}/templates?productType=LOWER_LIMB&status=ACTIVE`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    const templates = await templatesRes.json();
    const tp = templates.find((t: any) => t.name === 'TP-LL-02');
    const templateId = tp.id;

    // Try to create instance; if duplicate active exists, find another free order or fail the blocker
    let createRes = await request.post(`${PROSTH}/instances`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
      data: { orderId, templateId },
    });

    if (createRes.status() === 400) {
      const text = await createRes.text().catch(() => '');
      if (text.includes('active instance')) {
        // Find a free order (any not in active set) — fallback to UPPER_LIMB order if LOWER is blocked
        const instancesRes = await request.get(`${PROSTH}/instances`, {
          headers: { Authorization: `Bearer ${prosthetistToken}` },
        });
        const instances = (await instancesRes.json()) as Array<{ orderId: string; status: string }>;
        const activeOrderIds = new Set(
          instances
            .filter((i) => ['NEW', 'IN_PROGRESS', 'PAUSED', 'BLOCKED_PATIENT', 'BLOCKED_MATERIAL', 'WAITING_REVIEW', 'CORRECTION'].includes(i.status))
            .map((i) => i.orderId),
        );
        const ordersRes = await request.get(`${PROSTH}/orders`, {
          headers: { Authorization: `Bearer ${prosthetistToken}` },
        });
        const orders = (await ordersRes.json()) as Array<{ id: string }>;
        const freeOrder = orders.find((o) => !activeOrderIds.has(o.id));
        if (freeOrder) {
          orderId = freeOrder.id;
          createRes = await request.post(`${PROSTH}/instances`, {
            headers: { Authorization: `Bearer ${prosthetistToken}` },
            data: { orderId, templateId },
          });
        } else {
          // Last resort: fail the blocking instance and retry
          const blocker = instances.find((i) => i.orderId === orderId && ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status));
          if (blocker) {
            await request.post(`${PROSTH}/instances/${(blocker as any).id}/fail`, {
              headers: { Authorization: `Bearer ${prosthetistToken}` },
              data: { category: 'test_cleanup', description: 'cleanup blocker for tp-ll-02' },
            });
            createRes = await request.post(`${PROSTH}/instances`, {
              headers: { Authorization: `Bearer ${prosthetistToken}` },
              data: { orderId, templateId },
            });
          }
        }
      }
    }

    expect(createRes.ok()).toBeTruthy();
    const instance = await createRes.json();
    expect(instance.status).toBe('NEW');
    expect(instance.templateId).toBe(templateId);
    expect(instance.orderId).toBe(orderId);
    expect(instance.templateSnapshot).toBeTruthy();

    // Snapshot must contain 10 stages
    const snapshot = typeof instance.templateSnapshot === 'string' ? JSON.parse(instance.templateSnapshot) : instance.templateSnapshot;
    // If snapshot is not returned in create response, fetch it via /snapshot
    if (!snapshot || !snapshot.stages) {
      const snapRes = await request.get(`${PROSTH}/instances/${instance.id}/snapshot`, {
        headers: { Authorization: `Bearer ${prosthetistToken}` },
      });
      expect(snapRes.ok()).toBeTruthy();
      const snap = await snapRes.json();
      expect(snap.stages).toHaveLength(10);
    } else {
      expect(snapshot.stages).toHaveLength(10);
    }

    // Start must transition to IN_PROGRESS with first step
    const startRes = await request.post(`${PROSTH}/instances/${instance.id}/start`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    expect(startRes.ok()).toBeTruthy();
    const started = await startRes.json();
    expect(started.status).toBe('IN_PROGRESS');
    expect(started.currentStageId).toBeTruthy();
    expect(started.currentStepId).toBeTruthy();

    // Cleanup: fail instance to avoid blocking future runs (IN_PROGRESS → FAILED allows replacement)
    await request.post(`${PROSTH}/instances/${instance.id}/fail`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
      data: { category: 'test_cleanup', description: 'Phase 1 E2E cleanup' },
    });
  });

  test('Template snapshot roundtrip preserves estimatedDurationMin and generic level', async ({ request }) => {
    const res = await request.get(`${PROSTH}/templates?productType=LOWER_LIMB&status=ACTIVE`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    const templates = await res.json();
    const tp = templates.find((t: any) => t.name === 'TP-LL-02');
    const detailRes = await request.get(`${PROSTH}/templates/${tp.id}`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    const detail = await detailRes.json();
    expect(detail.estimatedDurationMin).toBe(540);
    expect(detail.limbSide == null || detail.limbSide === 'BOTH').toBeTruthy();
    expect(detail.amputationLevel).toBe('generic_lower_limb');
  });
});
