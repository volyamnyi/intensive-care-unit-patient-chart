import { test, expect, type Page } from '../../fixtures/index';

// E2E for per-item prescription day management (issue #169, phase 4).
// Round-trips: adds a new day (max_day_date + 1), then deletes the same day
// via the context menu — net-zero, no seed pollution.

const DODATI_DENY = 'Додати день';
const MENU_LABEL = 'Контекстне меню дня';
const MENU_REMOVE_DAY = 'Видалити цей день';

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

async function gotoDay(page: Page, iso: string): Promise<void> {
  const label = formatUa(iso);
  const header = page.locator('th', { hasText: label });
  if (await header.first().isVisible({ timeout: 500 }).catch(() => false)) return;
  const shiftRight = page.locator('button:has(.lucide-chevron-right)');
  for (let i = 0; i < 8; i++) {
    if (await header.first().isVisible({ timeout: 200 }).catch(() => false)) return;
    if (await shiftRight.isDisabled().catch(() => true)) break;
    await shiftRight.click();
    await page.waitForTimeout(150);
  }
  await expect(header.first()).toBeVisible({ timeout: 2_000 });
}

test.describe('Doctor — prescription day add + remove (UI)', () => {
  test.beforeAll(async ({ request }) => {
    const res = await request.post('http://localhost:8085/api/auth/login', {
      data: { login: 'doctor1', password: 'doctor123' },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    const token = body.token as string;
    await request.post('http://localhost:8085/api/mis/error-mode?mode=none', {
      headers: { Authorization: `Bearer ${token}` },
    });
  });

  test.beforeEach(async ({ page }) => {
    await navigateToDetail(page);
  });

  test('per-row "Додати день" buttons appear on every item row', async ({ page }) => {
    const addBtns = page.getByRole('button', { name: DODATI_DENY });
    await expect(addBtns.first()).toBeVisible({ timeout: 5_000 });
    const rows = page.locator('tbody tr');
    const expected = await rows.count();
    await expect(addBtns).toHaveCount(expected);
  });

  test('adds a new day then deletes it via context menu (round-trip)', async ({ page }) => {
    // Capture the last visible day header BEFORE adding, so we can assert
    // both that the new day appears and that it disappears after removal.
    const firstRow = page.locator('tbody tr').first();
    const beforeCount = await page.locator('tbody tr').count();
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
    await gotoDay(page, addedDate);
    const dateLabel = formatUa(addedDate);
    const thead = page.locator('thead tr').first();
    const dateColIdx = await thead.locator('th[colspan]').evaluateAll(
      (ths, target) => ths.findIndex(t => t.textContent?.trim() === target),
      dateLabel,
    );
    expect(dateColIdx).toBeGreaterThanOrEqual(0);

    const cell = firstRow.locator('td').nth(1 + dateColIdx * 4);
    await cell.click({ button: 'right' });
    const menu = page.getByRole('menu', { name: MENU_LABEL });
    await expect(menu).toBeVisible();
    // The added day is unplanned/uncompleted → no "Скасувати дозу" expected.
    await expect(menu.getByRole('menuitem', { name: 'Скасувати дозу' })).toHaveCount(0);
    await expect(menu.getByRole('menuitem', { name: MENU_REMOVE_DAY })).toBeVisible();

    const [delRes] = await Promise.all([
      page.waitForResponse(r => /\/items\/[0-9a-f-]+\/days\/[0-9a-f-]+$/.test(r.url()) && r.request().method() === 'DELETE'),
      menu.getByRole('menuitem', { name: MENU_REMOVE_DAY }).click(),
    ]);
    expect(delRes.status()).toBe(204);

    // The new day is gone from the header row.
    await expect(page.locator('th', { hasText: dateLabel })).toHaveCount(0, { timeout: 5_000 });
  });
});
