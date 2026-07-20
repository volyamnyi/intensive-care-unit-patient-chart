import { test, expect } from '../../fixtures/index';

test.describe('Nurse Vitals & Losses', () => {
  test('shows the hourly vitals grid (read-only for nurse)', async ({ page }) => {
    await page.goto('/nurse/episode/a3333333-3333-3333-3333-333333333333');
    await expect(page.getByText('Показник / година')).toBeVisible();
    await expect(page.getByText('АТсист')).toBeVisible();
  });

  test('nurse edits a loss cell inline', async ({ page }) => {
    await page.goto('/nurse/episode/a3333333-3333-3333-3333-333333333333');

    const input = page.getByLabel('Сеча 1:00').locator('input');
    await input.click();
    await input.fill('250');
    await input.press('Enter');

    await expect(page.getByLabel('Сеча 1:00').locator('input')).toHaveValue('250', { timeout: 10000 });
  });
});
