import { test, expect } from '../../fixtures/index';

test.describe('Episode Page', () => {
  test('shows all sections on a single screen', async ({ page }) => {
    await page.goto('/icu/doctor/episode/a3333333-3333-3333-3333-333333333333');
    await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);

    // Single ICU card: vitals/losses grid + therapy
    await expect(page.getByText('Показник / година')).toBeVisible();
    await expect(page.getByText('Терапія (призначення)')).toBeVisible();

    // Sidebar collapsible sections (all non-hourly data on one screen)
    await expect(page.getByText('Нотатки')).toBeVisible();
    await expect(page.getByText('Шкали')).toBeVisible();
    await expect(page.getByText('Баланс рідини')).toBeVisible();
  });

  test('single screen has no tab navigation', async ({ page }) => {
    await page.goto('/icu/doctor/episode/a3333333-3333-3333-3333-333333333333');
    await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);

    await expect(page.getByText('Показник / година')).toBeVisible();
    await expect(page.getByRole('tab')).toHaveCount(0);
  });

  test('back button returns to doctor dashboard', async ({ page }) => {
    await page.goto('/icu/doctor/episode/a3333333-3333-3333-3333-333333333333');
    await expect(page).toHaveURL(/\/prescriptions\/icu\/doctor\/episode\//);

    await page.getByRole('button', { name: 'Назад' }).click();
    await expect(page).toHaveURL('/icu/doctor');
  });
});
