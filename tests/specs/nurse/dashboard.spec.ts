import { test, expect } from '../../fixtures/index';

test.describe('Nurse Dashboard', () => {
  test('displays active patients list', async ({ page }) => {
    await page.goto('/nurse');
    await expect(page.getByText('Активні пацієнти')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('opens patient day page by clicking open', async ({ page }) => {
    await page.goto('/nurse');
    const openBtn = page.getByRole('button', { name: 'Відкрити' }).first();
    await expect(openBtn).toBeVisible({ timeout: 10000 });
    await openBtn.click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);
  });

  test('search filters the patients table', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByPlaceholder('Пошук пацієнта за ПІБ...').fill('Коваленко');
    await expect(page.getByText('Коваленко').first()).toBeVisible({ timeout: 5000 });
  });

  test('page title is set correctly', async ({ page }) => {
    await page.goto('/nurse');
    await expect(page).toHaveTitle('ВАІТ — Медсестра');
  });
});
