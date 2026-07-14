import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';

async function getDoctorToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: 'doctor1', password: 'doctor123' },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

async function createDoctorOrder(request: any, token: string, clinicalDayId: string) {
  const res = await request.post(`${API}/clinical-days/${clinicalDayId}/orders`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      category: 'MEDICATION',
      drugName: 'Dobutamine',
      dose: '250',
      unit: 'mg',
      route: 'IV',
      frequency: '8h',
      startTime: new Date().toISOString(),
    },
  });
  expect(res.ok()).toBeTruthy();
  return await res.json();
}

test.describe('Nurse Order Execution', () => {
  test('sees execute button for active orders', async ({ page, request }) => {
    const token = await getDoctorToken(request);

    await page.goto('/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);

    const episodeId = page.url().match(/episode\/([^/]+)/)?.[1];
    const daysRes = await request.get(`${API}/episodes/${episodeId}/clinical-days`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const days = await daysRes.json();
    const clinicalDayId = days[0]?.id;

    await createDoctorOrder(request, token, clinicalDayId);

    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByText('Dobutamine').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('columnheader', { name: 'Виконання' })).toBeVisible();
  });

  test('prescriptions tab shows order columns for nurse', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);

    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByRole('columnheader', { name: 'Препарат' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Доза' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Статус' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Виконання' })).toBeVisible();
  });
});
