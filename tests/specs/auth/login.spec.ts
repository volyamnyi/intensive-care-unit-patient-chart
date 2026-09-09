import { test, expect } from '../../fixtures/index';
import { testUser } from '../../helpers/test-users';

test.describe('Login', () => {
  test('doctor can login with valid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Логін').fill(testUser(1).login);
    await page.getByLabel('Пароль').fill(testUser(1).password);
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/select/);
  });

  test('nurse can login with valid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Логін').fill(testUser(3).login);
    await page.getByLabel('Пароль').fill(testUser(3).password);
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/select/);
  });

  test('invalid credentials show error', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Логін').fill('wrong');
    await page.getByLabel('Пароль').fill('wrong');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page.getByText('Невірний логін або пароль')).toBeVisible();
  });

  test('redirects to /login when unauthenticated', async ({ page }) => {
    await page.goto('/icu/doctor');
    await expect(page).toHaveURL('/login');
  });

  test('login page has correct title', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveTitle('Вхід — Superhumans Lviv');
  });
});
