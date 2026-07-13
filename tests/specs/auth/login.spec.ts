import { test, expect } from '../../fixtures/index';

test.describe('Login', () => {
  test('doctor can login with valid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('textbox', { name: /Логін/ }).fill('doctor1');
    await page.getByLabel(/Пароль/).fill('doctor123');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/doctor/);
    await expect(page.getByText('Карта інтенсивної терапії')).toBeVisible();
  });

  test('nurse can login with valid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('textbox', { name: /Логін/ }).fill('nurse1');
    await page.getByLabel(/Пароль/).fill('nurse123');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/nurse/);
    await expect(page.getByText('медсестра')).toBeVisible();
  });

  test('invalid credentials show error', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('textbox', { name: /Логін/ }).fill('wrong');
    await page.getByLabel(/Пароль/).fill('wrong');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page.getByText('Невірний логін або пароль')).toBeVisible();
  });

  test('empty password stays on login page', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('textbox', { name: /Логін/ }).fill('doctor1');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL('/login');
  });

  test('redirects to /login when unauthenticated', async ({ page }) => {
    await page.goto('/doctor');
    await expect(page).toHaveURL('/login');
  });

  test('login page has correct title', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveTitle('ВАІТ — Вхід');
  });
});
