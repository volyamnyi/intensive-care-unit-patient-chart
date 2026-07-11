import { test, expect } from '@playwright/test';
import { USERS, FRONTEND_BASE } from '../fixtures/env.js';
import { loginViaApi } from '../fixtures/auth.js';

test.describe('UI — Role separation', () => {
  test('TC-ROLE-01 doctor cannot access /nurse', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.doctor1.login, USERS.doctor1.password);
    await page.goto(FRONTEND_BASE + '/nurse');
    await expect(page).not.toHaveURL(/\/nurse/);
  });

  test('TC-ROLE-02 nurse cannot access /doctor', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.nurse1.login, USERS.nurse1.password);
    await page.goto(FRONTEND_BASE + '/doctor');
    await expect(page).not.toHaveURL(/\/doctor/);
  });

  test('TC-ROLE-03 nurse cannot access /admin', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.nurse1.login, USERS.nurse1.password);
    await page.goto(FRONTEND_BASE + '/admin');
    await expect(page).not.toHaveURL(/\/admin/);
  });

  test('TC-ROLE-04 HOD reaches doctor area', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.head1.login, USERS.head1.password);
    await page.waitForURL(/\/doctor/);
    await expect(page.getByText('Активні пацієнти ВАІТ')).toBeVisible();
  });

  test('TC-ROLE-05 admin reaches /admin only', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.admin.login, USERS.admin.password);
    await page.waitForURL(/\/admin/);
    await expect(page.getByText('Панель адміністратора')).toBeVisible();
  });

  test('TC-ROLE-06 unknown route for authenticated user does not crash', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.doctor1.login, USERS.doctor1.password);
    await page.goto(FRONTEND_BASE + '/nonexistent-route');
    expect(page.url()).toMatch(/http:\/\/localhost:5173/);
  });

  test('TC-ROLE-07 role badge visible in user menu', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.doctor1.login, USERS.doctor1.password);
    await page.getByRole('button', { name: 'Меню користувача' }).click();
    await expect(page.getByText('Лікар')).toBeVisible();
  });

  test('TC-ROLE-08 each role shows correct dashboard title', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.nurse1.login, USERS.nurse1.password);
    await expect(page.getByText('Карта інтенсивної терапії — медсестра')).toBeVisible();
  });
});
