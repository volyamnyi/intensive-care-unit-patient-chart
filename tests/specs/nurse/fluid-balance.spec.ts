import { test, expect } from '../../fixtures/index';

test.describe('Nurse Fluid Balance', () => {
  test('balance section shows fluid balance components', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);

    await expect(page.getByText('Баланс рідини')).toBeVisible();
    await expect(page.getByText(/Надійшло:/)).toBeVisible();
    await expect(page.getByText(/Виділено:/)).toBeVisible();
    await expect(page.getByText(/Добовий баланс:/)).toBeVisible();
  });
});
