import { test, expect } from '../../fixtures/index';

test.describe('Nurse Fluid Balance Full', () => {
  test('recalculate button computes fluid balance', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);

    await page.getByRole('tab', { name: 'Баланс рідини' }).click();
    await expect(page.getByRole('heading', { name: 'Баланс рідини' })).toBeVisible();

    await page.getByRole('button', { name: 'Перерахувати' }).click();
    await expect(page.getByText(/Надійшло:/)).toBeVisible();
    await expect(page.getByText(/Виділено:/)).toBeVisible();
    await expect(page.getByText(/Добовий баланс:/)).toBeVisible();
    await expect(page.getByText(/Кумулятивний баланс:/)).toBeVisible();
  });

  test('fluid balance displays correct sections', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);

    await page.getByRole('tab', { name: 'Баланс рідини' }).click();
    await expect(page.getByText(/мл/).first()).toBeVisible();
  });
});
