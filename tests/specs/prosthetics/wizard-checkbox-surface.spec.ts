import { expect, test, type APIRequestContext, type Locator, type Page } from '@playwright/test';
import { completeCurrentStepViaApi, completeInstanceViaApi, passPendingGateViaApi } from '../../helpers/prosthetics-flow';

// Wizard checkbox whole-surface clickability: every parent checkbox row in the
// «Операційна карта» wizard (WizardScreen.tsx) must be clickable across its
// whole surface. The rows are <label htmlFor> wrappers, so native label
// activation toggles the control from any point. This spec creates a fresh
// flow instance via API, walks ALL template steps, and on every step that
// renders checkbox rows clicks each row at 5 surface points (4 corners +
// center) asserting the control toggles exactly once per click. Steps without
// checkbox rows are skipped via the API.

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

async function getStatus(request: APIRequestContext, instanceId: string): Promise<string> {
  const token = await login(request);
  const res = await request.get(`${BASE}/instances/${instanceId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok()) {
    throw new Error(`Status fetch failed: HTTP ${res.status()}`);
  }
  return (await res.json()).status;
}

/**
 * Creates a fresh instance from the first order free of an active flow
 * and starts it so the walk sees IN_PROGRESS with step executions present.
 * A failed create (order race) falls through to the next candidate order.
 */
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
  // Prefer TP-UL-01 to keep legacy walk stable after TP-LL-02 ACTIVE was added (Фаза 1)
  const template =
    (templates as Array<{ id: string; status: string; prosthesisType: string; name?: string }>).find(
      (t) => (t as any).name === 'TP-UL-01' && t.status === 'ACTIVE',
    ) ?? templates.find((t) => t.status === 'ACTIVE');
  if (!template) {
    throw new Error('No ACTIVE flow template found');
  }

  const candidates = [
    ...orders.filter(
      (o) => o.prosthesisType === template.prosthesisType && !activeOrderIds.has(o.id),
    ),
    ...orders.filter((o) => !activeOrderIds.has(o.id)),
  ];
  let uniqueCandidates = [...new Map(candidates.map((o) => [o.id, o])).values()];
  if (uniqueCandidates.length === 0) {
    // Both seed orders are blocked (e.g., by TP-LL-02 leftovers) — free the oldest active instance
    const blocker = instances
      .filter((i) => ACTIVE_DUPLICATE_STATUSES.includes(i.status))
      .sort((a, b) => (a as any).createdAt?.localeCompare((b as any).createdAt ?? '') ?? 0)[0];
    if (blocker) {
      await request.post(`${BASE}/instances/${blocker.id}/fail`, {
        headers,
        data: { category: 'other', description: 'wizard surface: free order' },
      });
      const refreshedInstances = (await (await request.get(`${BASE}/instances`, { headers })).json()) as Array<{
        id: string;
        status: string;
        orderId: string;
      }>;
      const refreshedActive = new Set(
        refreshedInstances.filter((i) => ACTIVE_DUPLICATE_STATUSES.includes(i.status)).map((i) => i.orderId),
      );
      uniqueCandidates = [
        ...orders.filter((o) => o.prosthesisType === (template as any).prosthesisType && !refreshedActive.has(o.id)),
        ...orders.filter((o) => !refreshedActive.has(o.id)),
      ].filter((v, i, a) => a.findIndex((x) => x.id === v.id) === i);
    }
    if (uniqueCandidates.length === 0) {
      throw new Error('No order free of an active flow instance (even after cleanup)');
    }
  }

  for (const order of uniqueCandidates) {
    const created = await request.post(`${BASE}/instances`, {
      headers,
      data: { orderId: order.id, templateId: template.id },
    });
    if (created.ok()) {
      const id = ((await created.json()) as { id: string }).id;
      const started = await request.post(`${BASE}/instances/${id}/start`, { headers });
      if (!started.ok()) {
        throw new Error(`Instance start failed: HTTP ${started.status()}: ${await started.text()}`);
      }
      return id;
    }
  }
  throw new Error('Could not create a flow instance on any candidate order');
}

/** Scrolls the row so its whole surface sits clear of the sticky bars. */
async function centerRowInViewport(page: Page, row: Locator): Promise<void> {
  for (let attempt = 0; attempt < 6; attempt++) {
    await row.scrollIntoViewIfNeeded();
    const box = await row.boundingBox();
    const viewport = page.viewportSize();
    if (box && viewport && box.y >= 180 && box.y + box.height <= viewport.height - 110) {
      return;
    }
    await row.evaluate((el) => {
      let parent = el.parentElement;
      while (parent && parent !== document.body) {
        const style = getComputedStyle(parent);
        const scrollable =
          /(auto|scroll)/.test(style.overflowY) && parent.scrollHeight > parent.clientHeight;
        if (scrollable) {
          const rect = el.getBoundingClientRect();
          parent.scrollTop += rect.top + rect.height / 2 - parent.clientHeight / 2;
          return;
        }
        parent = parent.parentElement;
      }
    });
  }
  throw new Error('Could not center the checkbox row in the viewport');
}

/**
 * Clicks the row at a fraction of its width/height and asserts the checkbox
 * toggled exactly once (aria-checked flips from its previous value).
 */
async function clickSurfacePointAndExpectToggle(
  page: Page,
  box: { x: number; y: number; width: number; height: number },
  fx: number,
  fy: number,
  checkbox: Locator,
): Promise<void> {
  const before = await checkbox.getAttribute('aria-checked');
  expect(before, 'checkbox must have an aria-checked state').not.toBeNull();
  await page.mouse.click(box.x + box.width * fx, box.y + box.height * fy);
  await expect
    .poll(async () => checkbox.getAttribute('aria-checked'), {
      timeout: 5000,
      message: `checkbox did not toggle after a click at (${fx}, ${fy})`,
    })
    .not.toBe(before);
}

test.describe('wizard checkbox whole-surface clickability', () => {
  test.describe.configure({ mode: 'serial' });
  test.use({ storageState: '.auth/prosthetist.json' });

  let instanceId = '';

  test('creates a fresh flow instance for the surface walk', async ({ request }) => {
    instanceId = await createInstance(request);
  });

  test('every checkbox row toggles from all 5 surface points on every step', async ({
    page,
    request,
  }) => {
    test.setTimeout(600000);
    expect(instanceId, 'the instance setup test must run first').toBeTruthy();

    const rows = page.locator('label:has([data-slot="checkbox"])');
    let rowsTested = 0;

    for (let iteration = 0; iteration < 40; iteration++) {
      const status = await getStatus(request, instanceId);
      if (status === 'COMPLETED') {
        break;
      }
      if (status === 'WAITING_REVIEW' || status === 'CORRECTION') {
        await passPendingGateViaApi(request, instanceId);
        continue;
      }
      expect(status, 'the walk must stay IN_PROGRESS').toBe('IN_PROGRESS');

      await page.goto(`/prosthetics/process/${instanceId}/wizard`);
      await expect(
        page.getByRole('button', { name: /Готово|Завершити процес/ }).first(),
      ).toBeVisible({ timeout: 15000 });

      const count = await rows.count();
      for (let i = 0; i < count; i++) {
        const row = rows.nth(i);
        const checkbox = row.locator('[data-slot="checkbox"]');
        await centerRowInViewport(page, row);
        const box = await row.boundingBox();
        expect(box, `checkbox row ${i} (iteration ${iteration}) must be measurable`).toBeTruthy();
        rowsTested++;
        for (const [fx, fy] of [
          [0.05, 0.05],
          [0.95, 0.05],
          [0.05, 0.95],
          [0.95, 0.95],
          [0.5, 0.5],
        ] as const) {
          await clickSurfacePointAndExpectToggle(page, box!, fx, fy, checkbox);
        }
      }

      await completeCurrentStepViaApi(request, instanceId);
    }

    expect(rowsTested, 'the template must render at least one checkbox row').toBeGreaterThan(0);
    await expect
      .poll(async () => getStatus(request, instanceId), { timeout: 10000 })
      .toBe('COMPLETED');
  });

  test.afterAll(async ({ request }) => {
    if (instanceId) {
      await completeInstanceViaApi(request, instanceId);
    }
  });
});