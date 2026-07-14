import { test, expect } from '../../fixtures/index';

test.describe('Scales Tab', () => {
  test('scales tab shows scale panel', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    await page.getByRole('tab', { name: 'Шкали' }).click();
    await expect(page.getByText('Немає даних шкал').or(page.getByText('Не заповнено'))).toBeVisible();
  });
});
