import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

function futureStartTime(): string {
  const d = new Date();
  d.setHours(d.getHours() + 2);
  return d.toISOString().slice(0, 16);
}

test.describe('Doctor Prescriptions', () => {
  test('creates a prescription and shows it in the list', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);

    await page.getByRole('button', { name: '+ Нове призначення' }).click();

    await page.getByPlaceholder('Препарат').fill('Dopamine');
    await page.getByPlaceholder('Доза').fill('200');
    await page.getByPlaceholder('Од.').fill('mcg');
    await page.getByPlaceholder('Шлях').fill('IV');
    await page.getByPlaceholder('Частота').fill('stat');
    await page.locator('input[type="datetime-local"]').first().fill(futureStartTime());

    await page.getByRole('button', { name: 'Створити' }).click();

    await expect(page.getByText('Dopamine').first()).toBeVisible({ timeout: 10000 });
  });

  test('prescription status shows active after creation', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);

    await page.getByRole('button', { name: '+ Нове призначення' }).click();

    await page.getByPlaceholder('Препарат').fill('Norepinephrine');
    await page.getByPlaceholder('Доза').fill('4');
    await page.getByPlaceholder('Од.').fill('mcg');
    await page.getByPlaceholder('Шлях').fill('IV');
    await page.getByPlaceholder('Частота').fill('stat');
    await page.locator('input[type="datetime-local"]').first().fill(futureStartTime());

    await page.getByRole('button', { name: 'Створити' }).click();

    await expect(page.getByText('Активне').first()).toBeVisible({ timeout: 10000 });
  });
});
