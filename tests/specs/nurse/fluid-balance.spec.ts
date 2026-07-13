import { test, expect } from '../../fixtures/index';

test.describe('Nurse Fluid Balance', () => {
  test('balance tab shows fluid balance components', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);

    await page.getByRole('tab', { name: 'Баланс рідини' }).click();
    await expect(page.getByText('Баланс рідини')).toBeVisible();
    await expect(page.getByText(/Надійшло:/)).toBeVisible();
    await expect(page.getByText(/Виділено:/)).toBeVisible();
    await expect(page.getByText(/Добовий баланс:/)).toBeVisible();
  });
});
