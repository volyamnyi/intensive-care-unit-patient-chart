import { test, expect } from '../../fixtures/index';

test.describe('Nurse Order Execution', () => {
  test('therapy section shows orders for nurse', async ({ page }) => {
    await page.goto('/icu/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/prescriptions\/icu\/nurse\/episode\//);

    await expect(page.getByText('Терапія (призначення)')).toBeVisible();
  });
});
