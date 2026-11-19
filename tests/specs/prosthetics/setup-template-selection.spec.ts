import { test, expect, type APIRequestContext, type Page } from '@playwright/test';
import { testUser } from '../../helpers/test-users';
import { PROSTH, headersFor, login, terminateInstance } from '../../helpers/tp-ll-02-flow';

/**
 * #454 ([05/08]) — integrated technology-route selection on the order review.
 *
 * The separate «Вибір технологічного маршруту» screen was removed (#449/#453):
 * step 3 of 3 resolves the candidates and starts the process in one place.
 *
 * Contract under test (decision #449):
 *  - exactly one *exact* match → auto-selected, «Старт» enabled;
 *  - several exact matches   → nothing pre-selected, «Оберіть маршрут» disabled
 *                              until the user picks one;
 *  - only product-type matches (no exact level) → manual choice only;
 *  - none                    → creation blocked («Немає доступних маршрутів»);
 *  - an existing active process for the order blocks creation (duplicate guard);
 *  - the created instance carries the *chosen* templateId and an immutable
 *    snapshot of it, confirmed through the API rather than the DOM alone.
 *
 * Backend contract is unchanged (#453 moved the choice into the client;
 * candidates still come from `GET /templates`), therefore no new backend
 * integration test is added — `TpLl02SetupFlowIntegrationTest` and the
 * duplicate-order coverage stay as they are.
 *
 * Data isolation: the setup order is provisioned from MIS document 6001 of
 * patient 13373 (deterministic number `MIS-13373-6001`) and is freed in every
 * `finally` by driving the created instance to FAILED. Temporary ACTIVE
 * templates live only for the duration of one test and are archived afterwards;
 * leftovers from a crashed run are archived in `beforeAll` so the candidate
 * count can never drift.
 */

const PATIENT_ID = '13373';
const UPPER_DOC_ID = 6001;
// Seed order «ПВ-26-0413» — the only seeded order with a non-null
// amputationLevel, which is what makes the exact-match branch reachable.
const SEED_ORDER_WITH_LEVEL = '20000000-0000-4000-8000-000000000001';
const TP_UL_NAME = 'TP-UL-01';
const TEMP_NAME_PREFIXES = ['E2E-AUTO-', 'E2E-MULTI-'];

let prosthetistHeaders: Record<string, string>;
let adminHeaders: Record<string, string>;
let orderId: string;
let tpUlId: string;

interface FlowInstance {
  id: string;
  orderId: string;
  templateId: string;
  status: string;
}

interface FlowTemplate {
  id: string;
  name: string;
  status: string;
}

async function listInstances(request: APIRequestContext): Promise<FlowInstance[]> {
  const res = await request.get(`${PROSTH}/instances`, { headers: prosthetistHeaders });
  expect(res.ok()).toBeTruthy();
  return (await res.json()) as FlowInstance[];
}

async function activeInstancesFor(
  request: APIRequestContext,
  order: string,
): Promise<FlowInstance[]> {
  return (await listInstances(request)).filter(
    (i) => i.orderId === order && ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status),
  );
}

/** Fails every active instance this spec may have left on the setup order. */
async function freeOrder(request: APIRequestContext): Promise<void> {
  for (const blocker of await activeInstancesFor(request, orderId)) {
    await terminateInstance(request, prosthetistHeaders, blocker.id);
  }
}

async function findTemplateByName(
  request: APIRequestContext,
  name: string,
): Promise<FlowTemplate> {
  const res = await request.get(`${PROSTH}/templates`, { headers: prosthetistHeaders });
  expect(res.ok()).toBeTruthy();
  const templates = (await res.json()) as FlowTemplate[];
  const template = templates.find((t) => t.name === name);
  expect(template, `template ${name} is missing`).toBeTruthy();
  return template!;
}

async function archiveTempTemplate(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const res = await request.delete(`${PROSTH}/templates/${templateId}`, {
    headers: adminHeaders,
  });
  expect([204, 404]).toContain(res.status());
}

/** Archives ACTIVE/DRAFT `E2E-*` templates a crashed earlier run may have left. */
async function archiveLeftoverTemplates(request: APIRequestContext): Promise<void> {
  const res = await request.get(`${PROSTH}/templates`, { headers: adminHeaders });
  expect(res.ok()).toBeTruthy();
  const templates = (await res.json()) as FlowTemplate[];
  for (const template of templates) {
    const isTemp = TEMP_NAME_PREFIXES.some((prefix) => template.name.startsWith(prefix));
    if (isTemp && template.status !== 'ARCHIVED') {
      await archiveTempTemplate(request, template.id);
    }
  }
}

/**
 * Creates an ACTIVE template that exists only for the duration of one test.
 * One INFORMATION step keeps the instance startable/failable (the API cleanup
 * drives it NEW → IN_PROGRESS → FAILED).
 */
async function createTempTemplate(
  request: APIRequestContext,
  args: { name: string; amputationLevel?: string | null },
): Promise<FlowTemplate> {
  const created = await request.post(`${PROSTH}/templates`, {
    headers: adminHeaders,
    data: {
      name: args.name,
      description: 'E2E #454 — тимчасовий маршрут, архівується після тесту',
      productType: 'UPPER_LIMB',
      amputationLevel: args.amputationLevel ?? null,
      estimatedDurationMin: 30,
      stages: [
        {
          name: 'E2E етап',
          type: 'TECHNICAL',
          steps: [{ name: 'E2E крок', stepType: 'INFORMATION' }],
        },
      ],
    },
  });
  expect(
    created.ok(),
    `create template failed: ${created.status()}: ${await created.text()}`,
  ).toBeTruthy();
  const template = (await created.json()) as FlowTemplate;

  const activated = await request.patch(`${PROSTH}/templates/${template.id}`, {
    headers: adminHeaders,
    data: { status: 'ACTIVE' },
  });
  expect(activated.ok()).toBeTruthy();
  return template;
}

/** Seeds the setup draft so review opens directly on an existing order. */
async function openReview(page: Page, patientId: string, order: string): Promise<void> {
  await page.addInitScript(
    ([pid, oid]) => {
      const draft = JSON.stringify({
        patientId: pid,
        orderId: oid,
        templateId: null,
        instanceId: null,
      });
      window.localStorage.setItem('prosthetics:draft', draft);
      window.sessionStorage.setItem('prosthetics:draft', draft);
    },
    [patientId, order],
  );
  await page.goto('/prosthetics/new/review-order');
  await expect(page.getByRole('heading', { name: 'Перевірка замовлення' })).toBeVisible({
    timeout: 15000,
  });
  await expect(page.getByText(/Крок 3 з 3/)).toBeVisible({ timeout: 10000 });
}

/** Reads the persisted draft — the review page writes the chosen template there. */
async function draftTemplateId(page: Page): Promise<string | null> {
  return page.evaluate(() => {
    const raw = window.localStorage.getItem('prosthetics:draft');
    if (!raw) return null;
    try {
      return (JSON.parse(raw) as { templateId?: string | null }).templateId ?? null;
    } catch {
      return null;
    }
  });
}

function routesGroup(page: Page) {
  return page.getByRole('radiogroup', { name: 'Технологічні маршрути' });
}

function routeLabel(page: Page, name: string) {
  return routesGroup(page).locator('label').filter({ hasText: name });
}

async function uiCreatedInstanceId(page: Page): Promise<string> {
  await page.waitForURL('**/prosthetics/process/**', { timeout: 15000 });
  const instanceId = page.url().split('/process/')[1]?.split(/[/?#]/)[0];
  expect(instanceId, `no instance id in URL ${page.url()}`).toBeTruthy();
  return instanceId!;
}

/** Confirms the persisted instance and its immutable snapshot through the API. */
async function expectInstanceUsesTemplate(
  request: APIRequestContext,
  instanceId: string,
  templateId: string,
  templateName: string,
): Promise<void> {
  const instanceRes = await request.get(`${PROSTH}/instances/${instanceId}`, {
    headers: prosthetistHeaders,
  });
  expect(instanceRes.ok()).toBeTruthy();
  const instance = (await instanceRes.json()) as FlowInstance;
  expect(instance.templateId, 'instance must reference the chosen template').toBe(
    templateId,
  );

  const snapshotRes = await request.get(`${PROSTH}/instances/${instanceId}/snapshot`, {
    headers: prosthetistHeaders,
  });
  expect(snapshotRes.ok()).toBeTruthy();
  const snapshot = (await snapshotRes.json()) as { name: string; version: number };
  expect(snapshot.name).toBe(templateName);
  expect(snapshot.version).toBeGreaterThanOrEqual(1);
}

test.beforeAll(async ({ request }) => {
  const prosthetist = testUser(7);
  prosthetistHeaders = headersFor(await login(request, prosthetist.login, prosthetist.password));
  const admin = testUser(9);
  adminHeaders = headersFor(await login(request, admin.login, admin.password));

  await archiveLeftoverTemplates(request);

  // Deterministic find-or-create of the local order the review screen runs on.
  const provisioned = await request.post(`${PROSTH}/orders/provision`, {
    headers: prosthetistHeaders,
    data: { patientId: PATIENT_ID, documentId: UPPER_DOC_ID },
  });
  expect(provisioned.ok(), `provision failed: ${provisioned.status()}`).toBeTruthy();
  orderId = ((await provisioned.json()) as { id: string }).id;
  expect(orderId).toBeTruthy();

  tpUlId = (await findTemplateByName(request, TP_UL_NAME)).id;
});

test.beforeEach(async ({ request }) => {
  // A crashed earlier run must not turn every case into the duplicate guard.
  await freeOrder(request);
});

test.afterAll(async ({ request }) => {
  try {
    await freeOrder(request);
    await archiveLeftoverTemplates(request);
  } catch {
    // Cleanup is best-effort: the individual tests already clean after themselves.
  }
});

test.describe('Prosthetics setup — technology route on review (#449/#454)', () => {
  test('a sole product-type candidate stays manual and creates the process with it', async ({
    page,
    request,
  }) => {
    await openReview(page, PATIENT_ID, orderId);

    // The MIS order carries no amputation level → product-type fallback only.
    await expect(
      page.getByText(/Точного збігу за рівнем ампутації немає/),
    ).toBeVisible({ timeout: 15000 });
    await expect(page.getByText('За типом виробу').first()).toBeVisible();

    const routes = routesGroup(page);
    await expect(routes).toBeVisible({ timeout: 15000 });
    await expect(routes.getByRole('radio')).toHaveCount(1);
    await expect(routeLabel(page, TP_UL_NAME)).toBeVisible();

    // Decision #449: even a sole fallback candidate is never auto-selected.
    const startPrompt = page.getByRole('button', { name: 'Оберіть маршрут' });
    await expect(startPrompt).toBeDisabled();
    expect(await draftTemplateId(page)).toBeNull();

    await routeLabel(page, TP_UL_NAME).click();
    await expect(routeLabel(page, TP_UL_NAME).getByRole('radio')).toBeChecked();
    expect(await draftTemplateId(page)).toBe(tpUlId);

    const start = page.getByRole('button', { name: 'Старт' });
    await expect(start).toBeEnabled({ timeout: 15000 });

    let createdId: string | undefined;
    try {
      await start.click();
      createdId = await uiCreatedInstanceId(page);
      await expectInstanceUsesTemplate(request, createdId, tpUlId, TP_UL_NAME);
    } finally {
      if (createdId) await terminateInstance(request, prosthetistHeaders, createdId);
      await freeOrder(request);
    }
  });

  test('several candidates require an explicit choice — no arbitrary template', async ({
    page,
    request,
  }) => {
    const temp = await createTempTemplate(request, { name: `E2E-MULTI-${Date.now()}` });
    let createdId: string | undefined;
    try {
      await openReview(page, PATIENT_ID, orderId);

      const routes = routesGroup(page);
      await expect(routes).toBeVisible({ timeout: 15000 });
      await expect(routes.getByRole('radio')).toHaveCount(2);
      // The temp name sorts before TP-UL-01 — an "always pick the first"
      // implementation would fail the templateId assertion below.
      await expect(routeLabel(page, temp.name)).toBeVisible();

      await expect(page.getByRole('button', { name: 'Оберіть маршрут' })).toBeDisabled();
      await expect(routeLabel(page, temp.name).getByRole('radio')).not.toBeChecked();
      await expect(routeLabel(page, TP_UL_NAME).getByRole('radio')).not.toBeChecked();
      expect(await draftTemplateId(page)).toBeNull();

      await routeLabel(page, TP_UL_NAME).click();
      await expect(routeLabel(page, TP_UL_NAME).getByRole('radio')).toBeChecked();
      expect(await draftTemplateId(page)).toBe(tpUlId);

      const start = page.getByRole('button', { name: 'Старт' });
      await expect(start).toBeEnabled({ timeout: 15000 });
      await start.click();

      createdId = await uiCreatedInstanceId(page);
      await expectInstanceUsesTemplate(request, createdId, tpUlId, TP_UL_NAME);
    } finally {
      if (createdId) await terminateInstance(request, prosthetistHeaders, createdId);
      await freeOrder(request);
      await archiveTempTemplate(request, temp.id);
    }
  });

  test('zero candidates block creation and leave the order untouched', async ({
    page,
    request,
  }) => {
    const before = (await listInstances(request)).length;

    // No seeded order can have zero ACTIVE templates (both product types are
    // covered), so the empty response is injected at the network boundary.
    // Everything downstream — resolution, rendering, the disabled CTA and the
    // absence of a create call — is the real page code.
    await page.route(
      (url) =>
        url.pathname.endsWith('/api/prosthesis-manufacturing/templates') &&
        url.searchParams.get('status') === 'ACTIVE',
      async (route) => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: '[]',
          });
          return;
        }
        await route.continue();
      },
    );

    await openReview(page, PATIENT_ID, orderId);

    await expect(
      page.getByRole('alert').filter({ hasText: 'Немає доступних маршрутів' }),
    ).toBeVisible({ timeout: 15000 });
    await expect(page.getByText(/Створення процесу неможливе/)).toBeVisible();
    await expect(routesGroup(page)).toHaveCount(0);

    await expect(page.getByRole('button', { name: 'Немає доступних маршрутів' })).toBeDisabled();

    // No process was created — the list is unchanged and the order stays free.
    expect((await listInstances(request)).length).toBe(before);
    expect(await activeInstancesFor(request, orderId)).toHaveLength(0);
    await expect(page).toHaveURL(/\/prosthetics\/new\/review-order$/);
  });

  test('a sole exact match is auto-selected and starts with that template', async ({
    page,
    request,
  }) => {
    // The seed order carries a Cyrillic amputation level that matches no seeded
    // template. A template with a blank amputation level matches any non-blank
    // query (`isAmputationLevelMatch`), leaving the exact branch one candidate.
    const temp = await createTempTemplate(request, {
      name: `E2E-AUTO-${Date.now()}`,
      amputationLevel: null,
    });
    let createdId: string | undefined;
    const before = (await activeInstancesFor(request, SEED_ORDER_WITH_LEVEL)).length;
    try {
      await openReview(page, '900001', SEED_ORDER_WITH_LEVEL);

      const routes = routesGroup(page);
      await expect(routes).toBeVisible({ timeout: 15000 });
      await expect(routes.getByRole('radio')).toHaveCount(1);
      await expect(routeLabel(page, temp.name)).toBeVisible();

      await expect(
        page.getByText('Автоматично обрано єдиний сумісний активний шаблон'),
      ).toBeVisible();
      await expect(page.getByText('Автоматично обрано').first()).toBeVisible();
      await expect(routeLabel(page, temp.name).getByRole('radio')).toBeChecked();
      expect(await draftTemplateId(page)).toBe(temp.id);

      const start = page.getByRole('button', { name: 'Старт' });
      await expect(start).toBeEnabled({ timeout: 15000 });

      await start.click();
      createdId = await uiCreatedInstanceId(page);
      await expectInstanceUsesTemplate(request, createdId, temp.id, temp.name);
    } finally {
      if (createdId) await terminateInstance(request, prosthetistHeaders, createdId);
      // Free the shared seed order as well (only an RBAC 403 probe reads it).
      for (const blocker of await activeInstancesFor(request, SEED_ORDER_WITH_LEVEL)) {
        await terminateInstance(request, prosthetistHeaders, blocker.id);
      }
      await archiveTempTemplate(request, temp.id);
    }
    expect(await activeInstancesFor(request, SEED_ORDER_WITH_LEVEL)).toHaveLength(before);
  });

  test('an active process for the order blocks a second creation', async ({ page, request }) => {
    // API-created blocker (NEW) — the guard must fire before any create call.
    const blockerRes = await request.post(`${PROSTH}/instances`, {
      headers: prosthetistHeaders,
      data: { orderId, templateId: tpUlId },
    });
    expect(
      blockerRes.ok(),
      `blocker create failed: ${blockerRes.status()}`,
    ).toBeTruthy();
    const blockerId = ((await blockerRes.json()) as FlowInstance).id;

    try {
      await openReview(page, PATIENT_ID, orderId);
      await expect(routesGroup(page)).toBeVisible({ timeout: 15000 });
      await routeLabel(page, TP_UL_NAME).click();

      const start = page.getByRole('button', { name: 'Старт' });
      await expect(start).toBeEnabled({ timeout: 15000 });
      await start.click();

      await expect(
        page.getByText(/Для цього замовлення вже існує процес у роботі/),
      ).toBeVisible({ timeout: 15000 });
      await expect(page).toHaveURL(/\/prosthetics\/new\/review-order$/);

      const active = await activeInstancesFor(request, orderId);
      expect(active).toHaveLength(1);
      expect(active[0].id).toBe(blockerId);
    } finally {
      await terminateInstance(request, prosthetistHeaders, blockerId);
      await freeOrder(request);
    }
  });
});
