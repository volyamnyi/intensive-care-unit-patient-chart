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

test.describe('Prosthetics setup — step 1 lists the MIS patient registry', () => {
  test('select-patient table matches GET /patients (no candidates gate)', async ({ page, request }) => {
    const token = await prosthetistToken(request);
    const headers = { Authorization: `Bearer ${token}` };

    const res = await request.get(`${PROSTH}/patients`, { headers });
    expect(res.ok()).toBeTruthy();
    const patients = (await res.json()) as Array<{ id: string; pib: string }>;
    expect(patients.length).toBeGreaterThan(0);

    await page.goto('/prosthetics/new/select-patient');
    await expect(page.getByRole('heading', { name: /Вибір пацієнта/ })).toBeVisible({ timeout: 10000 });

    // One table row per registry patient — the candidates worklist
    // (local-orders + 120/121-docs gate) must not starve step 1.
    await expect(page.locator('table tbody tr')).toHaveCount(patients.length, { timeout: 15000 });
    for (const p of patients) {
      await expect(page.getByText(p.pib, { exact: true })).toBeVisible();
    }
  });

  test('search narrows the registry via the debounced query', async ({ page, request }) => {
    const token = await prosthetistToken(request);
    const headers = { Authorization: `Bearer ${token}` };

    const res = await request.get(`${PROSTH}/patients`, { headers });
    expect(res.ok()).toBeTruthy();
    const patients = (await res.json()) as Array<{ id: string; pib: string }>;
    expect(patients.length).toBeGreaterThan(0);
    const target = patients[0].pib;
    const needle = target.trim().slice(0, Math.min(6, target.trim().length));
    expect(needle.length).toBeGreaterThanOrEqual(2);

    await page.goto('/prosthetics/new/select-patient');
    await expect(page.getByRole('heading', { name: /Вибір пацієнта/ })).toBeVisible({ timeout: 10000 });
    await expect(page.locator('table tbody tr')).toHaveCount(patients.length, { timeout: 15000 });

    await page.getByPlaceholder(/пошук пацієнта/i).fill(needle);
    await expect(page.getByText(target, { exact: true })).toBeVisible({ timeout: 10000 });
    const shown = await page.locator('table tbody tr').count();
    expect(shown).toBeGreaterThanOrEqual(1);
    expect(shown).toBeLessThanOrEqual(patients.length);
  });
});
