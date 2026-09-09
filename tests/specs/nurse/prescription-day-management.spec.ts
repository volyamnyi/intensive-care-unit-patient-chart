import { test, expect } from '../../fixtures/index';
import { navigateToDetail } from '../../helpers/medication';

// Negative: nurses (read-only role) must NOT see the per-row «Додати день»
// action and must NOT trigger the per-day context menu on right-click.
//
// Dynamic-data contract (real MIS, no seed IDs): beforeEach picks the first
// dept-19/37 patient, guarantees an open list + one real-catalog item (API
// setup authenticates as the doctor test account; the page itself stays the
// nurse storageState), and navigates to the list detail.

const DODATI_DENY = 'Додати день';
const MENU_LABEL = 'Контекстне меню дня';

test.describe('Nurse — prescription day UI is read-only', () => {
  test.beforeEach(async ({ page, request }) => {
    await navigateToDetail(request, page, {
      base: '/prescriptions/nurse',
      expectPath: /\/prescriptions\/nurse\/[0-9a-f-]{36}$/,
    });
    await expect(page.getByText(/Статус: Відкрито/)).toBeVisible({ timeout: 10_000 });
  });

  test('«Додати день» button is not rendered for any item row (nurse gating)', async ({ page }) => {
    await expect(page.getByRole('button', { name: DODATI_DENY })).toHaveCount(0);
  });

  test('«Видалити день» button is not rendered for any item row (nurse gating)', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Видалити день' })).toHaveCount(0);
  });

  test('right-clicking a dose cell does not open the context menu (nurse gating)', async ({ page }) => {
    const firstRow = page.locator('tbody tr').first();
    const cell = firstRow.locator('td').nth(1);
    await cell.click({ button: 'right' });
    await expect(page.getByRole('menu', { name: MENU_LABEL })).toHaveCount(0, { timeout: 2_000 });
  });
});
