import { test, expect } from '../../fixtures/index';
import type { Locator, Page } from '@playwright/test';

// E2E for per-item prescription day management (issue #169, phase 4;
// reshaped by issue #223, phase 3: whole-day deletion left the cell menu,
// so this spec adds a day via «+», asserts the menu offers no whole-day
// delete, and removes the day via API — net-zero, no seed pollution).

const API = 'http://localhost:8085/api';
const DODATI_DENY = 'Додати день';
const MENU_LABEL = 'Контекстне меню дня';
const MENU_WHOLE_DAY_DELETE = 'Видалити цей день';

function formatUa(iso: string): string {
  return new Date(iso).toLocaleDateString('uk-UA', { day: '2-digit', month: 'short' });
}

async function navigateToDetail(page: Page): Promise<void> {
  await page.goto('/prescriptions/doctor');
  await page.getByPlaceholder('Пошук пацієнта').fill('1003');
  const row = page.locator('tr').filter({ hasText: 'В ході' }).first();
  await expect(row).toBeVisible({ timeout: 10_000 });
  await row.getByRole('button', { name: 'Відкрити' }).click();
  await expect(page.getByText('Листки призначень (').first()).toBeVisible({ timeout: 10_000 });
  const card = page.locator('div.rounded-xl.border', { hasText: 'В ході' }).first();
  await card.getByRole('button', { name: /Листок/ }).first().click();
  await page.waitForURL(/\/prescriptions\/doctor\/[0-9a-f-]{36}$/, { timeout: 15_000 });
  await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10_000 });
  await expect(page.getByText(/Статус: Відкрито/)).toBeVisible({ timeout: 10_000 });
}

// The detail page renders TWO tables (item grid AND vital-sign grid), so page-wide
// `tbody tr` / `th` locators are ambiguous. Scope every grid assertion to the items
// table — uniquely identified by its «Препарат / Метод» header (vital grid uses
// «Показник») — and wait for a «Додати день» button first so the non-retrying
// .count() never races the loading spinner.
function itemsTable(page: Page): Locator {
  return page.locator('table').filter({ hasText: 'Препарат / Метод' });
}

async function scopeGrid(page: Page): Promise<Locator> {
  await expect(page.getByRole('button', { name: DODATI_DENY }).first()).toBeVisible({ timeout: 10_000 });
  return itemsTable(page);
}

async function gotoDay(page: Page, grid: Locator, iso: string): Promise<void> {
  const label = formatUa(iso);
  const header = grid.locator('th', { hasText: label });
  if (await header.first().isVisible({ timeout: 500 }).catch(() => false)) return;
  // Items grid renders before the vital grid, so its chevron is the first in DOM order.
  const shiftRight = page.locator('button:has(.lucide-chevron-right)').first();
  for (let i = 0; i < 8; i++) {
    if (await header.first().isVisible({ timeout: 200 }).catch(() => false)) return;
    if (await shiftRight.isDisabled().catch(() => true)) break;
    await shiftRight.click();
    await page.waitForTimeout(150);
  }
  await expect(header.first()).toBeVisible({ timeout: 2_000 });
}

test.describe('Doctor — prescription day add + remove (UI)', () => {
  let doctorToken = '';
  test.beforeAll(async ({ request }) => {
    const res = await request.post('http://localhost:8085/api/auth/login', {
      data: { login: 'doctor1', password: 'doctor123' },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    const token = body.token as string;
    doctorToken = token;
    await request.post('http://localhost:8085/api/mis/error-mode?mode=none', {
      headers: { Authorization: `Bearer ${token}` },
    });
  });

  test.beforeEach(async ({ page }) => {
    await navigateToDetail(page);
  });

  test('per-row "Додати день" buttons appear on every item row', async ({ page }) => {
    const grid = await scopeGrid(page);
    const addBtns = page.getByRole('button', { name: DODATI_DENY });
    const itemRows = grid.locator('tbody tr');
    const expected = await itemRows.count();
    expect(expected).toBeGreaterThan(0);
    await expect(addBtns).toHaveCount(expected);
  });

  test('adds a new day via «+»; cell menu offers no whole-day delete (API cleanup)', async ({ page, request }) => {
    const grid = await scopeGrid(page);
    const itemRows = grid.locator('tbody tr');
    const firstRow = itemRows.first();
    const beforeCount = await itemRows.count();
    expect(beforeCount).toBeGreaterThan(0);

    const [addRes] = await Promise.all([
      page.waitForResponse(r => r.url().includes('/items/') && r.url().endsWith('/days') && r.request().method() === 'POST'),
      firstRow.getByRole('button', { name: DODATI_DENY }).click(),
    ]);
    expect(addRes.status()).toBe(201);
    const body = await addRes.json();
    const addedDate = body.dayParts
      .map((p: { dayDate: string }) => p.dayDate)
      .sort()
      .at(-1) as string;
    expect(addedDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    // Only the NEWLY-ADDED day must be unplanned + uncompleted.
    const newParts = (body.dayParts as { dayDate: string; period: string; isPlanned: boolean; isCompleted: boolean; isCompletedFinished: boolean }[])
      .filter(p => p.dayDate === addedDate);
    expect(newParts).toHaveLength(4);
    for (const part of newParts) {
      expect(part.isPlanned).toBe(false);
      expect(part.isCompleted).toBe(false);
      expect(part.isCompletedFinished).toBe(false);
    }
    const periods = new Set(newParts.map(p => p.period));
    expect(periods).toEqual(new Set(['morning', 'day', 'evening', 'night']));

    // Navigate to the new day and right-click its first period cell.
    await gotoDay(page, grid, addedDate);
    const dateLabel = formatUa(addedDate);
    const thead = grid.locator('thead tr').first();
    const dateColIdx = await thead.locator('th[colspan]').evaluateAll(
      (ths, target) => ths.findIndex(t => t.textContent?.trim() === target),
      dateLabel,
    );
    expect(dateColIdx).toBeGreaterThanOrEqual(0);

    const cell = firstRow.locator('td').nth(1 + dateColIdx * 4);
    await cell.click({ button: 'right', position: { x: 50, y: 16 } });
    const menu = page.getByRole('menu', { name: MENU_LABEL });
    await expect(menu).toBeVisible();
    // The added day is unplanned/uncompleted → no "Відмінити препарат" expected.
    await expect(menu.getByRole('menuitem', { name: 'Відмінити препарат' })).toHaveCount(0);
    // Phase 3 removed whole-day deletion from the cell menu (it returns as the
    // row-level «−» button in Phase 4).
    await expect(menu.getByRole('menuitem', { name: MENU_WHOLE_DAY_DELETE })).toHaveCount(0);
    await expect(menu.getByRole('menuitem', { name: 'Відмінити це призначення' })).toHaveCount(0);
    await page.keyboard.press('Escape');
    await expect(menu).toBeHidden({ timeout: 5_000 });

    // Net-zero cleanup via API (row-level «−» arrives in Phase 4).
    const itemId = body.id as string;
    const addedDay = (body.dayParts as { dayDate: string; dayId: string }[]).find(p => p.dayDate === addedDate);
    expect(addedDay).toBeTruthy();
    if (!addedDay) throw new Error('added day missing in add-day response');
    const delRes = await request.delete(
      `${API}/prescriptions/items/${itemId}/days/${addedDay.dayId}`,
      { headers: { Authorization: `Bearer ${doctorToken}` } },
    );
    expect(delRes.status()).toBe(204);
  });
});
