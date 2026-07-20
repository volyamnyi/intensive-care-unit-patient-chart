import { test, expect } from '../../fixtures/index';

test.describe('Nurse Fluid Balance Full', () => {
  test('fluid balance section shows computed balance', async ({ page }) => {
    await page.goto('/nurse/episode/a3333333-3333-3333-3333-333333333333');
    await expect(page.getByText('Баланс рідини')).toBeVisible();

    await expect(page.getByText(/Надійшло:/)).toBeVisible();
    await expect(page.getByText(/Виділено:/)).toBeVisible();
    await expect(page.getByText(/Добовий баланс:/)).toBeVisible();
    await expect(page.getByText(/Кумулятивний баланс:/)).toBeVisible();
  });

  test('fluid balance displays correct sections', async ({ page }) => {
    await page.goto('/nurse/episode/a3333333-3333-3333-3333-333333333333');
    await expect(page.getByText(/мл/).first()).toBeVisible();
  });
});
