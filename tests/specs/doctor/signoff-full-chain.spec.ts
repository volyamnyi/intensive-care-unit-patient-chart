import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a1111111-1111-1111-1111-111111111111';

test.describe('Sign-Off Full Chain', () => {
  test('nurse signs an open clinical day', async ({ nursePage }) => {
    await nursePage.goto(`/nurse/episode/${EPISODE_ID}`);
    const signBtn = nursePage.getByRole('button', { name: 'Підписати добу' });
    const existingSign = nursePage.getByText('Підписано медсестрою').first();

    if (await existingSign.isVisible().catch(() => false)) {
      return;
    }
    if (!(await signBtn.isVisible().catch(() => false))) {
      return;
    }

    await signBtn.click();
    await expect(nursePage.getByText('Після підписання доба стане read-only')).toBeVisible();
    await nursePage.getByRole('button', { name: 'Підписати' }).click();
    await expect(existingSign).toBeVisible({ timeout: 10000 });
  });

  test('doctor signs a nurse-signed clinical day', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);
    const signBtn = page.getByRole('button', { name: 'Підписати добу' });
    const nurseSigned = page.getByText('Підписано медсестрою').first();
    const doctorSigned = page.getByText('Підписано лікарем').first();

    if (await doctorSigned.isVisible().catch(() => false)) {
      return;
    }

    await expect(nurseSigned).toBeVisible({ timeout: 10000 });
    if (!(await signBtn.isVisible().catch(() => false))) {
      return;
    }

    await signBtn.click();
    await expect(page.getByText('Після підписання доба стане read-only')).toBeVisible();
    await page.getByRole('button', { name: 'Підписати' }).click();
    await expect(doctorSigned).toBeVisible({ timeout: 10000 });
  });
});
