import { test, expect } from '../../fixtures/index';

test.describe('Doctor Prescriptions', () => {
  test('creates a therapy prescription and shows it in the list', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: /Відкрити/ }).first().click();
    await page.getByRole('tab', { name: 'Призначення' }).click();

    await page.getByLabel('Препарат / дослідження').fill('Dopamine');
    await page.getByLabel('Доза').fill('200 mg');
    await page.getByLabel('Шлях').fill('IV');
    await page.getByLabel('Год. від').fill('23');

    await page.getByRole('button', { name: '+' }).click();

    await expect(page.getByText('Dopamine')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('200 mg')).toBeVisible();
  });

  test('creates a lab prescription', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: /Відкрити/ }).first().click();
    await page.getByRole('tab', { name: 'Призначення' }).click();

    await page.getByLabel('Тип').selectOption('LAB');
    await page.getByLabel('Препарат / дослідження').fill('Загальний аналіз крові');
    await page.getByLabel('Год. від').fill('23');

    await page.getByRole('button', { name: '+' }).click();

    await expect(page.getByText('Загальний аналіз крові')).toBeVisible({ timeout: 10000 });
  });

  test('prescription status shows active after creation', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: /Відкрити/ }).first().click();
    await page.getByRole('tab', { name: 'Призначення' }).click();

    await page.getByLabel('Препарат / дослідження').fill('Norepinephrine');
    await page.getByLabel('Доза').fill('4 mg');
    await page.getByLabel('Шлях').fill('IV');
    await page.getByLabel('Год. від').fill('23');

    await page.getByRole('button', { name: '+' }).click();

    await expect(page.getByText('Активне').first()).toBeVisible({ timeout: 10000 });
  });
});
