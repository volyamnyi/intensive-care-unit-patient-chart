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
    // The rail animates its width — poll until the transition settles.
    await expect
      .poll(async () => (await aside.boundingBox())!.width, { timeout: 10000 })
      .toBeGreaterThanOrEqual(200);

    // The hamburger is mobile-only (<640px) — it must not render at 768px.
    await expect(page.getByRole('button', { name: 'Відкрити навігацію' })).toHaveCount(0);
  });
});

test.describe('tablet — prosthetist dashboard', () => {
  test.use({ storageState: '.auth/prosthetist.json' });

  test('stat cards render in two columns', async ({ page }) => {
    await page.goto('/prosthetics');

    // Stat labels duplicate the filter-tab labels; the stat cards come first
    // in the DOM, so .first() picks the card. In a 2-column grid the first
    // row holds «Активні» + «Призупинені»: same y, different x.
    const active = page.getByText('Активні').first();
    const paused = page.getByText('Призупинені').first();
    await expect(active).toBeVisible();
    await expect(paused).toBeVisible();

    const activeBox = await active.boundingBox();
    const pausedBox = await paused.boundingBox();
    expect(activeBox!.y).toBe(pausedBox!.y);
    expect(pausedBox!.x).toBeGreaterThan(activeBox!.x);
  });
});