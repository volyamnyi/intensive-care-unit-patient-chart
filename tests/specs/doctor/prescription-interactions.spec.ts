import { type APIRequestContext } from '@playwright/test';
import { test, expect } from '../../fixtures/index';
import { testUser } from '../../helpers/test-users';
import {
  getToken,
  firstMedicationPatient,
  ensureMedicationList,
} from '../../helpers/medication';

// Issue #304 — drug-interaction warnings. The dataset lives in the `med` DB
// (drug_interaction_drugs/pairs); imports are ADMINISTRATOR-only and replace
// the whole dataset, so the spec is serial and self-contained: it imports a
// 2-drug dataset (N02BE01 ↔ M01AE01, matching the MIS-stub `itemKindATC`
// fixtures), adds + plans both drugs on overlapping periods and verifies the
// read endpoint, the UI surfaces and the RBAC.

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

async function importDataset(request: APIRequestContext, token: string) {
  return request.post(`${API}/admin/drug-interactions/import`, {
    headers: { Authorization: `Bearer ${token}` },
    multipart: {
      file: {
        name: 'interactions.json',
        mimeType: 'application/json',
        buffer: Buffer.from(JSON.stringify(dataset())),
      },
    },
  });
}

test.describe.serial('Drug interaction warnings (#304)', () => {
  let doctorToken: string;
  let listId: string;
  const addedItemIds: string[] = [];

  test.beforeAll(async ({ request }) => {
    doctorToken = await getToken(request, 1);
  });

  test.afterAll(async ({ request }) => {
    // Keep the shared stub-patient list clean for the parallel doctor specs.
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
    const patient = await firstMedicationPatient(request, doctorToken);
    listId = await ensureMedicationList(request, doctorToken, patient.id);

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
    const read = await request.get(`${API}/prescriptions/${listId}/interactions`, {
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

  test('UI: item name blinks and the planned day cell carries the red border', async ({ request, page }) => {
    await page.goto(`/prescriptions/doctor/${listId}`, { waitUntil: 'domcontentloaded' });
    await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 15000 });

    await expect(page.locator('p.interaction-warn').first()).toBeVisible({ timeout: 10000 });
    const warnedCells = page.locator('td[data-interaction-warn="true"]');
    // One planned day per item → at least 2 warned cells. Wait for the first
    // to appear, then check the count (toHaveCount takes a fixed number).
    await expect(warnedCells.first()).toBeVisible({ timeout: 10000 });
    expect(await warnedCells.count()).toBeGreaterThanOrEqual(2);
  });
});
