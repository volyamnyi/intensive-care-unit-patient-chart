import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';
const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

test.describe('Prescription Cancel', () => {
  test('creates prescription via UI and shows active status', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);

    await page.getByRole('button', { name: '+ Нове призначення' }).click();

    await page.getByLabel('Препарат').fill('Lidocaine');
    await page.getByLabel('Доза').fill('100');
    await page.getByLabel('Од.').fill('mg');
    await page.getByLabel('Шлях').fill('IV');
    await page.getByLabel('Частота').fill('PRN');
    await page.getByLabel('Початок').fill('2025-04-08T08:00');

    await page.getByRole('button', { name: 'Створити' }).click();
    await expect(page.getByText('Lidocaine').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Активне').first()).toBeVisible({ timeout: 10000 });
  });

  test('prescription form has cancel button to close form', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);

    await page.getByRole('button', { name: '+ Нове призначення' }).click();

    await expect(page.getByRole('heading', { name: 'Нове призначення' })).toBeVisible();
    await page.getByRole('button', { name: 'Скасувати' }).first().click();
    await expect(page.getByRole('button', { name: '+ Нове призначення' })).toBeVisible();
  });
});
