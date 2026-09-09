import { test, expect } from '../../fixtures/index';
import { testUser } from '../../helpers/test-users';

test.describe('Logout', () => {
  test('doctor can logout via user menu', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Логін').fill(testUser(1).login);
    await page.getByLabel('Пароль').fill(testUser(1).password);
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
    await page.getByLabel('Логін').fill(testUser(3).login);
    await page.getByLabel('Пароль').fill(testUser(3).password);
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
    await page.getByLabel('Логін').fill(testUser(1).login);
    await page.getByLabel('Пароль').fill(testUser(1).password);
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
    await page.getByLabel('Логін').fill(testUser(6).login);
    await page.getByLabel('Пароль').fill(testUser(6).password);
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/select/);
    await page.goto('/admin');
    await expect(page).toHaveURL(/\/admin/);

    await page.getByRole('button', { name: 'Меню користувача' }).click();
    await page.getByRole('menuitem', { name: 'Вийти' }).click();

    await expect(page).toHaveURL('/login');
  });
});
