import { test, expect } from '@playwright/test';
import { testUser } from '../../helpers/test-users';

const API = 'http://localhost:8085/api';
const PROSTH = `${API}/prosthesis-manufacturing`;

async function prosthetistToken(request: any): Promise<string> {
  const u = testUser(7);
  const res = await request.post(`${API}/auth/login`, {
    data: { login: u.login, password: u.password },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe('Prosthetics setup — step 2 MIS order documents', () => {
  test('GET /orders/documents narrows to templates 120/121 with live URLs', async ({ request }) => {
    const token = await prosthetistToken(request);
    const headers = { Authorization: `Bearer ${token}` };

    const res = await request.get(`${PROSTH}/orders/documents?patientId=13373`, { headers });
    expect(res.ok()).toBeTruthy();
    const docs = (await res.json()) as Array<{
      documentTemplateId: number;
      documentUrl: string;
    }>;
    if (docs.length === 0) {
      test.skip(true, 'No limb-order MIS documents for patient 13373 right now');
      return;
    }
    for (const d of docs) {
      expect([120, 121]).toContain(d.documentTemplateId);
      expect(d.documentUrl).toBeTruthy();
    }
  });

  test('select-order lists MIS documents and review embeds the picked documentUrl', async ({
    page,
    request,
  }) => {
    const token = await prosthetistToken(request);
    const headers = { Authorization: `Bearer ${token}` };

    const patientsRes = await request.get(`${PROSTH}/patients`, { headers });
    expect(patientsRes.ok()).toBeTruthy();
    const patients = (await patientsRes.json()) as Array<{ id: string; pib: string }>;
    expect(patients.length).toBeGreaterThan(0);
    const patient = patients[0];

    await page.goto('/prosthetics/new/select-patient');
    await expect(page.getByRole('heading', { name: /Вибір пацієнта/ })).toBeVisible({
      timeout: 10000,
    });
    const row = page.locator('table tbody tr').filter({ hasText: patient.pib }).first();
    await expect(row).toBeVisible({ timeout: 15000 });

    // MIS rotates documentUrl tokens per call, so the expected URLs must
    // come from the page's own fetch — not from an earlier API snapshot.
    const [docsResponse] = await Promise.all([
      page.waitForResponse(
        (resp) => resp.url().includes('/orders/documents') && resp.request().method() === 'GET',
        { timeout: 15000 },
      ),
      row.getByRole('button', { name: 'Обрати' }).click(),
    ]);
    expect(docsResponse.ok()).toBeTruthy();
    const docs = (await docsResponse.json()) as Array<{ documentUrl: string }>;
    await expect(page).toHaveURL(/select-order/);

    if (docs.length === 0) {
      await expect(page.getByText('Замовлень на протези в MIS не знайдено')).toBeVisible({
        timeout: 15000,
      });
      return;
    }

    const misTable = page.getByTestId('mis-order-documents');
    await expect(misTable).toBeVisible({ timeout: 15000 });
    await expect(misTable.locator('tbody tr')).toHaveCount(docs.length);
    await misTable.locator('tbody tr').first().getByRole('button', { name: 'Обрати' }).click();

    await expect(page).toHaveURL(/review-order/);
    const frame = page.locator('iframe[title="Замовлення на протез (MIS)"]');
    await expect(frame).toBeVisible({ timeout: 10000 });
    expect(await frame.getAttribute('src')).toBe(docs[0].documentUrl);
  });
});
