import { test, expect } from '../../fixtures/index';

// Phase 4 tablet pass (issue #178): the admin can operate the RBAC matrix
// fully at 768 — the sticky first column stays pinned while the role columns
// scroll horizontally, toggles mark the draft dirty, and a save round-trip
// restores the original grant. Runs in responsive-tablet-chromium with an
// ADMINISTRATOR storageState override.

test.describe.configure({ mode: 'serial' });

test.describe('Tablet admin at 768', () => {
  test.use({ storageState: '.auth/admin.json' });

  test('RBAC matrix pins its first column during horizontal scroll', async ({ page }) => {
    await page.goto('/admin');
    await page.waitForLoadState('networkidle');

    await page.getByRole('tab', { name: 'Доступи та ролі' }).click();
    const stickyHead = page.locator('th', { hasText: 'Операція' }).first();
    await expect(stickyHead).toBeVisible({ timeout: 10000 });

    // Each matrix <Table> nests TWO overflow-x-auto containers (the consumer
    // wrapper + the shadcn Table's internal container); both hold the sticky
    // header, so pick the innermost — it is the element that really scrolls.
    const wrapper = page
      .locator('div.overflow-x-auto')
      .filter({ has: page.locator('th', { hasText: 'Операція' }) })
      .last();
    await expect(wrapper).toBeVisible();

    const before = (await stickyHead.boundingBox())!.x;
    await wrapper.evaluate((el) => {
      el.scrollLeft = 400;
    });

    // The table must actually be wider than the 768 viewport…
    const scrolled = await wrapper.evaluate((el) => el.scrollLeft);
    expect(scrolled).toBeGreaterThan(0);

    // …while the sticky cell stays pinned at its original viewport position.
    const after = (await stickyHead.boundingBox())!.x;
    expect(Math.abs(after - before)).toBeLessThanOrEqual(2);
    await expect(stickyHead).toBeVisible();
  });

  test('matrix toggle marks dirty and a save round-trip restores the grant', async ({ page }) => {
    await page.goto('/admin');
    await page.waitForLoadState('networkidle');

    await page.getByRole('tab', { name: 'Доступи та ролі' }).click();
    const panel = page.getByRole('tabpanel');
    const firstCheckbox = panel.getByRole('checkbox').first();
    await expect(firstCheckbox).toBeVisible({ timeout: 10000 });

    const saveButton = panel.getByRole('button', { name: 'Зберегти зміни' });
    await expect(saveButton).toBeDisabled();

    // Grant → dirty badge + enabled CTA
    await firstCheckbox.click();
    await expect(page.getByText('Є незбережені зміни')).toBeVisible();
    await expect(saveButton).toBeEnabled();

    // Persist the grant…
    await saveButton.click();
    await expect(page.getByText(/Збережено змін: 1/)).toBeVisible({ timeout: 10000 });

    // …then revoke it again so shared RBAC state ends where it started.
    // After the saved grant, toggling off makes the draft dirty once more.
    await firstCheckbox.click();
    await expect(page.getByText('Є незбережені зміни')).toBeVisible();
    await saveButton.click();
    await expect(page.getByText(/Збережено змін: 1/)).toBeVisible({ timeout: 10000 });
  });

  test('stats tab pairs cards into two columns at 768', async ({ page }) => {
    await page.goto('/admin');
    await page.waitForLoadState('networkidle');

    await page.getByRole('tab', { name: 'Статистика' }).click();
    const cards = page.getByRole('tabpanel').locator('.grid > .rounded-xl');
    await expect(cards.first()).toBeVisible({ timeout: 10000 });

    // sm:grid-cols-2 applies at 768 → the first two cards share row 1
    const y1 = (await cards.nth(0).boundingBox())!.y;
    const y2 = (await cards.nth(1).boundingBox())!.y;
    expect(Math.abs(y1 - y2)).toBeLessThanOrEqual(2);

    const docWidth = await page.evaluate(() => document.documentElement.scrollWidth);
    expect(docWidth).toBeLessThanOrEqual(769);
  });
});
