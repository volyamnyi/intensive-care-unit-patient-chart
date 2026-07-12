import { test, expect } from '../../fixtures/index';

test.describe('Sign Off Day', () => {
  test('creates a card and signs it off completely', async ({ page }) => {
    await page.goto('/doctor/create-card');
    await page.getByRole('combobox').click();
    await page.getByRole('combobox').fill('Ткачук');

    const option = page.getByRole('option', { name: /Ткачук Андрій/ });
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();

    await page.getByLabel('Діагноз').fill('Тест E2E підписання');
    await page.getByLabel('APACHE II').fill('8');
    await page.getByLabel('SOFA').fill('2');

    await page.getByRole('button', { name: 'Створити карту' }).click();
    await expect(page).toHaveURL(/\/doctor\/card\/\d+\/day\/\d+/);

    await page.getByRole('button', { name: 'Підписати добу' }).click();
    await expect(page.getByText('Після підписання доба стане read-only')).toBeVisible();

    await page.getByRole('button', { name: 'Підписати' }).click();
    await expect(page).toHaveURL('/doctor');
  });
});
