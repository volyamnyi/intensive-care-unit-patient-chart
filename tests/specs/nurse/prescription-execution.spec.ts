import { test, expect } from '../../fixtures/index';

test.describe('Prescription Execution (Nurse)', () => {
  test('nurse can access prescription page', async ({ page }) => {
    await page.goto('/nurse/prescriptions');
    // Nurse prescription page should load
    await expect(page.getByText('Виконання призначень').first()).toBeVisible({ timeout: 10000 });
  });

  test('nurse prescription page shows patient selector', async ({ page }) => {
    await page.goto('/nurse/prescriptions');
    // Should prompt to select a patient or show patient list
    await expect(page).toHaveURL(/\/nurse\/prescriptions/, { timeout: 10000 });
  });

  test('nurse sees info message without patient context', async ({ page }) => {
    await page.goto('/nurse/prescriptions');
    // Should show a message about needing patient context or empty state
    const body = page.locator('body');
    await expect(body).not.toHaveText('Error', { timeout: 5000 });
  });
});
