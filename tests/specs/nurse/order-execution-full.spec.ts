import { test, expect } from '../../fixtures/index';

test.describe('Nurse Order Execution', () => {
  test('therapy section shows orders for nurse', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/nurse\/episode\//);

    await expect(page.getByText('Терапія (призначення)')).toBeVisible();
    await expect(
      page.getByText(/мл|mcg|mg|г|Од\.|Препарат/).first()
        .or(page.getByText('Немає призначень'))
    ).toBeVisible();
  });
});
