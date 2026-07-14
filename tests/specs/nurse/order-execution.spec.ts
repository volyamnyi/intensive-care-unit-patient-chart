import { test, expect } from '../../fixtures/index';

test.describe('Nurse Order Execution', () => {
  test('sees prescriptions tab with active orders', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);

    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByRole('columnheader', { name: 'Препарат' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Статус' })).toBeVisible();
  });
});
