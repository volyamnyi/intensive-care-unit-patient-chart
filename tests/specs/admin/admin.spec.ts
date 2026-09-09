import { test, expect } from '../../fixtures/index';
import { testUser } from '../../helpers/test-users';

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
    await expect(page.getByText(testUser(1).login)).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(testUser(2).login)).toBeVisible();
  });

  test('users tab shows nurse user data', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.getByText(testUser(3).login)).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(testUser(4).login)).toBeVisible();
  });

  test('page title is set correctly', async ({ page }) => {
    await page.goto('/admin');
    await expect(page).toHaveTitle('Адмін — Superhumans Lviv');
  });
});
