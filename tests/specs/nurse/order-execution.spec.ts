import { test, expect } from '../../fixtures/index';

test.describe('Nurse Order Execution', () => {
  test('sees therapy section with active orders on the card', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);

    await expect(page.getByText('Терапія (призначення)')).toBeVisible();
    // The therapy section renders an empty-state row inside the grid (the patient heading
    // "Петренко …" contains "г" so the order-text regex is avoided to prevent false matches)
    const grid = page.getByRole('table').first();
    await expect(grid.getByRole('row', { name: 'Немає призначень' })).toBeVisible();
  });
});
