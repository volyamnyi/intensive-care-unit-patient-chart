import { test, expect } from '@playwright/test';
import { USERS, FRONTEND_BASE } from '../fixtures/env.js';
import { loginViaApi } from '../fixtures/auth.js';

async function clearAndGotoLogin(page) {
  await page.goto(FRONTEND_BASE + '/');
  await page.evaluate(() => localStorage.clear());
  await page.goto(FRONTEND_BASE + '/login');
  await page.waitForLoadState('networkidle');
}

test.describe('UI — Authentication & login', () => {
  test('TC-AUTH-01 login page renders form', async ({ page }) => {
    await clearAndGotoLogin(page);
    await expect(page.getByLabel('Логін')).toBeVisible();
    await expect(page.getByLabel('Пароль')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Увійти' })).toBeVisible();
  });

  test('TC-AUTH-02 valid doctor login redirects to dashboard', async ({ page }) => {
    await clearAndGotoLogin(page);
    await page.getByLabel('Логін').fill(USERS.doctor1.login);
    await page.getByLabel('Пароль').fill(USERS.doctor1.password);
    await page.getByRole('button', { name: 'Увійти' }).click();
    await page.waitForURL(/\/doctor/, { timeout: 15000 });
    await expect(page.getByText('Активні пацієнти ВАІТ')).toBeVisible();
  });

  test('TC-AUTH-03 invalid password shows error', async ({ page }) => {
    await clearAndGotoLogin(page);
    await page.getByLabel('Логін').fill(USERS.doctor1.login);
    await page.getByLabel('Пароль').fill('wrong-password');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page.getByText('Невірний логін або пароль')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test('TC-AUTH-04 nurse login lands on nurse dashboard', async ({ page }) => {
    const { role } = await loginViaApi(page, page.request, USERS.nurse1.login, USERS.nurse1.password);
    expect(role).toBe('NURSE');
    await page.waitForURL(/\/nurse/);
    await expect(page.getByText('Карта інтенсивної терапії — медсестра')).toBeVisible();
  });

  test('TC-AUTH-05 HOD login lands on doctor area', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.head1.login, USERS.head1.password);
    await page.waitForURL(/\/doctor/);
    await expect(page.getByText('Активні пацієнти ВАІТ')).toBeVisible();
  });

  test('TC-AUTH-06 admin login lands on admin panel', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.admin.login, USERS.admin.password);
    await page.waitForURL(/\/admin/);
    await expect(page.getByText('Панель адміністратора')).toBeVisible();
  });

  test('TC-AUTH-07 unauthenticated access to /doctor redirects to /login', async ({ page }) => {
    await clearAndGotoLogin(page);
    await page.goto(FRONTEND_BASE + '/doctor');
    await page.waitForURL(/\/login/);
  });

  test('TC-AUTH-08 logout clears session and returns to login', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.doctor1.login, USERS.doctor1.password);
    await page.waitForURL(/\/doctor/);
    await page.getByRole('button', { name: 'Меню користувача' }).click();
    await page.getByText('Вийти').click();
    await page.waitForURL(/\/login/);
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeNull();
  });
});
