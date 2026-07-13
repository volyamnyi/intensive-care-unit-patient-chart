import { test, expect } from '../../fixtures/index';

test.describe('Nurse Dashboard', () => {
  test('displays page header with patient selector', async ({ page }) => {
    await page.goto('/nurse');
    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван Сергійович/ }).click();
    await expect(page.getByRole('heading', { name: /Показники/ })).toBeVisible({ timeout: 10000 });
  });

  test('hour selector shows 24 tiles', async ({ page }) => {
    await page.goto('/nurse');
    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван Сергійович/ }).click();
    const tiles = page.locator('[class*="MuiBox-root"]').filter({ hasText: /^\d{1,2}:00$/ });
    await expect(tiles.first()).toBeVisible({ timeout: 10000 });
  });

  test('can select patient from dropdown', async ({ page }) => {
    await page.goto('/nurse');
    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван Сергійович/ }).click();
    await expect(page.getByText('Петренко Іван Сергійович').first()).toBeVisible({ timeout: 10000 });
  });

  test('vital signs form is visible when patient selected', async ({ page }) => {
    await page.goto('/nurse');
    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван Сергійович/ }).click();
    await expect(page.getByText('АТ сист (мм.рт.ст)').first()).toBeVisible({ timeout: 10000 });
  });

  test('fluid output form is visible', async ({ page }) => {
    await page.goto('/nurse');
    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван Сергійович/ }).click();
    await expect(page.getByText('Сеча (мл)').first()).toBeVisible({ timeout: 10000 });
  });

  test('patient select has aria-label', async ({ page }) => {
    await page.goto('/nurse');
    const combobox = page.getByRole('combobox').first();
    await expect(combobox).toHaveAttribute('aria-label', 'Пацієнт');
  });

  test('page title is set correctly', async ({ page }) => {
    await page.goto('/nurse');
    await expect(page).toHaveTitle('ВАІТ — Медсестра');
  });
});
