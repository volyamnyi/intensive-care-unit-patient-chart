import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';
const HOD_PATIENT_ID = '00000000-0000-0000-0000-000000001005'; // Ткачук

async function getToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: 'head1', password: 'head123' },
  });
  return (await res.json()).token as string;
}

async function closeActiveEpisode(request: any, token: string) {
  const response = await request.get(`${API}/episodes`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const list = await response.json() as any[];
  const active = list.find((e: any) => e.patientId === HOD_PATIENT_ID && e.status === 'ACTIVE');
  if (active) {
    await request.post(`${API}/episodes/${active.id}/close`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { dischargeDate: new Date().toISOString(), version: active.version },
    });
  }
}

test.describe('HOD Dashboard', () => {
  test('displays active patients list', async ({ page }) => {
    await page.goto('/doctor');
    await expect(page.getByText('Активні пацієнти ВАІТ')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('can create a new ICU card', async ({ page, request }) => {
    const token = await getToken(request);
    await closeActiveEpisode(request, token);

    await page.goto('/doctor/create-card');
    await page.getByLabel('ПІБ, телефон або № медкарти').fill('Ткачук');

    const option = page.getByRole('option', { name: /Ткачук/ });
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();

    await expect(page.getByText('Дані пацієнта (з МІС)')).toBeVisible();
    await page.getByRole('button', { name: 'Створити карту' }).click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);
  });

  test('can view prescriptions tab on episode page', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByRole('button', { name: '+ Нове призначення' })).toBeVisible();
  });

  test('can view scales tab', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await page.getByRole('tab', { name: 'Шкали' }).click();
  });
});
