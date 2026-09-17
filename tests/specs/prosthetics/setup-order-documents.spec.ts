import { test, expect } from '@playwright/test';
import { testUser } from '../../helpers/test-users';

const API = 'http://localhost:8085/api';
const PROSTH = `${API}/prosthesis-manufacturing`;

/**
 * MIS-stub note: every GET /orders/documents and every POST /provision costs
 * one stub round-trip for `spiDocumentProsthesCheck`, and GET /patients one
 * stub-roster fetch. This file therefore fetches the shared fixtures ONCE in
 * beforeAll — one login, one documents snapshot for patient 13373, one
 * patients snapshot — and all three tests reuse them. Only the calls under
 * test (the page's own fetches, the two provision POSTs) still reach the stub.
 * The stub roster always carries 120/121 documents for 13373, so no skip
 * branches are needed.
 */
const PATIENT_ID = '13373';

interface OrderDoc {
  documentId: number;
  documentTemplateId: number;
  documentUrl: string;
}

interface RegistryPatient {
  id: string;
  pib: string;
}

let headers: Record<string, string>;
let docs: OrderDoc[];
let firstPatient: RegistryPatient;

test.beforeAll(async ({ request }) => {
  const u = testUser(7);
  const loginRes = await request.post(`${API}/auth/login`, {
    data: { login: u.login, password: u.password },
  });
  expect(loginRes.ok()).toBeTruthy();
  headers = { Authorization: `Bearer ${(await loginRes.json()).token as string}` };

  const docsRes = await request.get(`${PROSTH}/orders/documents?patientId=${PATIENT_ID}`, {
    headers,
  });
  expect(docsRes.ok()).toBeTruthy();
  docs = (await docsRes.json()) as OrderDoc[];

  const patientsRes = await request.get(`${PROSTH}/patients`, { headers });
  expect(patientsRes.ok()).toBeTruthy();
  const patients = (await patientsRes.json()) as RegistryPatient[];
  expect(patients.length).toBeGreaterThan(0);
  firstPatient = patients[0];
});

test.describe('Prosthetics setup — step 2 MIS order documents', () => {
  test('GET /orders/documents narrows to templates 120/121 with live URLs', async () => {
    // Asserted against the shared beforeAll snapshot — no new stub call.
    // The stub always serves both 120 and 121 documents for patient 13373.
    expect(docs.length).toBeGreaterThan(0);
    for (const d of docs) {
      expect([120, 121]).toContain(d.documentTemplateId);
      expect(d.documentUrl).toBeTruthy();
    }
  });

  test('select-order lists MIS documents and review embeds the picked documentUrl', async ({
    page,
    request,
  }) => {
    // Registry row comes from the shared beforeAll snapshot (no roster
    // re-fetch); the page performs its own patients + documents fetches —
    // those two calls ARE the test.
    const patient = firstPatient;

    await page.goto('/prosthetics/new/select-patient');
    await expect(page.getByRole('heading', { name: /Вибір пацієнта/ })).toBeVisible({
      timeout: 10000,
    });
    const row = page.locator('table tbody tr').filter({ hasText: patient.pib }).first();
    await expect(row).toBeVisible({ timeout: 15000 });

    // The expected URLs come from the page's own fetch — not from an earlier
    // API snapshot (the stub serves stable URLs, so both agree).
    const [docsResponse] = await Promise.all([
      page.waitForResponse(
        (resp) => resp.url().includes('/orders/documents') && resp.request().method() === 'GET',
        { timeout: 15000 },
      ),
      row.getByRole('button', { name: 'Обрати' }).click(),
    ]);
    expect(docsResponse.ok()).toBeTruthy();
    const pageDocs = (await docsResponse.json()) as Array<{
      documentId: number;
      documentUrl: string;
    }>;
    await expect(page).toHaveURL(/select-order/);

    if (pageDocs.length === 0) {
      await expect(page.getByText('Замовлень на протези в MIS не знайдено')).toBeVisible({
        timeout: 15000,
      });
      return;
    }

    const misTable = page.getByTestId('mis-order-documents');
    await expect(misTable).toBeVisible({ timeout: 15000 });
    await expect(misTable.locator('tbody tr')).toHaveCount(pageDocs.length);
    await misTable.locator('tbody tr').first().getByRole('button', { name: 'Обрати' }).click();

    await expect(page).toHaveURL(/review-order/);
    const frame = page.locator('iframe[title="Замовлення на протез (MIS)"]');
    await expect(frame).toBeVisible({ timeout: 10000 });
    expect(await frame.getAttribute('src')).toBe(pageDocs[0].documentUrl);

    // Selecting the MIS document provisions the local order the execution
    // chain runs on — no pre-existing local rows required.
    const ordersRes = await request.get(`${PROSTH}/orders?patientId=${patient.id}`, { headers });
    expect(ordersRes.ok()).toBeTruthy();
    const orders = (await ordersRes.json()) as Array<{ orderNumber: string }>;
    expect(orders.map((o) => o.orderNumber)).toContain(`MIS-${patient.id}-${pageDocs[0].documentId}`);
  });

  test('POST /provision is idempotent for the same MIS document', async ({ request }) => {
    // Document id comes from the shared beforeAll snapshot — no new stub
    // documents call. The two provision POSTs each re-read the stub
    // server-side; those two reads ARE the test.
    expect(docs.length).toBeGreaterThan(0);

    const body = { patientId: PATIENT_ID, documentId: docs[0].documentId };
    const first = await request.post(`${PROSTH}/orders/provision`, { headers, data: body });
    expect(first.ok()).toBeTruthy();
    const second = await request.post(`${PROSTH}/orders/provision`, { headers, data: body });
    expect(second.ok()).toBeTruthy();
    expect((await second.json()).id).toBe((await first.json()).id);
  });
});
