import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

test.describe('Doctor Lab Results', () => {
  test('shows lab results section on the unified card', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);
    const lab = page.getByText('Лабораторні результати').first();
    await expect(lab).toBeVisible();
    await lab.click();
    await expect(page.getByText('Лабораторні результати')).toBeVisible();
  });
});
