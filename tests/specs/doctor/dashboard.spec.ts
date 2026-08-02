import { test, expect } from '../../fixtures/index';

test.describe('Doctor Dashboard', () => {
  test('displays active episodes with patient names', async ({ page }) => {
    await page.goto('/icu/doctor');
    await expect(page.getByText('Активні пацієнти')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('new card button navigates to create card page', async ({ page }) => {
    await page.goto('/icu/doctor');
    await page.getByRole('button', { name: 'Нова карта' }).click();
    await expect(page).toHaveURL('/icu/doctor/create-card');
  });

  test('search filters episodes', async ({ page }) => {
    await page.goto('/icu/doctor');
    await page.getByPlaceholder('Пошук пацієнта за ПІБ...').fill('Петренко');
    // Wait for table to filter
    await expect(page.getByText('Петренко').first()).toBeVisible({ timeout: 5000 });
  });

  test('opening an episode navigates to episode page', async ({ page }) => {
    await page.goto('/icu/doctor');
    const openBtn = page.getByRole('button', { name: 'Відкрити' }).first();
    await expect(openBtn).toBeVisible({ timeout: 10000 });
    await openBtn.click();
    await expect(page).toHaveURL(/\/icu\/doctor\/episode\//);
  });

  test('page title is set correctly', async ({ page }) => {
    await page.goto('/icu/doctor');
    await expect(page).toHaveTitle('ВАІТ — Лікар');
  });

  test('theme toggle persists across navigation (UC-25)', async ({ page }) => {
    await page.goto('/icu/doctor');
    const initial = await page.evaluate(() => localStorage.getItem('themeMode') || 'light');

    await page.getByRole('button', { name: 'Переключити тему' }).click();

    const flipped = await page.evaluate(() => localStorage.getItem('themeMode'));
    expect(flipped).not.toBe(initial);

    // Navigate away and back — theme must remain persisted.
    await page.goto('/icu/doctor/create-card');
    await page.goto('/icu/doctor');
    const afterNav = await page.evaluate(() => localStorage.getItem('themeMode'));
    expect(afterNav).toBe(flipped);
  });
});
