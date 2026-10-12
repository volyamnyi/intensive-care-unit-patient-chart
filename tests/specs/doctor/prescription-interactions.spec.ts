import { type APIRequestContext, type Browser, type BrowserContext, type Locator, type Page } from '@playwright/test';
import { test, expect } from '../../fixtures/index';
import { testUser } from '../../helpers/test-users';
import {
  getToken,
  firstMedicationPatient,
  ensureMedicationList,
} from '../../helpers/medication';

// Issues #304/#355 — drug-interaction warnings + animated alert. The dataset
// lives in the `med` DB (drug_interaction_drugs/pairs); imports are
// ADMINISTRATOR-only and replace the whole dataset, so the spec is serial
// and self-contained: it imports a 2-drug dataset (N02BE01 ↔ M01AE01,
// matching the MIS-stub `itemKindATC` fixtures), adds + plans both drugs on
// overlapping periods and verifies the read endpoint, the UI surfaces, the
// RBAC, the alert banner (announcement + severity), the centered overlay
// announcement (scroll-independent visibility, dismissal), the medium
// downgrade, the banner unmount on delete, and the screenshot baselines.

const API = 'http://localhost:8085/api';

// Same codes the MIS stub exposes via `itemKindATC` (Paracetamol / Ibuprofen).
const PARACETAMOL_ATC = 'N02BE01';
const IBUPROFEN_ATC = 'M01AE01';

function dataset() {
  return {
    drugs: [
      {
        id: 1,
        atc_code: PARACETAMOL_ATC,
        generic_en: 'Paracetamol',
        ukrainian_raw: 'Парацетамол 500 мг',
        confidence: 0.99,
        drug_interactions: [
          {
            id: 1,
            atc_code: IBUPROFEN_ATC,
            severity: 'high',
            interaction: 'Збільшення ризику шлунково-кишкової кровотечі',
            interaction_id: 'DI-0001',
          },
        ],
      },
      {
        id: 2,
        atc_code: IBUPROFEN_ATC,
        generic_en: 'Ibuprofen',
        ukrainian_raw: 'Ібупрофен 200 мг',
        confidence: 0.98,
        drug_interactions: [],
      },
    ],
  };
}

async function loginToken(request: APIRequestContext, slot: number): Promise<string> {
  const { login, password } = testUser(slot);
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

async function importDataset(request: APIRequestContext, token: string, payload: unknown = dataset()) {
  return request.post(`${API}/admin/drug-interactions/import`, {
    headers: { Authorization: `Bearer ${token}` },
    multipart: {
      file: {
        name: 'interactions.json',
        mimeType: 'application/json',
        buffer: Buffer.from(JSON.stringify(payload)),
      },
    },
  });
}

// Same pair, medium severity — for the downgrade scenario (#355).
function mediumDataset() {
  const d = dataset();
  d.drugs[0].drug_interactions[0].severity = 'medium';
  d.drugs[0].drug_interactions[0].interaction = 'Помірна взаємодія: потрібен моніторинг стану';
  d.drugs[0].drug_interactions[0].interaction_id = 'DI-0002';
  return d;
}

test.describe.serial('Drug interaction warnings (#304)', () => {
  let doctorToken: string;
  let listIdRef: string | undefined;
  const addedItemIds: string[] = [];
  // Set by the last test: the afterAll cleanup may only run when the whole
  // serial block completed in THIS worker. Playwright tears down (and runs
  // afterAll of) a failed worker before retrying in a fresh one — an ungated
  // cleanup deleted the IT304 items mid-run and every subsequent test/retry
  // found an empty list (no warnings, no banner).
  let suiteCompleted = false;

  // Playwright restarts the worker after a failed test (serial + retries): the
  // fresh module instance loses describe-level state, and a stale `listId`
  // produced literal `/prescriptions/doctor/undefined` navigations. Every test
  // resolves the list through this memoized helper — on a fresh worker it
  // re-ensures the same always-open list of the deterministic stub patient.
  async function currentListId(request: APIRequestContext): Promise<string> {
    if (!listIdRef) {
      const patient = await firstMedicationPatient(request, doctorToken);
      listIdRef = await ensureMedicationList(request, doctorToken, patient.id);
    }
    return listIdRef;
  }

  // The centered overlay paints over the middle of the viewport and would
  // leak into element screenshots of the in-flow banner — close it first
  // (the committed banner baselines pre-date the overlay).
  async function dismissOverlay(pg: Page) {
    const ov = pg.getByTestId('interaction-alert-overlay');
    // The overlay renders when the interactions fetch resolves — it may not be
    // there yet when the banner (fed by the same response) already is.
    await ov.waitFor({ state: 'visible', timeout: 10000 }).catch(() => {});
    const btn = ov.getByRole('button', { name: 'Закрити сповіщення' });
    if (await btn.isVisible().catch(() => false)) await btn.click();
    await expect(ov).toHaveCount(0);
  }

  // The overlap period («Період перетину: 09.10–09.10») is anchored to the
  // run date — every CI day renders different digits and the baselines drift
  // (caught live: a deterministic 221px diff, threshold 200). Mask the line.
  const periodMask = (scope: Locator) => scope.locator('p.text-xs').first();

  test.beforeAll(async ({ request }) => {
    doctorToken = await getToken(request, 1);
  });

  test.afterAll(async ({ request }) => {
    // Keep the shared stub-patient list clean for the parallel doctor specs —
    // but only after the block actually finished (see suiteCompleted above).
    if (!suiteCompleted) return;
    for (const id of addedItemIds) {
      await request.delete(`${API}/prescriptions/items/${id}`, {
        headers: { Authorization: `Bearer ${doctorToken}` },
      });
    }
  });

  test('import as doctor is forbidden (403), as administrator succeeds', async ({ request }) => {
    const doctorRes = await importDataset(request, doctorToken);
    expect(doctorRes.status()).toBe(403);

    const adminRes = await importDataset(request, await loginToken(request, 6));
    expect(adminRes.ok()).toBeTruthy();
    const report = await adminRes.json();
    expect(report.drugs).toBe(2);
    expect(report.interactions).toBeGreaterThanOrEqual(1);
    expect(report.skipped).toBe(0);
    expect(typeof report.sourceHash).toBe('string');
  });

  test('reads: no warning before items exist, warnings after both drugs are planned', async ({ request }) => {
    const listId = await currentListId(request);

    const empty = await request.get(`${API}/prescriptions/${listId}/interactions`, {
      headers: { Authorization: `Bearer ${doctorToken}` },
    });
    expect(empty.ok()).toBeTruthy();
    const emptyBody = await empty.json();
    // Warnings are scoped to the list's items; pre-existing items of other
    // specs may already carry ATC, so assert shape instead of exact emptiness.
    expect(Array.isArray(emptyBody.warnings)).toBeTruthy();
    expect(typeof emptyBody.missingAtc.present).toBe('boolean');

    // Add BOTH drugs with their 7-char ATC codes (the helper variant sends no
    // ATC — that would exercise the missing-ATC notice path instead). Names are
    // unique so the concurrent doctor specs (which ensure/drop the shared-stub
    // 'Paracetamol 500 mg' on the same list) can't collide with these rows.
    for (const [name, atc] of [
      ['IT304-Paracetamol', PARACETAMOL_ATC],
      ['IT304-Ibuprofen', IBUPROFEN_ATC],
    ] as const) {
      // Drop any pre-existing row of the same name for determinism.
      const itemsRes = await request.get(`${API}/prescriptions/${listId}/items`, {
        headers: { Authorization: `Bearer ${doctorToken}` },
      });
      for (const it of (await itemsRes.json()).filter((i: any) => i?.medicineName === name)) {
        await request.delete(`${API}/prescriptions/items/${it.id}`, {
          headers: { Authorization: `Bearer ${doctorToken}` },
        });
      }
      const addRes = await request.post(`${API}/prescriptions/${listId}/items`, {
        headers: { Authorization: `Bearer ${doctorToken}` },
        data: { medicineName: name, medicineAtcCode: atc },
      });
      expect(addRes.status()).toBe(201);
      const added = await addRes.json();
      addedItemIds.push(added.id);
      expect(added.medicineAtcCode).toBe(atc);
    }

    const [paracetamolId, ibuprofenId] = addedItemIds.slice(-2);

    // Warnings are computed over the PLANNED period only — plan the first
    // morning part of each new item so the two 21-day periods overlap.
    const planTarget = (itemId: string) =>
      request.get(`${API}/prescriptions/${listId}/items`, {
        headers: { Authorization: `Bearer ${doctorToken}` },
      }).then(async (r) => {
        const items: any[] = await r.json();
        const item = items.find((i) => i.id === itemId);
        const morning = item.dayParts.find(
          (p: any) => p.period === 'morning' && !p.isCompleted && !p.isCompletedFinished,
        );
        if (!morning) throw new Error(`no open morning day part for item ${itemId}`);
        const planRes = await request.put(
          `${API}/prescriptions/day-parts/${morning.id}/plan`,
          { headers: { Authorization: `Bearer ${doctorToken}` }, data: { dose: '500 mg' } },
        );
        if (!planRes.ok()) throw new Error(`plan failed: ${planRes.status()}`);
      });
    await planTarget(paracetamolId);
    await planTarget(ibuprofenId);

    const withWarning = await request.get(`${API}/prescriptions/${listId}/interactions`, {
      headers: { Authorization: `Bearer ${doctorToken}` },
    });
    expect(withWarning.ok()).toBeTruthy();
    const body = await withWarning.json();
    const paracetamolWarn = body.warnings.find((w: any) => w.itemId === paracetamolId);
    expect(paracetamolWarn).toBeTruthy();
    // nameUk is the item's stored medicine name (the unique IT304-* label); the
    // dataset `ukrainian_raw` value is the PARTNER display name (see ibuprofenWarn).
    expect(paracetamolWarn.nameUk).toBe('IT304-Paracetamol');
    expect(paracetamolWarn.interactions[0].otherItemId).toBe(ibuprofenId);
    expect(paracetamolWarn.interactions[0].severity).toBe('high');
    expect(paracetamolWarn.interactions[0].interactionText).toBe(
      'Збільшення ризику шлунково-кишкової кровотечі',
    );
    expect(paracetamolWarn.interactions[0].interactionIds).toEqual(['DI-0001']);
    const ibuprofenWarn = body.warnings.find((w: any) => w.itemId === ibuprofenId);
    expect(ibuprofenWarn).toBeTruthy();
    expect(ibuprofenWarn.interactions[0].otherItemId).toBe(paracetamolId);
    // The partner display name comes from the dataset `ukrainian_raw` value.
    expect(ibuprofenWarn.interactions[0].otherNameUk).toBe('Парацетамол 500 мг');
    expect(Array.isArray(body.missingAtc.names)).toBeTruthy();
  });

  test('nurse may read the warnings (PATIENT_VIEW), may not import', async ({ request }) => {
    const nurseToken = await loginToken(request, 3);
    const read = await request.get(`${API}/prescriptions/${await currentListId(request)}/interactions`, {
      headers: { Authorization: `Bearer ${nurseToken}` },
    });
    expect(read.ok()).toBeTruthy();
    const body = await read.json();
    expect(body.warnings.length).toBeGreaterThanOrEqual(2);

    const importRes = await importDataset(request, nurseToken);
    expect(importRes.status()).toBe(403);
  });

  test('invalid import payloads are rejected (bad JSON → 400, 0 valid rows → 422)', async ({ request }) => {
    const adminToken = await loginToken(request, 6);

    const badJson = await request.post(`${API}/admin/drug-interactions/import`, {
      headers: { Authorization: `Bearer ${adminToken}` },
      multipart: {
        file: {
          name: 'interactions.json',
          mimeType: 'application/json',
          buffer: Buffer.from('{"drugs": ['),
        },
      },
    });
    expect(badJson.status()).toBe(400);

    const badAtc = await request.post(`${API}/admin/drug-interactions/import`, {
      headers: { Authorization: `Bearer ${adminToken}` },
      multipart: {
        file: {
          name: 'interactions.json',
          mimeType: 'application/json',
          buffer: Buffer.from(JSON.stringify({ drugs: [{ atc_code: 'A01AA1', ukrainian_raw: 'x' }] })),
        },
      },
    });
    // All rows skipped → the dataset is rejected as a whole (BUSINESS_RULE).
    expect(badAtc.status()).toBe(422);

    // The rejected 422 import must not have touched the good dataset.
    const adminRes = await importDataset(request, adminToken);
    expect(adminRes.ok()).toBeTruthy();
    expect((await adminRes.json()).drugs).toBeGreaterThanOrEqual(2);
  });

  test('UI: item name carries the severity mark and the planned day cell carries the red border', async ({ request, page }) => {
    await page.goto(`/prescriptions/doctor/${await currentListId(request)}`, { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 15000 });

    await expect(page.locator('td[data-interaction-severity="high"]').first()).toBeVisible({ timeout: 10000 });
    const warnedCells = page.locator('td[data-interaction-warn="true"]');
    // One planned day per item → at least 2 warned cells. Wait for the first
    // to appear, then check the count (toHaveCount takes a fixed number).
    await expect(warnedCells.first()).toBeVisible({ timeout: 10000 });
    expect(await warnedCells.count()).toBeGreaterThanOrEqual(2);
  });

  test('alert: HIGH banner appears after planning with the exact announcement (#355)', async ({ request, page }) => {
    await page.goto(`/prescriptions/doctor/${await currentListId(request)}`, { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 15000 });

    const alert = page.getByTestId('interaction-alert');
    await expect(alert).toBeVisible({ timeout: 10000 });
    await expect(alert).toHaveAttribute('role', 'alert');
    // Partner name comes from the dataset `ukrainian_raw` ('Ібупрофен 200 мг'),
    // and the A/B order follows the random UUID sort — assert order-agnostically.
    const label = await alert.getAttribute('aria-label');
    expect(label).toContain('Увага. Виявлено взаємодію високого рівня:');
    expect(label).toContain('IT304-Paracetamol');
    expect(label).toContain('Ібупрофен 200 мг');
    expect(label).toContain('Збільшення ризику шлунково-кишкової кровотечі');
    expect(label).toContain('Період перетину:');
    await expect(alert.getByText('високо', { exact: true })).toBeVisible();
    const text = await alert.textContent();
    expect(text).toContain('IT304-Paracetamol');
    expect(text).toContain('Ібупрофен 200 мг');
  });

  test('screenshots: HIGH banner at desktop and narrow widths', async ({ request, page }) => {
    await page.goto(`/prescriptions/doctor/${await currentListId(request)}`, { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 15000 });
    const alert = page.getByTestId('interaction-alert');
    await expect(alert).toBeVisible({ timeout: 10000 });
    await dismissOverlay(page);

    await expect(alert).toHaveScreenshot('interaction-alert-high-1280.png', { maxDiffPixels: 200, mask: [periodMask(alert)] });
    await page.setViewportSize({ width: 360, height: 800 });
    await expect(alert).toHaveScreenshot('interaction-alert-high-360.png', { maxDiffPixels: 200, mask: [periodMask(alert)] });
  });

  test('overlay: centered announcement is visible even when scrolled to the bottom', async ({ request, page }) => {
    await page.goto(`/prescriptions/doctor/${await currentListId(request)}`, { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 15000 });

    // A short viewport guarantees the page scrolls (the grid alone may fit a
    // full-height window): the in-flow banner must leave the viewport while
    // the centered overlay stays visible (the original complaint).
    await page.setViewportSize({ width: 1280, height: 400 });
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    expect(await page.evaluate(() => window.scrollY)).toBeGreaterThan(0);
    await expect(page.getByTestId('interaction-alert')).not.toBeInViewport();

    const overlay = page.getByTestId('interaction-alert-overlay');
    await expect(overlay).toBeVisible({ timeout: 10000 });
    // Class and role live on the inner card (the outer div is the fixed wrapper).
    const card = overlay.locator('.interaction-alert-center-enter');
    await expect(card).toBeVisible();
    // The banner owns the single assertive announcement; the overlay is visual only.
    await expect(card).toHaveAttribute('role', 'group');
    const center = await overlay.evaluate((el) => {
      const box = el.getBoundingClientRect();
      return {
        x: box.x + box.width / 2, y: box.y + box.height / 2,
        vw: window.innerWidth, vh: window.innerHeight,
      };
    });
    expect(Math.abs(center.x - center.vw / 2)).toBeLessThan(center.vw * 0.1);
    expect(Math.abs(center.y - center.vh / 2)).toBeLessThan(center.vh * 0.15);
    await expect(overlay.getByText('високо', { exact: true })).toBeVisible();
  });

  test('overlay: X dismisses it while the in-flow banner stays', async ({ request, page }) => {
    await page.goto(`/prescriptions/doctor/${await currentListId(request)}`, { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 15000 });
    const overlay = page.getByTestId('interaction-alert-overlay');
    await expect(overlay).toBeVisible({ timeout: 10000 });

    await overlay.getByRole('button', { name: 'Закрити сповіщення' }).click();
    await expect(overlay).toHaveCount(0);
    // Persistent context survives: the banner is untouched by the dismissal.
    await expect(page.getByTestId('interaction-alert')).toBeVisible();
  });

  test('overlay screenshots at desktop and narrow widths', async ({ request, page }) => {
    const listId = await currentListId(request);
    async function freshOverlay(width: number) {
      // A reload remounts the card and restarts its 10s auto-dismiss timer.
      await page.setViewportSize({ width, height: 800 });
      await page.goto(`/prescriptions/doctor/${listId}`, { waitUntil: 'domcontentloaded' });
      await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 15000 });
      const overlay = page.getByTestId('interaction-alert-overlay');
      await expect(overlay).toBeVisible({ timeout: 10000 });
      return overlay;
    }

    const wide = await freshOverlay(1280);
    await expect(wide).toHaveScreenshot('interaction-alert-overlay-1280.png', { maxDiffPixels: 200, mask: [periodMask(wide)] });
    const narrow = await freshOverlay(360);
    await expect(narrow).toHaveScreenshot('interaction-alert-overlay-360.png', { maxDiffPixels: 200, mask: [periodMask(narrow)] });
  });

  test('medium-only dataset downgrades the banner, then the high set is restored', async ({ request, page }) => {
    const listId = await currentListId(request);
    const adminRes = await importDataset(request, await loginToken(request, 6), mediumDataset());
    expect(adminRes.ok()).toBeTruthy();

    await page.goto(`/prescriptions/doctor/${listId}`, { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 15000 });
    const alert = page.getByTestId('interaction-alert');
    await expect(alert).toBeVisible({ timeout: 10000 });
    await expect(alert.getByText('помірно', { exact: true })).toBeVisible();
    await expect(alert.getByText('високо', { exact: true })).toHaveCount(0);
    await dismissOverlay(page);

    await page.setViewportSize({ width: 768, height: 800 });
    await expect(alert).toHaveScreenshot('interaction-alert-medium-768.png', { maxDiffPixels: 200, mask: [periodMask(alert)] });

    // Restore the high set for the tests below (and the shared DB afterwards).
    const restoreRes = await importDataset(request, await loginToken(request, 6));
    expect(restoreRes.ok()).toBeTruthy();
    await page.goto(`/prescriptions/doctor/${listId}`, { waitUntil: 'domcontentloaded' });
    await expect(page.getByTestId('interaction-alert').getByText('високо', { exact: true })).toBeVisible({ timeout: 10000 });
  });

  // Opens the list in a fresh context with the given context options, waits
  // for the banner and dismisses the overlay (it would leak into screenshots).
  async function openList(
    request: APIRequestContext,
    browser: Browser,
    contextOpts: object,
  ): Promise<{ ctx: BrowserContext; pg: Page }> {
    const listId = await currentListId(request);
    const ctx = await browser.newContext({ storageState: '.auth/doctor.json', ...contextOpts });
    const pg = await ctx.newPage();
    await pg.goto(`/prescriptions/doctor/${listId}`, { waitUntil: 'domcontentloaded' });
    await expect(pg.getByText('Статус: Відкрито')).toBeVisible({ timeout: 15000 });
    await expect(pg.getByTestId('interaction-alert')).toBeVisible({ timeout: 10000 });
    await dismissOverlay(pg);
    return { ctx, pg };
  }

  // The baselines are HIGH-content: if the medium-downgrade test failed between
  // its medium import and the restore, the DB still holds the medium dataset —
  // re-import HIGH instead of trusting the previous test's cleanup.
  async function ensureHighDataset(request: APIRequestContext) {
    const reImport = await importDataset(request, await loginToken(request, 6));
    expect(reImport.ok()).toBeTruthy();
  }

  test('screenshots: reduced-motion on HIGH', async ({ request, browser }) => {
    await ensureHighDataset(request);
    const reduced = await openList(request, browser, { reducedMotion: 'reduce' });
    await expect(reduced.pg.getByTestId('interaction-alert')).toHaveScreenshot(
      'interaction-alert-reduced-motion.png', { maxDiffPixels: 200, mask: [periodMask(reduced.pg.getByTestId('interaction-alert'))] },
    );
    await reduced.ctx.close();
  });

  test('screenshots: forced-colors on HIGH', async ({ request, browser }) => {
    await ensureHighDataset(request);
    const forced = await openList(request, browser, { forcedColors: 'active' });
    await expect(forced.pg.getByTestId('interaction-alert')).toHaveScreenshot(
      'interaction-alert-forced-colors.png', { maxDiffPixels: 200, mask: [periodMask(forced.pg.getByTestId('interaction-alert'))] },
    );
    await forced.ctx.close();
  });

  test('screenshots: 200% zoom on HIGH', async ({ request, browser }) => {
    await ensureHighDataset(request);
    const zoomed = await openList(request, browser, {});
    await zoomed.pg.evaluate(() => { document.body.style.zoom = '200%'; });
    await expect(zoomed.pg.getByTestId('interaction-alert')).toHaveScreenshot(
      'interaction-alert-zoom200.png', { maxDiffPixels: 200, mask: [periodMask(zoomed.pg.getByTestId('interaction-alert'))] },
    );
    await zoomed.ctx.close();
  });

  test('delete ibuprofen unmounts the banner and clears the marks', async ({ request, page }) => {
    const listId = await currentListId(request);
    // The list is shared with parallel doctor specs: refetch until the row
    // is readable (a concurrent mutation can briefly return an error shape).
    let ibuprofen: any = null;
    for (let attempt = 0; attempt < 5 && !ibuprofen; attempt++) {
      const itemsRes = await request.get(`${API}/prescriptions/${listId}/items`, {
        headers: { Authorization: `Bearer ${doctorToken}` },
      });
      expect(itemsRes.ok()).toBeTruthy();
      const body = await itemsRes.json();
      expect(Array.isArray(body)).toBeTruthy();
      ibuprofen = body.find((i: any) => i?.medicineName === 'IT304-Ibuprofen');
      if (!ibuprofen && attempt < 4) await new Promise((r) => setTimeout(r, 1000));
    }
    expect(ibuprofen).toBeTruthy();
    const delRes = await request.delete(`${API}/prescriptions/items/${ibuprofen.id}`, {
      headers: { Authorization: `Bearer ${doctorToken}` },
    });
    expect(delRes.ok()).toBeTruthy();
    const idx = addedItemIds.indexOf(ibuprofen.id);
    if (idx >= 0) addedItemIds.splice(idx, 1);

    await page.goto(`/prescriptions/doctor/${listId}`, { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 15000 });
    await expect(page.getByTestId('interaction-alert')).toHaveCount(0);
    await expect(page.locator('td[data-interaction-warn="true"]')).toHaveCount(0);
    await expect(page.locator('td[data-interaction-severity]')).toHaveCount(0);
    suiteCompleted = true;
  });
});
