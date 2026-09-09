import { test, expect } from '../../fixtures/index';
import { testUser } from '../../helpers/test-users';

const API = 'http://localhost:8085/api';

async function getToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: testUser(5).login, password: testUser(5).password },
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

test.describe('HOD Dashboard', () => {
  test('displays active patients list', async ({ page }) => {
    await page.goto('/icu/doctor');
    await expect(page.getByText('Активні пацієнти')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('can create a new ICU card', async ({ page, request }) => {
    const token = await getToken(request);
    const patient = await firstIcuPatient(request, token);
    await closeActiveEpisode(request, token, patient.id);

    await page.goto('/icu/doctor/create-card');
    await page.getByLabel('ПІБ, телефон або № медкарти').fill(patient.query);

    const option = patientOption(page, patient.fullName);
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();

    await expect(page.getByText('Дані пацієнта (з МІС)')).toBeVisible();
    await page.getByRole('button', { name: 'Створити карту' }).click();
    await expect(page).toHaveURL(/\/icu\/doctor\/episode\//);
  });

  test('can view prescriptions section on episode page', async ({ page }) => {
    await page.goto('/icu/doctor/episode/a3333333-3333-3333-3333-333333333333');
    await expect(page.getByRole('button', { name: '+ Нове призначення' })).toBeVisible();
  });

  test('can view scales section', async ({ page }) => {
    await page.goto('/icu/doctor/episode/a3333333-3333-3333-3333-333333333333');
    await expect(page.getByText('Шкали').first()).toBeVisible();
  });
});
