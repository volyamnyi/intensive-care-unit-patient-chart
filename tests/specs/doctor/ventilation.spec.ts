import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

test.describe('Ventilation Section', () => {
  test('shows ventilation section on the unified card', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);
    const ventilation = page.getByText('ШВЛ').first();
    await expect(ventilation).toBeVisible();
    await ventilation.click();
    await expect(page.getByText('ШВЛ')).toBeVisible();
  });
});
