import { test, expect, type APIRequestContext } from '@playwright/test';
import { completeInstanceViaApi } from '../../helpers/prosthetics-flow';

// Phase 4 tablet pass (issue #178): ProcessLayout shows a collapsed icon rail
// in the tablet band (640–1023px); labels return at ≥1024px. The flow instance
// is created via API and driven to COMPLETED in afterAll — same pattern as
// mobile-wizard-smoke, so the "new process" review screen stays unblocked.

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

async function createInstance(request: APIRequestContext): Promise<string> {
  const res = await request.post(AUTH, {
    data: { login: 'prosthetist1', password: 'doctor123' },
  });
  if (!res.ok()) {
    throw new Error(`Login failed: HTTP ${res.status()}`);
  }
  const headers = { Authorization: `Bearer ${(await res.json()).token}` };

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
  // TP-LL-02 is also ACTIVE since Фаза 1 — ensure tablet smoke still uses TP-UL-01
  const template =
    (templates as Array<{ id: string; status: string; prosthesisType: string; name?: string }>).find(
      (t) => (t as any).name === 'TP-UL-01' && t.status === 'ACTIVE',
    ) ?? templates.find((t) => t.status === 'ACTIVE');
  if (!template) {
    throw new Error('No ACTIVE flow template found');
  }

  let order =
    orders.find((o) => o.prosthesisType === (template as any).prosthesisType && !activeOrderIds.has(o.id)) ??
    orders.find((o) => !activeOrderIds.has(o.id));
  if (!order) {
    const blocker = instances
      .filter((i) => ACTIVE_DUPLICATE_STATUSES.includes(i.status))
      .sort((a, b) => (a as any).createdAt?.localeCompare((b as any).createdAt ?? '') ?? 0)[0];
    if (blocker) {
      await request.post(`${BASE}/instances/${blocker.id}/fail`, {
        headers,
        data: { category: 'test_cleanup', description: 'responsive tablet: free order' },
      });
      const refreshedOrders = (await (await request.get(`${BASE}/orders`, { headers })).json()) as Array<{
        id: string;
        prosthesisType: string;
      }>;
      const refreshedInstances = (await (await request.get(`${BASE}/instances`, { headers })).json()) as Array<{
        id: string;
        status: string;
        orderId: string;
      }>;
      const refreshedActive = new Set(
        refreshedInstances.filter((i) => ACTIVE_DUPLICATE_STATUSES.includes(i.status)).map((i) => i.orderId),
      );
      order =
        refreshedOrders.find((o) => o.prosthesisType === (template as any).prosthesisType && !refreshedActive.has(o.id)) ??
        refreshedOrders.find((o) => !refreshedActive.has(o.id));
    }
    if (!order) {
      throw new Error('No order free of an active flow instance (even after cleanup)');
    }
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

test.describe.configure({ mode: 'serial' });

test.describe('tablet prosthetics at 768 — prosthetist', () => {
  test.use({ storageState: '.auth/prosthetist.json' });

  let instanceId = '';

  test.beforeAll(async ({ request }) => {
    instanceId = await createInstance(request);
  });

  test.afterAll(async ({ request }) => {
    if (instanceId) {
      await completeInstanceViaApi(request, instanceId);
    }
  });

  test('dashboard renders without horizontal overflow at 768', async ({ page }) => {
    await page.goto('/prosthetics');
    await expect(page.getByRole('button', { name: 'Новий процес' })).toBeVisible({
      timeout: 10000,
    });
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth))
      .toBeLessThanOrEqual(1);
  });

  test('process rail collapses to icons at 768 and still navigates', async ({ page }) => {
    await page.goto(`/prosthetics/process/${instanceId}`);
    const rail = page.locator('nav').filter({ has: page.locator('a[href$="/history"]') }).first();
    await expect(rail).toBeVisible({ timeout: 10000 });

    // w-14 (56px) mid-band vs lg:w-56 (224px) on desktop
    const width = (await rail.boundingBox())!.width;
    expect(width).toBeLessThanOrEqual(80);

    // Labels are hidden below lg…
    await expect(rail.getByText('Огляд')).toBeHidden();
    await expect(rail.getByText('Історія')).toBeHidden();

    // …but the links stay reachable via their icons.
    await rail.locator('a[href$="/history"]').click();
    await expect(page).toHaveURL(new RegExp(`/prosthetics/process/${instanceId}/history$`));
  });

  test('wizard opens at 768 without horizontal overflow', async ({ page }) => {
    await page.goto(`/prosthetics/process/${instanceId}/wizard`);
    await expect(page.getByText('Зняття мірок та виготовлення гіпсового негатива').first()).toBeVisible({
      timeout: 15000,
    });
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth))
      .toBeLessThanOrEqual(1);
  });
});
