import { test, expect } from '../../fixtures/index';

test.describe('Create Card', () => {
  test('creates a new episode for a patient from MIS', async ({ page }) => {
    await page.goto('/doctor/create-card');
    await expect(page.getByText('Нова карта інтенсивної терапії')).toBeVisible();

    await page.getByLabel('ПІБ, телефон або № медкарти').fill('Бондаренко');
    const option = page.getByRole('option', { name: /Бондаренко/ });
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();

    await expect(page.getByText('Дані пацієнта (з МІС)')).toBeVisible();

    await page.getByRole('button', { name: 'Створити карту' }).click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);
  });

  test('shows info message for short search query', async ({ page }) => {
    await page.goto('/doctor/create-card');
    await page.getByLabel('ПІБ, телефон або № медкарти').fill('A');
    await expect(page.getByText('Введіть мінімум 2 символи')).toBeVisible();
  });
});
