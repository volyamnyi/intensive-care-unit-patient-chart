import { test, expect } from '@playwright/test';

// Responsive UI Phase 6 (issue #165): tablet band (768px). The sidebar is a
// collapsed icon rail with an expand toggle (no hamburger — the sheet nav is
// mobile-only), and the prosthetics dashboard keeps its 2-column stat grid.

test.describe('tablet — doctor layout', () => {
  test('sidebar is a collapsed icon rail that expands on toggle', async ({ page }) => {
    await page.goto('/icu/doctor');

    const aside = page.getByLabel('Бічна панель');
    await expect(aside).toBeVisible();

    const collapsedBox = await aside.boundingBox();
    expect(collapsedBox!.width).toBeLessThanOrEqual(64);

    await page.getByRole('button', { name: 'Розгорнути меню' }).click();
    const expandedBox = await aside.boundingBox();
    expect(expandedBox!.width).toBeGreaterThanOrEqual(200);

    // The hamburger is mobile-only (<640px) — it must not render at 768px.
    await expect(page.getByRole('button', { name: 'Відкрити навігацію' })).toHaveCount(0);
  });
});

test.describe('tablet — prosthetist dashboard', () => {
  test.use({ storageState: '.auth/prosthetist.json' });

  test('stat cards render in two columns', async ({ page }) => {
    await page.goto('/prosthetics');

    // Stat labels duplicate the filter-tab labels; the stat cards come first
    // in the DOM, so .first() picks the card.
    const paused = page.getByText('Призупинені').first();
    const completed = page.getByText('Завершені').first();
    await expect(paused).toBeVisible();
    await expect(completed).toBeVisible();

    const pausedBox = await paused.boundingBox();
    const completedBox = await completed.boundingBox();
    expect(pausedBox!.y).toBe(completedBox!.y);
    expect(Math.abs(pausedBox!.x - completedBox!.x)).toBeGreaterThan(0);
  });
});