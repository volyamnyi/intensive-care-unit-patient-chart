import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

test.describe('Patient State Section', () => {
  test('shows patient state section on the unified card', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);
    const state = page.getByText('Стан пацієнта').first();
    await expect(state).toBeVisible();
    await state.click();
    await expect(page.getByText('Стан пацієнта')).toBeVisible();
  });
});
