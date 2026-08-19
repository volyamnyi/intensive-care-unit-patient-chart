import { test, expect, type APIRequestContext } from '@playwright/test';
import { completeInstanceViaApi } from '../../helpers/prosthetics-flow';
import { WizardExecutionPage } from '../../pages/prosthetics/WizardExecutionPage';

// Responsive UI Phase 6 (issue #165): a prosthetist can complete one full
// wizard stage at 360x740 — start the process, fill the step elements,
// advance past the first stage — with 44px CTAs and no horizontal overflow.
// The flow instance is created via API (seed orders are read-only for the UI)
// and driven to COMPLETED in afterAll so the "new process" review screen is
// not blocked for later projects.

const BASE = 'http://localhost:8085/api/prosthesis-manufacturing';
const AUTH = 'http://localhost:8085/api/auth/login';

// Same list as OrderReviewPage — an order with one of these statuses blocks
// a new process on the review screen and must be avoided.
const ACTIVE_DUPLICATE_STATUSES = [
  'NEW',
  'IN_PROGRESS',
  'PAUSED',
  'BLOCKED_PATIENT',
  'BLOCKED_MATERIAL',
  'WAITING_REVIEW',
  'CORRECTION',
];

async function login(request: APIRequestContext): Promise<string> {
  const res = await request.post(AUTH, {
    data: { login: 'prosthetist1', password: 'doctor123' },
  });
  if (!res.ok()) {
    throw new Error(`Login failed: HTTP ${res.status()}`);
  }
  return (await res.json()).token;
}

async function createInstance(request: APIRequestContext): Promise<string> {
  const token = await login(request);
  const headers = { Authorization: `Bearer ${token}` };

  const orders = (await (
    await request.get(`${BASE}/orders`, { headers })
  ).json()) as Array<{ id: string; prosthesisType: string }>;
  const templates = (await (
    await request.get(`${BASE}/templates`, { headers })
  ).json()) as Array<{ id: string; status: string; prosthesisType: string }>;
  const instances = (await (
    await request.get(`${BASE}/instances`, { headers })
  ).json()) as Array<{ id: string; status: string; orderId: string }>;

  const activeOrderIds = new Set(
    instances
      .filter((i) => ACTIVE_DUPLICATE_STATUSES.includes(i.status))
      .map((i) => i.orderId),
  );
  const template = templates.find((t) => t.status === 'ACTIVE');
  if (!template) {
    throw new Error('No ACTIVE flow template found');
  }

  const order =
    orders.find((o) => o.prosthesisType === template.prosthesisType && !activeOrderIds.has(o.id)) ??
    orders.find((o) => !activeOrderIds.has(o.id));
  if (!order) {
    throw new Error('No order free of an active flow instance');
  }

  const created = await request.post(`${BASE}/instances`, {
    headers,
    data: { orderId: order.id, templateId: template.id },
  });
  if (!created.ok()) {
    throw new Error(`Instance create failed: HTTP ${created.status()}: ${await created.text()}`);
  }
  return (await created.json()).id;
}

test.describe('mobile wizard smoke — prosthetist', () => {
  test.use({
    storageState: '.auth/prosthetist.json',
    viewport: { width: 360, height: 740 },
  });

  let instanceId: string;

  test.beforeAll(async ({ request }) => {
    instanceId = await createInstance(request);
  });

  test.afterAll(async ({ request }) => {
    if (instanceId) {
      await completeInstanceViaApi(request, instanceId);
    }
  });

  test('setup pages render without horizontal overflow', async ({ page }) => {
    await page.goto('/prosthetics/new/select-patient');
    await expect(page.getByRole('button', { name: 'Далі' })).toBeVisible();
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth))
      .toBeLessThanOrEqual(1);
  });

  test('starts the process and completes one stage at 360px', async ({ page }) => {
    await page.goto(`/prosthetics/process/${instanceId}/wizard`);

    const start = page.getByRole('button', { name: /Розпочати процес/ });
    await expect(start).toBeVisible();
    const startBox = await start.boundingBox();
    expect(startBox!.height).toBeGreaterThanOrEqual(44);
    await start.click();

    const stage1 = page.getByText('Зняття мірок та виготовлення гіпсового негатива');
    await expect(stage1).toBeVisible();

    const wizard = new WizardExecutionPage(page);
    const completeButton = wizard.completeStepButton;
    await expect(completeButton).toBeVisible();
    const ctaBox = await completeButton.boundingBox();
    expect(ctaBox!.height).toBeGreaterThanOrEqual(44);

    for (let i = 0; i < 5; i++) {
      const advanced = await wizard.executeCurrentStep();
      if (!advanced) break;
      const reachedStage2 = await page
        .getByText('Виготовлення гіпсового позитива')
        .isVisible({ timeout: 1000 })
        .catch(() => false);
      if (reachedStage2) break;
    }

    await expect(page.getByText('Виготовлення гіпсового позитива')).toBeVisible({
      timeout: 15000,
    });
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth))
      .toBeLessThanOrEqual(1);
  });
});