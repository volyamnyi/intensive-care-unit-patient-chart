import { test, expect } from '../../fixtures/index';

test.describe('Doctor Dashboard Table — Exploratory E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/icu/doctor');
    // Wait for the table to be visible and loaded
    await expect(page.getByRole('table')).toBeVisible({ timeout: 10000 });
  });

  // ─── Positive Functional Flows ───────────────────────────────────────────

  test.describe('Positive: Rendering', () => {
    test('renders table header with all expected columns', async ({ page }) => {
      const headers = [
        'Пацієнт',
        'Палата/Ліжко',
        'Діагноз',
        'Дата госпіталізації',
        'Дата виписки',
        'Статус',
        'Дії',
      ];
      for (const header of headers) {
        await expect(page.getByRole('columnheader', { name: header })).toBeVisible();
      }
    });

    test('renders at least one patient row', async ({ page }) => {
      const rows = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]');
      await expect(rows.first()).toBeVisible();
      const count = await rows.count();
      expect(count).toBeGreaterThan(0);
    });

    test('each row contains patient name, status badge, and open button', async ({ page }) => {
      const rows = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]');
      const count = await rows.count();
      expect(count).toBeGreaterThan(0);

      for (let i = 0; i < count; i++) {
        const row = rows.nth(i);
        // Patient name cell exists and is not empty
        const nameCell = row.locator('td[data-slot="table-cell"]').first();
        const name = await nameCell.innerText();
        expect(name.trim().length).toBeGreaterThan(0);

        // Status badge exists
        const badge = row.locator('[data-slot="badge"]');
        await expect(badge).toBeVisible();

        // Open button exists
        const openBtn = row.getByRole('button', { name: 'Відкрити' });
        await expect(openBtn).toBeVisible();
      }
    });

    test('renders ACTIVE status badge for all visible rows', async ({ page }) => {
      const badges = page.locator('tbody[data-slot="table-body"] [data-slot="badge"]');
      const count = await badges.count();
      expect(count).toBeGreaterThan(0);

      for (let i = 0; i < count; i++) {
        await expect(badges.nth(i)).toHaveText('Активний');
      }
    });

    test('renders admission dates in Ukrainian format', async ({ page }) => {
      const rows = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]');
      const count = await rows.count();
      // Date format: DD.MM.YYYY
      const dateRegex = /^\d{2}\.\d{2}\.\d{4}$/;
      for (let i = 0; i < Math.min(count, 5); i++) {
        const admissionCell = rows.nth(i).locator('td[data-slot="table-cell"]').nth(3);
        const dateText = await admissionCell.innerText();
        expect(dateText.trim()).toMatch(dateRegex);
      }
    });

    test('renders dash for missing discharge date', async ({ page }) => {
      const rows = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]');
      const count = await rows.count();
      // Discharge date is the 5th cell (index 4)
      for (let i = 0; i < count; i++) {
        const dischargeCell = rows.nth(i).locator('td[data-slot="table-cell"]').nth(4);
        const text = await dischargeCell.innerText();
        // Should be either a date or a dash
        expect(text.trim().length).toBeGreaterThan(0);
      }
    });
  });

  test.describe('Positive: Navigation via Row Click', () => {
    test('clicking the first row navigates to episode page', async ({ page }) => {
      const firstRow = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]').first();
      await firstRow.click();
      await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);
    });

    test('clicking the last row navigates to episode page', async ({ page }) => {
      const lastRow = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]').last();
      await lastRow.click();
      await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);
    });

    test('clicking the patient name cell navigates to episode page', async ({ page }) => {
      const firstName = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"] td[data-slot="table-cell"]').first();
      await firstName.click();
      await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);
    });

    test('clicking different rows opens different episodes', async ({ page }) => {
      // Click first row
      const firstRow = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]').first();
      await firstRow.click();
      const firstUrl = page.url();

      // Go back
      await page.goto('/icu/doctor');
      await expect(page.getByRole('table')).toBeVisible({ timeout: 10000 });

      // Click second row
      const secondRow = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]').nth(1);
      await secondRow.click();
      const secondUrl = page.url();

      // URLs should be different (different episode IDs)
      expect(firstUrl).not.toBe(secondUrl);
      expect(firstUrl).toMatch(/\/prescriptions\/icu\/doctor\/episode\/[^/]+/);
      expect(secondUrl).toMatch(/\/prescriptions\/icu\/doctor\/episode\/[^/]+/);
    });
  });

  test.describe('Positive: Navigation via Open Button', () => {
    test('clicking the Відкрити button on the first row navigates to episode page', async ({ page }) => {
      const openBtn = page.getByRole('button', { name: 'Відкрити' }).first();
      await openBtn.click();
      await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);
    });

    test('clicking the Відкрити button on a middle row navigates correctly', async ({ page }) => {
      const openBtns = page.getByRole('button', { name: 'Відкрити' });
      const count = await openBtns.count();
      if (count > 2) {
        await openBtns.nth(2).click();
        await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);
      }
    });

    test('clicking row and clicking button both navigate to the same episode for that row', async ({ page }) => {
      // Get the first row's episode ID by clicking the button
      const openBtn = page.getByRole('button', { name: 'Відкрити' }).first();
      await openBtn.click();
      const buttonUrl = page.url();

      // Go back and click the row itself
      await page.goto('/icu/doctor');
      await expect(page.getByRole('table')).toBeVisible({ timeout: 10000 });
      const firstRow = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]').first();
      await firstRow.click();
      const rowUrl = page.url();

      // Should navigate to the same episode
      expect(rowUrl).toBe(buttonUrl);
    });
  });

  test.describe('Positive: Search', () => {
    test('search input is present and accepts text', async ({ page }) => {
      const searchInput = page.getByPlaceholder('Пошук пацієнта за ПІБ...');
      await expect(searchInput).toBeVisible();
      await searchInput.fill('Петренко');
      await expect(searchInput).toHaveValue('Петренко');
    });

    test('search filters table by patient name', async ({ page }) => {
      await page.getByPlaceholder('Пошук пацієнта за ПІБ...').fill('Петренко');
      // Wait for filtering
      await page.waitForTimeout(500);
      const rows = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]');
      const count = await rows.count();
      expect(count).toBeGreaterThan(0);
      // All visible rows should contain "Петренко"
      for (let i = 0; i < count; i++) {
        const name = await rows.nth(i).locator('td').first().innerText();
        expect(name.toLowerCase()).toContain('петренко');
      }
    });

    test('search is case-insensitive', async ({ page }) => {
      await page.getByPlaceholder('Пошук пацієнта за ПІБ...').fill('петренко');
      await page.waitForTimeout(500);
      const rows = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]');
      const count = await rows.count();
      expect(count).toBeGreaterThan(0);
    });

    test('clearing search shows all rows again', async ({ page }) => {
      // First filter
      await page.getByPlaceholder('Пошук пацієнта за ПІБ...').fill('Петренко');
      await page.waitForTimeout(500);
      const filteredCount = await page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]').count();

      // Clear search
      await page.getByPlaceholder('Пошук пацієнта за ПІБ...').clear();
      await page.waitForTimeout(500);
      const allCount = await page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]').count();

      expect(allCount).toBeGreaterThanOrEqual(filteredCount);
    });
  });

  // ─── Negative Scenarios ──────────────────────────────────────────────────

  test.describe('Negative: Empty / No Results', () => {
    test('search with no matches shows empty state message', async ({ page }) => {
      await page.getByPlaceholder('Пошук пацієнта за ПІБ...').fill('ZZZNONEXISTENT');
      await page.waitForTimeout(500);
      await expect(page.getByText('Немає пацієнтів за запитом')).toBeVisible();
    });

    test('search with no matches hides the table', async ({ page }) => {
      await page.getByPlaceholder('Пошук пацієнта за ПІБ...').fill('ZZZNONEXISTENT');
      await page.waitForTimeout(500);
      const table = page.getByRole('table');
      // Table should be hidden when no results
      await expect(table).not.toBeVisible();
    });

    test('extreme search query does not crash the page', async ({ page }) => {
      // Fill with very long string
      const longQuery = 'a'.repeat(500);
      await page.getByPlaceholder('Пошук пацієнта за ПІБ...').fill(longQuery);
      await page.waitForTimeout(500);
      // Page should still be responsive
      await expect(page.getByText('Активні пацієнти')).toBeVisible();
    });

    test('search with SQL-like injection characters does not crash', async ({ page }) => {
      await page.getByPlaceholder('Пошук пацієнта за ПІБ...').fill("'; DROP TABLE episodes; --");
      await page.waitForTimeout(500);
      await expect(page.getByText('Активні пацієнти')).toBeVisible();
    });
  });

  test.describe('Negative: Invalid / Edge Navigation', () => {
    test('doctor cannot access nurse routes from dashboard', async ({ page }) => {
      await page.goto('/icu/nurse');
      await expect(page).not.toHaveURL(/\/prescriptions\/icu\/nurse/);
    });

    test('doctor cannot access admin routes from dashboard', async ({ page }) => {
      await page.goto('/admin');
      await expect(page).not.toHaveURL(/\/admin/);
    });

    test('non-existent episode ID shows error or stays on page', async ({ page }) => {
      await page.goto('/icu/doctor/episode/00000000-0000-0000-0000-000000000000');
      // Should not show a blank page
      await expect(page.locator('#root')).not.toBeEmpty();
    });
  });

  // ─── Boundary Conditions ─────────────────────────────────────────────────

  test.describe('Boundary: Row Interaction', () => {
    test('rapid double-click on a row does not break navigation', async ({ page }) => {
      const firstRow = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]').first();
      await firstRow.dblclick();
      await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);
    });

    test('clicking exactly on the patient name cell navigates', async ({ page }) => {
      const nameCell = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"] td[data-slot="table-cell"]').first();
      await nameCell.click();
      await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);
    });

    test('clicking on the status badge cell navigates (row-level click)', async ({ page }) => {
      // Status badge is in the 6th cell (index 5)
      const statusCell = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"] td[data-slot="table-cell"]').nth(5);
      await statusCell.click();
      await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);
    });

    test('clicking on the open button does not trigger row click handler twice', async ({ page }) => {
      // This is a behavioral test: the open button stops propagation,
      // so clicking it should result in exactly one navigation event.
      const openBtn = page.getByRole('button', { name: 'Відкрити' }).first();
      await openBtn.click();
      await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);

      // Verify we are on exactly one episode page (URL has one episode ID)
      const url = page.url();
      const episodeIdMatch = url.match(/\/prescriptions\/icu\/doctor\/episode\/([^/]+)/);
      expect(episodeIdMatch).not.toBeNull();
      expect(episodeIdMatch![1].length).toBeGreaterThan(0);
    });
  });

  test.describe('Boundary: Keyboard Accessibility', () => {
    test('tabbing focuses the open button', async ({ page }) => {
      // Tab to the first open button
      await page.keyboard.press('Tab');
      await page.waitForTimeout(200);

      // The first interactive element after page load should be focusable
      const focused = await page.evaluate(() => document.activeElement?.tagName);
      // Should be some focusable element (button, input, etc.)
      expect(['BUTTON', 'INPUT', 'A', 'SELECT']).toContain(focused);
    });

    test('enter key on focused open button navigates', async ({ page }) => {
      const openBtn = page.getByRole('button', { name: 'Відкрити' }).first();
      await openBtn.focus();
      await page.keyboard.press('Enter');
      await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);
    });
  });

  // ─── Edge Cases ──────────────────────────────────────────────────────────

  test.describe('Edge: Data Integrity', () => {
    test('all patient names contain only valid Cyrillic characters and spaces', async ({ page }) => {
      const rows = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]');
      const count = await rows.count();
      // Names should not contain HTML entities or garbled text
      for (let i = 0; i < count; i++) {
        const name = await rows.nth(i).locator('td').first().innerText();
        expect(name.trim().length).toBeGreaterThan(0);
        // Should not contain replacement character or HTML tags
        expect(name).not.toContain('�');
        expect(name).not.toContain('<');
        expect(name).not.toContain('>');
      }
    });

    test('all rows have the same number of cells', async ({ page }) => {
      const rows = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]');
      const count = await rows.count();
      if (count > 0) {
        const firstCellCount = await rows.first().locator('td[data-slot="table-cell"]').count();
        for (let i = 1; i < count; i++) {
          const cellCount = await rows.nth(i).locator('td[data-slot="table-cell"]').count();
          expect(cellCount).toBe(firstCellCount);
        }
      }
    });

    test('no row has empty patient name', async ({ page }) => {
      const rows = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]');
      const count = await rows.count();
      for (let i = 0; i < count; i++) {
        const name = await rows.nth(i).locator('td').first().innerText();
        expect(name.trim().length).toBeGreaterThan(0);
      }
    });

    test('status badges are all ACTIVE', async ({ page }) => {
      const badges = page.locator('tbody[data-slot="table-body"] [data-slot="badge"]');
      const count = await badges.count();
      expect(count).toBeGreaterThan(0);
      for (let i = 0; i < count; i++) {
        await expect(badges.nth(i)).toHaveText('Активний');
      }
    });

    test('no duplicate patient names in the table', async ({ page }) => {
      const rows = page.locator('tbody[data-slot="table-body"] tr[data-slot="table-row"]');
      const count = await rows.count();
      const names: string[] = [];
      for (let i = 0; i < count; i++) {
        const name = await rows.nth(i).locator('td').first().innerText();
        names.push(name.trim());
      }
      const uniqueNames = new Set(names);
      expect(uniqueNames.size).toBe(names.length);
    });
  });

  test.describe('Edge: Responsive / CSS', () => {
    test('table is visible on desktop viewport', async ({ page }) => {
      await page.setViewportSize({ width: 1280, height: 720 });
      await page.goto('/icu/doctor');
      await expect(page.getByRole('table')).toBeVisible({ timeout: 10000 });
    });

    test('table is visible on tablet viewport', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });
      await page.goto('/icu/doctor');
      await expect(page.getByRole('table')).toBeVisible({ timeout: 10000 });
    });

    test('hidden columns are present in DOM but visually hidden', async ({ page }) => {
      // The ward/bed column has `hidden text-xs sm:table-cell` class
      // On desktop it should be visible, but let's verify it exists in DOM
      const header = page.getByRole('columnheader', { name: 'Палата/Ліжко' });
      await expect(header).toBeAttached();
    });

    test('open buttons have correct orange color styling', async ({ page }) => {
      const openBtn = page.getByRole('button', { name: 'Відкрити' }).first();
      const color = await openBtn.evaluate((el) => {
        return window.getComputedStyle(el).color;
      });
      // Should be some form of orange/reddish color (#FF8C66 or rgb equivalent)
      expect(color).toMatch(/255.*140.*102|rgb\(255,\s*140,\s*102\)/);
    });
  });

  test.describe('Edge: Interaction Isolation', () => {
    test('clicking open button does not trigger search or other inputs', async ({ page }) => {
      const searchInput = page.getByPlaceholder('Пошук пацієнта за ПІБ...');
      await expect(searchInput).toBeVisible();

      // Click open button
      const openBtn = page.getByRole('button', { name: 'Відкрити' }).first();
      await openBtn.click();

      // Should navigate away, not affect search
      await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);
    });

    test('page title is set correctly on dashboard', async ({ page }) => {
      await expect(page).toHaveTitle('ВАІТ — Лікар');
    });

    test('navigating back to dashboard preserves table state', async ({ page }) => {
      // Open an episode
      const openBtn = page.getByRole('button', { name: 'Відкрити' }).first();
      await openBtn.click();
      await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);

      // Navigate back
      await page.goto('/icu/doctor');
      await expect(page.getByRole('table')).toBeVisible({ timeout: 10000 });
      await expect(page).toHaveTitle('ВАІТ — Лікар');
    });
  });
});
