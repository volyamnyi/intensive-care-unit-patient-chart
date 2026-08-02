import { test, expect } from '../../fixtures/index';

test.describe('Access Control', () => {
  test('admin redirected to /admin when accessing /nurse', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.getByText('Користувачі системи')).toBeVisible({ timeout: 10000 });

    await page.goto('/icu/nurse');
    await expect(page).toHaveURL('/admin');
  });

  test('admin redirected to /admin when accessing /doctor', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.getByText('Користувачі системи')).toBeVisible({ timeout: 10000 });

    await page.goto('/icu/doctor');
    await expect(page).toHaveURL('/admin');
  });

  test('doctor can access direct /doctor/create-card route', async ({ page }) => {
    await page.goto('/icu/doctor/create-card');
    await expect(page.getByText('Нова карта інтенсивної терапії')).toBeVisible();
  });

  test('nurse can access direct /nurse/episode/:id route', async ({ page }) => {
    await page.goto('/icu/nurse');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    const episodeUrl = page.url();

    await page.goto('/login');
    await page.getByLabel('Логін').fill('nurse1');
    await page.getByLabel('Пароль').fill('nurse123');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/icu\/nurse/);

    await page.goto(episodeUrl);
    await expect(page).toHaveURL(/\/icu\/nurse\/episode\//);
    await expect(page.getByText('Показник / година')).toBeVisible();
  });
});
