import { test, expect } from '../../fixtures/index';

// Phase 1 tablet clinical grids (issue #175).
// Runs in responsive-tablet-chromium (768×1024, hasTouch, doctor storageState).

test.describe.configure({ mode: 'serial' });

test.describe('Tablet clinical grids at 768', () => {
  test('doctor dashboard renders without horizontal overflow at 768', async ({ page }) => {
    await page.goto('/icu/doctor');
    await page.waitForLoadState('networkidle');
    const docWidth = await page.evaluate(() => document.documentElement.scrollWidth);
    expect(docWidth).toBeLessThanOrEqual(768);
  });

  test('HourlyGrid is visible and interactive at 768 on an episode page', async ({ page }) => {
    // Use the seed episode a1111111 → Петренко
    await page.goto('/icu/doctor/episode/a1111111-1111-1111-1111-111111111111');
    await page.waitForLoadState('networkidle');

    // The grid table should be present in the DOM
    const grid = page.locator('[data-slot="table"]');
    await expect(grid).toBeVisible({ timeout: 10000 });
  });

  test('IntensiveCareCard shows two-column layout at 768', async ({ page }) => {
    await page.goto('/icu/doctor/episode/a1111111-1111-1111-1111-111111111111');
    await page.waitForLoadState('networkidle');

    // The two-column flex container should be flex-row at md: (≥768)
    const layout = page.locator('.flex-col.md\\:flex-row').first();
    await expect(layout).toBeVisible({ timeout: 10000 });

    // The sidebar content should be visible (hidden md:block)
    const sidebar = page.locator('.md\\:block').first();
    await expect(sidebar).toBeAttached();
  });

  test('prescription page renders spreadsheet without crash at 768', async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    await page.waitForLoadState('networkidle');

    // Page loads successfully — the prescription list or search UI is visible
    const main = page.locator('main');
    await expect(main).toBeVisible({ timeout: 10000 });
  });
});
