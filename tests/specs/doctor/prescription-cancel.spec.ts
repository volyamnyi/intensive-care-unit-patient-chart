import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';
const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

function futureStartTime(): string {
  const d = new Date();
  d.setHours(d.getHours() + 2);
  return d.toISOString().slice(0, 16);
}

test.describe('Prescription Cancel', () => {
  test('creates prescription via UI and shows active status', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);

    await page.getByRole('button', { name: '+ Нове призначення' }).click();

    await page.getByPlaceholder('Препарат').fill('Lidocaine');
    await page.getByPlaceholder('Доза').fill('100');
    await page.getByPlaceholder('Од.').fill('mg');
    await page.getByPlaceholder('Шлях').fill('IV');
    await page.getByPlaceholder('Частота').fill('PRN');
    await page.locator('input[type="datetime-local"]').first().fill(futureStartTime());

    await page.getByRole('button', { name: 'Створити' }).click();
    await expect(page.getByText('Lidocaine').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Активне').first()).toBeVisible({ timeout: 10000 });
  });

  test('prescription form has cancel button to close form', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);

    await page.getByRole('button', { name: '+ Нове призначення' }).click();

    await expect(page.getByText('Нове призначення').first()).toBeVisible();
    await page.getByRole('button', { name: 'Скасувати' }).first().click();
    await expect(page.getByRole('button', { name: '+ Нове призначення' })).toBeVisible();
  });
});
