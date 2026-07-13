import { test, expect } from '../../fixtures/index';

test.describe('Doctor Prescriptions', () => {
  test('creates a prescription and shows it in the list', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await page.getByRole('tab', { name: 'Призначення' }).click();

    await page.getByRole('button', { name: '+ Нове призначення' }).click();

    await page.getByLabel('Препарат').fill('Dopamine');
    await page.getByLabel('Доза').fill('200');
    await page.getByLabel('Од.').fill('mcg');
    await page.getByLabel('Шлях').fill('IV');
    await page.getByLabel('Частота').fill('stat');
    await page.getByLabel('Початок').fill('2025-04-08T08:00');

    await page.getByRole('button', { name: 'Створити' }).click();

    await expect(page.getByText('Dopamine')).toBeVisible({ timeout: 10000 });
  });

  test('prescription status shows active after creation', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await page.getByRole('tab', { name: 'Призначення' }).click();

    await page.getByRole('button', { name: '+ Нове призначення' }).click();

    await page.getByLabel('Препарат').fill('Norepinephrine');
    await page.getByLabel('Доза').fill('4');
    await page.getByLabel('Од.').fill('mcg');
    await page.getByLabel('Шлях').fill('IV');
    await page.getByLabel('Частота').fill('stat');
    await page.getByLabel('Початок').fill('2025-04-08T08:00');

    await page.getByRole('button', { name: 'Створити' }).click();

    await expect(page.getByText('Активне').first()).toBeVisible({ timeout: 10000 });
  });
});
