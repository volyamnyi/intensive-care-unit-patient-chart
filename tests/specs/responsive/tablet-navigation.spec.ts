import { test, expect, type Page } from '@playwright/test';

// Phase 5 (#179): hamburger-free tablet navigation at 768 — icon rail toggle
// round-trip, breadcrumb containment on a deep route, module switch through
// collapsed rail icons, and zero console errors across all navigations.

test.describe.configure({ mode: 'serial' });

const EPISODE = '/icu/doctor/episode/a3333333-3333-3333-3333-333333333333';

/** Collapsed-rail state is persisted in localStorage — reset it per test. */
function resetRailState(page: Page) {
  return page.addInitScript(() => {
    window.localStorage.removeItem('app-sidebar-collapsed');
  });
}

function collectConsoleErrors(page: Page): string[] {
  const errors: string[] = [];
  page.on('console', (m) => {
    if (m.type() === 'error') errors.push(m.text());
  });
  return errors;
}

test.describe('tablet navigation at 768 — doctor', () => {
  test.use({ storageState: '.auth/doctor.json' });

  test('rail toggle round-trip shows and hides labels without a hamburger', async ({ page }) => {
    resetRailState(page);
    const consoleErrors = collectConsoleErrors(page);

    await page.goto('/icu/doctor');
    const aside = page.getByLabel('Бічна панель');
    await expect(aside).toBeVisible();

    // Tablet band defaults to the collapsed icon rail…
    expect((await aside.boundingBox())!.width).toBeLessThanOrEqual(64);
    await expect(page.getByText('Навігація')).toBeHidden();

    // …which expands to labels on toggle.
    await page.getByRole('button', { name: 'Розгорнути меню' }).click();
    await expect
      .poll(async () => (await aside.boundingBox())!.width, { timeout: 10000 })
      .toBeGreaterThanOrEqual(200);
    await expect(page.getByText('Навігація')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Листок лікарських призначень' })).toBeVisible();

    // …and collapses back.
    await page.getByRole('button', { name: 'Згорнути меню' }).click();
    await expect
      .poll(async () => (await aside.boundingBox())!.width, { timeout: 10000 })
      .toBeLessThanOrEqual(64);
    await expect(page.getByText('Навігація')).toBeHidden();

    // The hamburger + sheet nav is mobile-only (<640px).
    await expect(page.getByRole('button', { name: 'Відкрити навігацію' })).toHaveCount(0);

    expect(consoleErrors, consoleErrors.join('\n')).toEqual([]);
  });

  test('breadcrumbs stay contained on a deep route', async ({ page }) => {
    resetRailState(page);
    const consoleErrors = collectConsoleErrors(page);

    await page.goto(EPISODE);
    const crumbs = page.getByLabel('Breadcrumb');
    await expect(crumbs).toBeVisible({ timeout: 10000 });
    await expect(crumbs).toContainText('Пацієнти');
    await expect(crumbs).toContainText('День');

    // The strip is an internal scroller (touch-pan-x) — the page never shifts.
    const cls = (await crumbs.getAttribute('class')) ?? '';
    expect(cls).toContain('overflow-x-auto');
    expect(cls).toContain('touch-pan-x');

    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth))
      .toBeLessThanOrEqual(1);

    expect(consoleErrors, consoleErrors.join('\n')).toEqual([]);
  });

  test('collapsed rail icons navigate modules; breadcrumbs lead back', async ({ page }) => {
    resetRailState(page);
    const consoleErrors = collectConsoleErrors(page);

    await page.goto('/icu/doctor');
    await page.waitForLoadState('networkidle');

    // Icon-only rail: the accessible name comes from the title attribute.
    await page.getByRole('link', { name: 'Листок лікарських призначень' }).click();
    await expect(page).toHaveURL(/\/prescriptions\/doctor$/);

    // Deep-route breadcrumbs link back to the ICU dashboard.
    await page.goto(EPISODE);
    const back = page.getByLabel('Breadcrumb').getByRole('link', { name: 'Пацієнти' });
    await expect(back).toBeVisible({ timeout: 10000 });
    await back.click();
    await expect(page).toHaveURL(/\/icu\/doctor$/);

    expect(consoleErrors, consoleErrors.join('\n')).toEqual([]);
  });
});
