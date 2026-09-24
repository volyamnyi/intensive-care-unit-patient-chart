import { test, expect } from '../../fixtures/index';
import type { APIRequestContext } from '@playwright/test';
import { testUser } from '../../helpers/test-users';

// Issue #305 — admin «База взаємодій» catalog browse (summary + drugs + paged pairs).
// Serial: the import replaces the whole dataset (same 2-drug mini-dataset as the #304 spec).
// Runs in admin-chromium (storageState = admin).

test.describe.configure({ mode: 'serial' });

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
  const res = await request.post('/api/auth/login', { data: { login, password } });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.beforeAll(async ({ request }) => {
  const adminToken = await loginToken(request, 6);
  const res = await request.post('/api/admin/drug-interactions/import', {
    headers: { Authorization: `Bearer ${adminToken}` },
    multipart: {
      file: {
        name: 'interactions.json',
        mimeType: 'application/json',
        buffer: Buffer.from(JSON.stringify(dataset())),
      },
    },
  });
  expect(res.ok()).toBeTruthy();
});

test('tab shows the imported dataset: summary + pair row', async ({ page }) => {
  await page.goto('/admin');
  await page.getByRole('tab', { name: 'База взаємодій' }).click();

  await expect(page.getByText('Парацетамол 500 мг')).toBeVisible({ timeout: 15000 });
  await expect(page.getByText('Ібупрофен 200 мг')).toBeVisible();
  // Exact match: the summary chip reads «Високо:» (capitalized), the pair badge «високо».
  await expect(page.getByText('високо', { exact: true })).toBeVisible();
  await expect(page.getByText('Ст. 1 із 1')).toBeVisible();
});

test('severity filter narrows the rows', async ({ page }) => {
  await page.goto('/admin');
  await page.getByRole('tab', { name: 'База взаємодій' }).click();
  await expect(page.getByText('Парацетамол 500 мг')).toBeVisible({ timeout: 15000 });

  await page.getByRole('combobox', { name: 'Рівень взаємодії' }).click();
  await page.getByRole('option', { name: 'низько' }).click();
  await expect(page.getByText('Нічого не знайдено')).toBeVisible({ timeout: 10000 });
});
