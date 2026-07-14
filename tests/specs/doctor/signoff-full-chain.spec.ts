import { test, expect } from '../../fixtures/index';

const DOCTOR_EPISODE_ID = 'a1111111-1111-1111-1111-111111111111';

test.describe('Sign-Off Full Chain', () => {
  test('nurse signs an open clinical day', async ({ nursePage }) => {
    await nursePage.goto('/nurse');
    await nursePage.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(nursePage).toHaveURL(/\/nurse\/episode\//);

    await nursePage.getByRole('button', { name: 'Підписати добу' }).click();
    await expect(nursePage.getByText('Після підписання доба стане read-only')).toBeVisible();

    await nursePage.getByRole('button', { name: 'Підписати' }).click();
    await expect(nursePage.getByText('Підписана медсестрою').first()).toBeVisible({ timeout: 10000 });
  });

  test('doctor signs a nurse-signed clinical day', async ({ page }) => {
    await page.goto(`/doctor/episode/${DOCTOR_EPISODE_ID}`);
    await expect(page.getByText('Підписана медсестрою').first()).toBeVisible({ timeout: 10000 });

    await page.getByRole('button', { name: 'Підписати добу' }).click();
    await expect(page.getByText('Після підписання доба стане read-only')).toBeVisible();

    await page.getByRole('button', { name: 'Підписати' }).click();
    await expect(page.getByText('Підписана').first()).toBeVisible({ timeout: 10000 });
  });
});
