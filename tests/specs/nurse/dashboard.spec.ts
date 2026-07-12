import { test, expect } from '../../fixtures/index';

test.describe('Nurse Dashboard', () => {
  test('displays page header with patient selector', async ({ page }) => {
    await page.goto('/nurse');
    await expect(page.getByText('Показники')).toBeVisible({ timeout: 10000 });
  });

  test('hour selector shows 24 tiles', async ({ page }) => {
    await page.goto('/nurse');
    const tiles = page.locator('[class*="MuiBox-root"]').filter({ hasText: /^\d{1,2}:00$/ });
    await expect(tiles.first()).toBeVisible({ timeout: 10000 });
  });

  test('can select patient from dropdown', async ({ page }) => {
    await page.goto('/nurse');
    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван Сергійович/ }).click();
    await expect(page.getByText('Петренко Іван Сергійович')).toBeVisible({ timeout: 10000 });
  });

  test('vital signs form is visible when patient selected', async ({ page }) => {
    await page.goto('/nurse');
    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван Сергійович/ }).click();
    await expect(page.getByText('АТ сист')).toBeVisible({ timeout: 10000 });
  });

  test('fluid output form is visible', async ({ page }) => {
    await page.goto('/nurse');
    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван Сергійович/ }).click();
    await expect(page.getByText('Сеча')).toBeVisible({ timeout: 10000 });
  });
});
