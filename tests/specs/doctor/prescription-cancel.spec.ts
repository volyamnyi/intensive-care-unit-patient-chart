import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';

test.describe('Prescription Cancel', () => {
  test('creates prescription and shows it in the list', async ({ page, request }) => {
    const loginRes = await request.post(`${API}/auth/login`, {
      data: { login: 'doctor1', password: 'doctor123' },
    });
    expect(loginRes.ok()).toBeTruthy();
    const { token } = await loginRes.json();

    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    const episodeId = page.url().match(/\/doctor\/episode\/([^/]+)/)?.[1];
    expect(episodeId).toBeTruthy();

    const daysRes = await request.get(`${API}/episodes/${episodeId}/clinical-days`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(daysRes.ok()).toBeTruthy();
    const days = await daysRes.json();
    const dayId = days[0]?.id;
    expect(dayId).toBeTruthy();

    const orderRes = await request.post(`${API}/clinical-days/${dayId}/orders`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        category: 'MEDICATION',
        drugName: 'Lidocaine',
        dose: '100',
        unit: 'mg',
        route: 'IV',
        frequency: 'PRN',
        startTime: new Date().toISOString(),
      },
    });
    expect(orderRes.ok()).toBeTruthy();
    const order = await orderRes.json();

    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByText('Lidocaine').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Активне').first()).toBeVisible({ timeout: 10000 });

    const cancelRes = await request.post(`${API}/orders/${order.id}/cancel`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { version: order.version },
    });
    expect(cancelRes.ok()).toBeTruthy();

    await page.reload();
    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByText('Скасовано').first()).toBeVisible({ timeout: 10000 });
  });

  test('prescription form has cancel button to close form', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    await page.getByRole('tab', { name: 'Призначення' }).click();
    await page.getByRole('button', { name: '+ Нове призначення' }).click();

    await expect(page.getByText('Нове призначення')).toBeVisible();
    await page.getByRole('button', { name: 'Скасувати' }).click();
    await expect(page.getByRole('button', { name: '+ Нове призначення' })).toBeVisible();
  });
});
