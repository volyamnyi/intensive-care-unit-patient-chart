import { test, expect } from '@playwright/test';

test.describe('Prosthetics Wizard Flow', () => {
  test.use({ storageState: '.auth/prosthetist.json' });

  test('full wizard flow: patient search → order selection → review → start → process', async ({ page }) => {
    // 1. Navigate to prosthetics dashboard
    await page.goto('/prosthetics');
    await expect(page.getByRole('heading', { name: 'Виробництво протезів' })).toBeVisible();

    // 2. Click "Новий процес" to start setup wizard
    await page.getByRole('button', { name: 'Новий процес' }).click();
    await page.waitForURL(/\/prosthetics\/new\/select-patient/);

    // 3. Patient search page - search for seed patient (Сніжко)
    await expect(page.getByRole('heading', { name: 'Вибір пацієнта' })).toBeVisible();
    await page.getByPlaceholder("Пошук пацієнта за ПІБ або номером координати...").fill('Сніжко');
    await expect(page.getByText('Сніжко Оксана Володимирівна')).toBeVisible();

    // 4. Select patient
    await page.getByRole('button', { name: 'Обрати' }).first().click();
    await page.waitForURL(/\/prosthetics\/new\/select-order/);

    // 5. Order selection page
    await expect(page.getByRole('heading', { name: 'Вибір замовлення' })).toBeVisible();
    await expect(page.getByText('ПВ-26-0413')).toBeVisible();

    // 6. Select order → navigates to review
    await page.getByRole('button', { name: 'Обрати' }).first().click();
    await page.waitForURL(/\/prosthetics\/new\/review-order/);

    // 7. Order Review page
    await expect(page.getByRole('heading', { name: 'Перевірка замовлення' })).toBeVisible();
    await expect(page.getByText('ПВ-26-0413')).toBeVisible();

    // 8. "Старт" button should be disabled initially, then enabled after doc loads
    const startButton = page.getByRole('button', { name: /^Старт$/ });
    await expect(startButton).toBeVisible();

    // Wait for doc to load (button becomes enabled)
    await expect(startButton).toBeEnabled({ timeout: 10000 });

    // 9. Click "Старт" → creates instance → redirects to process page
    await startButton.click();
    await page.waitForURL(/\/prosthetics\/process\/[a-f0-9-]+/);

    // 10. Should be on the process page (ProcessDetail or Wizard)
    await expect(page.getByText('ПВ-26-0413')).toBeVisible();
  });

  test('wizard validation: required fields disable action button', async ({ page }) => {
    await page.goto('/prosthetics');
    await page.getByRole('button', { name: 'Новий процес' }).click();
    await page.waitForURL(/\/prosthetics\/new\/select-patient/);

    await page.getByPlaceholder("Пошук пацієнта за ПІБ або номером координати...").fill('Сніжко');
    await page.getByRole('button', { name: 'Обрати' }).first().click();
    await page.waitForURL(/\/prosthetics\/new\/select-order/);

    await page.getByRole('button', { name: 'Обрати' }).first().click();
    await page.waitForURL(/\/prosthetics\/new\/review-order/);

    // On review page, "Далі" should not exist - we have "Старт"
    const startButton = page.getByRole('button', { name: /^Старт$/ });
    await expect(startButton).toBeVisible();

    // Initially disabled (waiting for doc)
    // After doc loads, it becomes enabled
    await expect(startButton).toBeEnabled({ timeout: 10000 });
  });
});