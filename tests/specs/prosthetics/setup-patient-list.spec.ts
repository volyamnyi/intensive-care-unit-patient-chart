import { test, expect } from '@playwright/test';
import { testUser } from '../../helpers/test-users';

const API = 'http://localhost:8085/api';
const PROSTH = `${API}/prosthesis-manufacturing`;

/**
 * MIS-stub note: GET /patients costs one stub-roster fetch, so the registry
 * snapshot is taken ONCE in beforeAll (one login, one fetch) and both tests
 * reuse it. The page loads perform their own fetches — those calls ARE the
 * tests. With the deterministic stub roster the snapshot is stable across runs.
 */
interface RegistryPatient {
  id: string;
  pib: string;
}

let headers: Record<string, string>;
let patients: RegistryPatient[];

test.beforeAll(async ({ request }) => {
  const u = testUser(7);
  const loginRes = await request.post(`${API}/auth/login`, {
    data: { login: u.login, password: u.password },
  });
  expect(loginRes.ok()).toBeTruthy();
  headers = { Authorization: `Bearer ${(await loginRes.json()).token as string}` };

  const res = await request.get(`${PROSTH}/patients`, { headers });
  expect(res.ok()).toBeTruthy();
  patients = (await res.json()) as RegistryPatient[];
  expect(patients.length).toBeGreaterThan(0);
});

test.describe('Prosthetics setup — step 1 lists the MIS patient registry', () => {
  test('select-patient table matches GET /patients (no candidates gate)', async ({ page }) => {
    // Asserted against the shared beforeAll snapshot — no new MIS call;
    // the page performs its own registry fetch.
    await page.goto('/prosthetics/new/select-patient');
    await expect(page.getByRole('heading', { name: /Вибір пацієнта/ })).toBeVisible({ timeout: 10000 });

    // One table row per registry patient — the candidates worklist
    // (local-orders + 120/121-docs gate) must not starve step 1.
    await expect(page.locator('table tbody tr')).toHaveCount(patients.length, { timeout: 15000 });
    for (const p of patients) {
      await expect(page.getByText(p.pib, { exact: true })).toBeVisible();
    }
  });

  test('search narrows the registry via the debounced query', async ({ page }) => {
    // Search target comes from the shared beforeAll snapshot — no new MIS
    // call; the page performs its own registry fetch.
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
