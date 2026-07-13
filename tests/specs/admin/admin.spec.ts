import { test, expect } from '../../fixtures/index';

test.describe('Admin Page', () => {
  test('displays doctors and nurses tables', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.getByText('Користувачі системи')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Лікарі')).toBeVisible();
    await expect(page.getByText('Медсестри')).toBeVisible();
  });

  test('doctor table shows user details', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.getByText('doctor1')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('doctor2')).toBeVisible();
  });

  test('nurse table shows user details', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.getByText('nurse1')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('nurse2')).toBeVisible();
  });

  test('page title is set correctly', async ({ page }) => {
    await page.goto('/admin');
    await expect(page).toHaveTitle('ВАІТ — Адміністратор');
  });
});
