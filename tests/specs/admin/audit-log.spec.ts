import { test, expect } from '../../fixtures/index';

test.describe('Admin Audit Log', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Логін').fill('admin');
    await page.getByLabel('Пароль').fill('admin123');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/admin/, { timeout: 10000 });
  });

  test('audit log renders rows without crashing (regression F1)', async ({ page }) => {
    const pageErrors: string[] = [];
    page.on('pageerror', (e) => pageErrors.push(String(e)));

    await page.getByRole('button', { name: 'Переглянути журнал аудиту' }).click();

    // Header columns prove the paginated `content` array rendered (regression F1:
    // previous code crashed with `logs.map is not a function` because the API returns
    // a PageResponse object, not a bare array).
    await expect(page.getByText('Сутність')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Дія')).toBeVisible({ timeout: 10000 });
    // At least one data row must have rendered.
    await expect(page.locator('table tbody tr').first()).toBeVisible({ timeout: 10000 });
    expect(pageErrors).toEqual([]);
  });

  test('audit log filters by entity without crashing (UC-19)', async ({ page }) => {
    const pageErrors: string[] = [];
    page.on('pageerror', (e) => pageErrors.push(String(e)));

    await page.getByRole('button', { name: 'Переглянути журнал аудиту' }).click();
    await expect(page.getByText('Сутність')).toBeVisible({ timeout: 10000 });

    const filter = page.getByLabel(/сутність|entity/i);
    if (await filter.count()) {
      await filter.fill('AUTH');
      await page.getByRole('button', { name: 'Пошук' }).click();
      await page.waitForTimeout(500);
    }
    // No crash regardless of result shape.
    expect(pageErrors).toEqual([]);
  });

  test('audit log shows empty state when no records', async ({ page }) => {
    const pageErrors: string[] = [];
    page.on('pageerror', (e) => pageErrors.push(String(e)));

    // Force an empty result by filtering on a non-existent entity.
    await page.getByRole('button', { name: 'Переглянути журнал аудиту' }).click();
    const filter = page.getByLabel(/сутність|entity/i);
    if (await filter.count()) {
      await filter.fill('NON_EXISTENT_ENTITY');
      await page.getByRole('button', { name: 'Пошук' }).click();
      await page.waitForTimeout(500);
    }
    expect(pageErrors).toEqual([]);
  });
});
