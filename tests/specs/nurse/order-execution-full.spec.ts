import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';

test.describe('Nurse Order Execution', () => {
  test('prescriptions tab shows active orders for nurse', async ({ page, request }) => {
    const loginRes = await request.post(`${API}/auth/login`, {
      data: { login: 'doctor1', password: 'doctor123' },
    });
    expect(loginRes.ok()).toBeTruthy();
    const { token } = await loginRes.json();

    await page.goto('/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);

    const episodeId = page.url().match(/\/nurse\/episode\/([^/]+)/)?.[1];
    expect(episodeId).toBeTruthy();

    const daysRes = await request.get(`${API}/episodes/${episodeId}/clinical-days`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(daysRes.ok()).toBeTruthy();
    const days = await daysRes.json();
    const clinicalDayId = days[0]?.id;
    expect(clinicalDayId).toBeTruthy();

    const orderRes = await request.post(`${API}/clinical-days/${clinicalDayId}/orders`, {
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
    expect(orderRes.ok()).toBeTruthy();

    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByText('Dobutamine').first()).toBeVisible({ timeout: 10000 });
  });

  test('prescriptions tab shows order columns for nurse', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);

    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByRole('columnheader', { name: 'Препарат' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Доза' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Статус' })).toBeVisible();
  });
});
