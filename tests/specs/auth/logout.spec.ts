import { test, expect } from '../../fixtures/index';

test.describe('Logout', () => {
  test('doctor can logout via user menu', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Логін').fill('doctor1');
    await page.getByLabel('Пароль').fill('doctor123');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/select/);
    await page.goto('/icu/doctor');
    await expect(page).toHaveURL(/\/icu\/doctor/);

    await page.getByRole('button', { name: 'Меню користувача' }).click();
    await page.getByRole('menuitem', { name: 'Вийти' }).click();

    await expect(page).toHaveURL('/login');
  });

  test('nurse can logout via user menu', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Логін').fill('nurse1');
    await page.getByLabel('Пароль').fill('nurse123');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/select/);
    await page.goto('/icu/nurse');
    await expect(page).toHaveURL(/\/icu\/nurse/);

    await page.getByRole('button', { name: 'Меню користувача' }).click();
    await page.getByRole('menuitem', { name: 'Вийти' }).click();

    await expect(page).toHaveURL('/login');
  });

  test('protected route redirects to login after logout', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Логін').fill('doctor1');
    await page.getByLabel('Пароль').fill('doctor123');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/select/);
    await page.goto('/icu/doctor');
    await expect(page).toHaveURL(/\/icu\/doctor/);

    await page.getByRole('button', { name: 'Меню користувача' }).click();
    await page.getByRole('menuitem', { name: 'Вийти' }).click();
    await expect(page).toHaveURL('/login');

    await page.goto('/icu/doctor');
    await expect(page).toHaveURL('/login');
  });

  test('admin can logout via user menu', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Логін').fill('admin');
    await page.getByLabel('Пароль').fill('admin123');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/select/);
    await page.goto('/admin');
    await expect(page).toHaveURL(/\/admin/);

    await page.getByRole('button', { name: 'Меню користувача' }).click();
    await page.getByRole('menuitem', { name: 'Вийти' }).click();

    await expect(page).toHaveURL('/login');
  });
});
