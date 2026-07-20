import { test, expect } from '../../fixtures/index';

test.describe('Scales Section', () => {
  test('scales section shows scale panel in sidebar', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    const scalesSection = page.getByText('Шкали').first();
    await scalesSection.click();
    await expect(page.getByText('Немає даних шкал').or(page.getByText('Не заповнено'))).toBeVisible();
  });
});
