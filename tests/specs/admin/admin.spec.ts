import { test, expect } from '../../fixtures/index';

test.describe('Admin Page', () => {
  test('displays administrative panel with users tab', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.getByRole('heading', { name: 'Адміністративна панель' })).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('tab', { name: 'Користувачі' })).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('tab', { name: 'Журнал аудиту' })).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('tab', { name: 'Статистика' })).toBeVisible({ timeout: 10000 });
  });

  test('users tab shows doctor user data', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.getByText('doctor1')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('doctor2')).toBeVisible();
  });

  test('users tab shows nurse user data', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.getByText('nurse1')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('nurse2')).toBeVisible();
  });

  test('page title is set correctly', async ({ page }) => {
    await page.goto('/admin');
    await expect(page).toHaveTitle('Адмін — Superhumans Lviv');
  });
});
