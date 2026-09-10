import { test, expect } from '../../fixtures/index';
import { testUser } from '../../helpers/test-users';

const API = 'http://localhost:8085/api';

async function getToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: testUser(1).login, password: testUser(1).password },
  });
  return (await res.json()).token as string;
}

// create-card's PatientSearch searches the dept-19 ICU roster from real MIS, so the
// page must be driven by a real patient (no hardcoded mock id/name).
async function firstIcuPatient(request: any, token: string): Promise<{ id: number; fullName: string; query: string }> {
  const res = await request.get(`${API}/patients?module=icu`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  const patients = await res.json();
  const p = patients.find((x: any) => typeof x?.fullName === 'string' && x.fullName.trim().length >= 2);
  if (!p) {
    throw new Error('No ICU (dept 19) patient with a ≥2-char full name available from real MIS');
  }
  return { id: p.id, fullName: p.fullName.trim(), query: p.fullName.trim().slice(0, 4) };
}

async function closeActiveEpisode(request: any, token: string, patientId: number) {
  const response = await request.get(`${API}/episodes`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const list = await response.json() as any[];
  const active = list.find((e: any) => e.patientId === patientId && e.status === 'ACTIVE');
  if (active) {
    await request.post(`${API}/episodes/${active.id}/close`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { dischargeDate: new Date().toISOString(), version: active.version },
    });
  }
}

// PatientSearch dropdown options are `div` wrappers whose first line is the full name.
function patientOption(page: any, fullName: string) {
  return page.locator('div:has(> p.font-semibold)').filter({ hasText: fullName }).first();
}

test.describe('Create Card', () => {
  test('creates a new episode for a patient from MIS', async ({ page, request }) => {
    const token = await getToken(request);
    const patient = await firstIcuPatient(request, token);
    await closeActiveEpisode(request, token, patient.id);

    await page.goto('/icu/doctor/create-card');
    await expect(page.getByText('Нова карта інтенсивної терапії')).toBeVisible();

    await page.getByLabel('ПІБ, телефон або ID').fill(patient.query);
    const option = patientOption(page, patient.fullName);
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();

    await expect(page.getByText('Дані пацієнта (з МІС)')).toBeVisible();

    await page.getByRole('button', { name: 'Створити карту' }).click();
    await expect(page).toHaveURL(/\/icu\/doctor\/episode\//);
  });

  test('shows info message for short search query', async ({ page }) => {
    await page.goto('/icu/doctor/create-card');
    await page.getByLabel('ПІБ, телефон або ID').fill('A');
    await expect(page.getByText('Введіть мінімум 2 символи')).toBeVisible();
  });

  test('cancel returns to doctor dashboard', async ({ page, request }) => {
    const token = await getToken(request);
    const patient = await firstIcuPatient(request, token);
    await closeActiveEpisode(request, token, patient.id);

    await page.goto('/icu/doctor/create-card');
    await page.getByLabel('ПІБ, телефон або ID').fill(patient.query);
    const option = patientOption(page, patient.fullName);
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();
    await expect(page.getByText('Дані пацієнта (з МІС)')).toBeVisible();

    await page.getByRole('button', { name: 'Скасувати' }).click();
    await expect(page).toHaveURL('/icu/doctor');
  });
});
