import { test, expect } from '../../fixtures/index';

// Negative: nurses (read-only role) must NOT see the per-row «Додати день»
// action and must NOT trigger the per-day context menu on right-click.

const DODATI_DENY = 'Додати день';
const MENU_LABEL = 'Контекстне меню дня';

test.describe('Nurse — prescription day UI is read-only', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/prescriptions/nurse');
    await page.getByPlaceholder('Пошук пацієнта').fill('1003');
    const row = page.locator('tr').filter({ hasText: 'В ході' }).first();
    await expect(row).toBeVisible({ timeout: 10_000 });
    await row.getByRole('button', { name: 'Відкрити' }).click();
    await expect(page.getByText('Листки призначень (')).first().toBeVisible({ timeout: 10_000 });
    const card = page.locator('div.rounded-xl.border', { hasText: 'В ході' }).first();
    await card.getByRole('button', { name: /Листок/ }).first().click();
    await page.waitForURL(/\/prescriptions\/nurse\/[0-9a-f-]{36}$/, { timeout: 15_000 });
    await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10_000 });
    await expect(page.getByText(/Статус: Відкрито/)).toBeVisible({ timeout: 10_000 });
  });

  test('«Додати день» button is not rendered for any item row (nurse gating)', async ({ page }) => {
    await expect(page.getByRole('button', { name: DODATI_DENY })).toHaveCount(0);
  });

  test('right-clicking a dose cell does not open the context menu (nurse gating)', async ({ page }) => {
    const firstRow = page.locator('tbody tr').first();
    const cell = firstRow.locator('td').nth(1);
    await cell.click({ button: 'right' });
    await expect(page.getByRole('menu', { name: MENU_LABEL })).toHaveCount(0, { timeout: 2_000 });
  });
});
