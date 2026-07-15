import { test, expect } from '../../fixtures/index';

test.describe('Doctor Dashboard', () => {
  test('displays active episodes with patient names', async ({ page }) => {
    await page.goto('/doctor');
    await expect(page.getByText('Активні пацієнти')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('new card button navigates to create card page', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Нова карта' }).click();
    await expect(page).toHaveURL('/doctor/create-card');
  });

  test('search filters episodes', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByPlaceholder('Пошук пацієнта за ПІБ...').fill('Петренко');
    // Wait for table to filter
    await expect(page.getByText('Петренко').first()).toBeVisible({ timeout: 5000 });
  });

  test('opening an episode navigates to episode page', async ({ page }) => {
    await page.goto('/doctor');
    const openBtn = page.getByRole('button', { name: 'Відкрити' }).first();
    await expect(openBtn).toBeVisible({ timeout: 10000 });
    await openBtn.click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);
  });

  test('page title is set correctly', async ({ page }) => {
    await page.goto('/doctor');
    await expect(page).toHaveTitle('ВАІТ — Лікар');
  });
});
