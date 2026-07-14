import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';
const PATIENT_ID = '00000000-0000-0000-0000-000000001004'; // Бондаренко

async function getToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: 'doctor1', password: 'doctor123' },
  });
  return (await res.json()).token as string;
}

async function closeActiveEpisode(request: any, token: string) {
  const response = await request.get(`${API}/episodes`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const list = await response.json() as any[];
  const active = list.find((e: any) => e.patientId === PATIENT_ID && e.status === 'ACTIVE');
  if (active) {
    await request.post(`${API}/episodes/${active.id}/close`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { dischargeDate: new Date().toISOString(), version: active.version },
    });
  }
}

test.describe('Create Card', () => {
  test('creates a new episode for a patient from MIS', async ({ page, request }) => {
    const token = await getToken(request);
    await closeActiveEpisode(request, token);

    await page.goto('/doctor/create-card');
    await expect(page.getByText('Нова карта інтенсивної терапії')).toBeVisible();

    await page.getByLabel('ПІБ, телефон або № медкарти').fill('Бондаренко');
    const option = page.getByRole('option', { name: /Бондаренко/ });
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();

    await expect(page.getByText('Дані пацієнта (з МІС)')).toBeVisible();

    await page.getByRole('button', { name: 'Створити карту' }).click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);
  });

  test('shows info message for short search query', async ({ page }) => {
    await page.goto('/doctor/create-card');
    await page.getByLabel('ПІБ, телефон або № медкарти').fill('A');
    await expect(page.getByText('Введіть мінімум 2 символи')).toBeVisible();
  });

  test('cancel returns to doctor dashboard', async ({ page, request }) => {
    const token = await getToken(request);
    await closeActiveEpisode(request, token);

    await page.goto('/doctor/create-card');
    await page.getByLabel('ПІБ, телефон або № медкарти').fill('Бондаренко');
    const option = page.getByRole('option', { name: /Бондаренко/ });
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();
    await expect(page.getByText('Дані пацієнта (з МІС)')).toBeVisible();

    await page.getByRole('button', { name: 'Скасувати' }).click();
    await expect(page).toHaveURL('/doctor');
  });
});
