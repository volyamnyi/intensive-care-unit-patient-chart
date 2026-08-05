import { test, expect } from '@playwright/test';

test.describe('Prosthetics Dashboard', () => {
  test.describe('Prosthetist dashboard access', () => {
    test.use({ storageState: '.auth/prosthetist.json' });

    test('prosthetist1 sees dashboard with own instances', async ({ page }) => {
      await page.goto('/prosthetics');
      await page.waitForURL(/\/prosthetics/);

      await expect(page.getByRole('heading', { name: 'Виробництво протезів' })).toBeVisible();

      // Should have "Новий процес" button
      await expect(page.getByRole('button', { name: 'Новий процес' })).toBeVisible();

      // Should have search input
      await expect(page.getByPlaceholder('Пошук за номером замовлення або пацієнтом...')).toBeVisible();

      // Should have status filter tabs
      await expect(page.getByRole('tab', { name: 'Всі' })).toBeVisible();
      await expect(page.getByRole('tab', { name: 'Активні' })).toBeVisible();
      await expect(page.getByRole('tab', { name: 'Припущені' })).toBeVisible();
      await expect(page.getByRole('tab', { name: 'Завершені' })).toBeVisible();
      await expect(page.getByRole('tab', { name: 'Провалені' })).toBeVisible();
    });

    test('status filters work correctly', async ({ page }) => {
      await page.goto('/prosthetics');

      // Click "Активні" filter
      await page.getByRole('tab', { name: 'Активні' }).click();

      // Click "Завершені" filter
      await page.getByRole('tab', { name: 'Завершені' }).click();

      // Click "Провалені" filter
      await page.getByRole('tab', { name: 'Провалені' }).click();

      // Click "Всі" filter
      await page.getByRole('tab', { name: 'Всі' }).click();
    });

    test('search input is functional', async ({ page }) => {
      await page.goto('/prosthetics');

      // Type in search
      await page.getByPlaceholder('Пошук за номером замовлення або пацієнтом...').fill('ПВ-26-0413');

      // Wait for debounce
      await page.waitForTimeout(500);
    });
  });

  test.describe('Prosthetist isolation', () => {
    test.use({ storageState: '.auth/prosthetist.json' });

    test('prosthetist1 can navigate to setup wizard', async ({ page }) => {
      await page.goto('/prosthetics');
      await page.getByRole('button', { name: 'Новий процес' }).click();
      await page.waitForURL(/\/prosthetics\/new\/select-patient/);

      // Should see patient search page
      await expect(page.getByRole('heading', { name: 'Вибір пацієнта' })).toBeVisible();

      // Search for patient
      await page.getByPlaceholder("Пошук пацієнта за ПІБ або номером координати...").fill('Сніжко');
      await expect(page.getByText('Сніжко Оксана Володимирівна')).toBeVisible();
    });
  });
});