import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a1111111-1111-1111-1111-111111111111';

test.describe('Sign Off Day', () => {
  test('opens sign dialog and shows sign confirmation', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    await page.getByRole('button', { name: 'Підписати добу' }).click();
    await expect(page.getByText('Після підписання доба стане read-only')).toBeVisible({ timeout: 10000 });
  });
});
