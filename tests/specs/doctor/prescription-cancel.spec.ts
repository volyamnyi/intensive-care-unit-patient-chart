import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';

async function getDoctorToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: 'doctor1', password: 'doctor123' },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe('Prescription Cancel', () => {
  test('creates prescription and cancels it via API, status changes', async ({ page, request }) => {
    const token = await getDoctorToken(request);

    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    await page.getByRole('tab', { name: 'Призначення' }).click();
    await page.getByRole('button', { name: '+ Нове призначення' }).click();

    await page.getByLabel('Препарат').fill('Lidocaine');
    await page.getByLabel('Доза').fill('100');
    await page.getByLabel('Од.').fill('mg');
    await page.getByLabel('Шлях').fill('IV');
    await page.getByLabel('Частота').fill('PRN');
    await page.getByLabel('Початок').fill('2025-04-08T08:00');

    await page.getByRole('button', { name: 'Створити' }).click();
    await expect(page.getByText('Lidocaine').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Активне').first()).toBeVisible();

    const episodeId = page.url().match(/episode\/([^/]+)/)?.[1];
    const daysRes = await request.get(`${API}/episodes/${episodeId}/clinical-days`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const days = await daysRes.json();
    const dayId = days[0]?.id;

    const ordersRes = await request.get(`${API}/clinical-days/${dayId}/orders`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const orders = await ordersRes.json();
    const order = orders.find((o: any) => o.drugName === 'Lidocaine');
    expect(order).toBeTruthy();

    await request.post(`${API}/orders/${order.id}/cancel`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { version: order.version },
    });

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
