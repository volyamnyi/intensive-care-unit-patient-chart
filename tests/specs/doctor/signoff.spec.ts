import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a2222222-2222-2222-2222-222222222222';

test.describe('Sign Off Day', () => {
  test('opens sign dialog and shows sign confirmation', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    await page.getByRole('button', { name: 'Підписати добу' }).click();
    await expect(page.getByText('Після підписання доба стане read-only')).toBeVisible({ timeout: 10000 });
  });
});
