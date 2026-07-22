import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a1111111-1111-1111-1111-111111111111';

test.describe('Sign-Off Full Chain', () => {
  test('nurse signs an open clinical day', async ({ nursePage }) => {
    await nursePage.goto(`/nurse/episode/${EPISODE_ID}`);

    await nursePage.getByRole('button', { name: 'Підписати добу' }).click();
    await expect(nursePage.getByText('Після підписання доба стане read-only')).toBeVisible();

    await nursePage.getByRole('button', { name: 'Підписати' }).click();
    await expect(nursePage.getByText('Підписано медсестрою').first()).toBeVisible({ timeout: 10000 });
  });

  test('doctor signs a nurse-signed clinical day', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);
    await expect(page.getByText('Підписано медсестрою').first()).toBeVisible({ timeout: 10000 });

    await page.getByRole('button', { name: 'Підписати добу' }).click();
    await expect(page.getByText('Після підписання доба стане read-only')).toBeVisible();

    await page.getByRole('button', { name: 'Підписати' }).click();
    await expect(page.getByText('Підписано лікарем').first()).toBeVisible({ timeout: 10000 });
  });
});
