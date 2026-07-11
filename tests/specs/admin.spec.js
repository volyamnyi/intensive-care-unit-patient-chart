import { test, expect } from '@playwright/test';
import { USERS, FRONTEND_BASE } from '../fixtures/env.js';
import { loginViaApi } from '../fixtures/auth.js';

test.describe('UI — Administrator', () => {
  test('TC-ADM-01 admin lands on admin panel', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.admin.login, USERS.admin.password);
    await page.waitForURL(/\/admin/);
    await expect(page.getByText('Панель адміністратора')).toBeVisible();
  });

  test('TC-ADM-02 admin sees doctor and nurse lists', async ({ page }) => {
    const { fullName, role } = await loginViaApi(page, page.request, USERS.admin.login, USERS.admin.password);
    expect(role).toBe('ADMINISTRATOR');
    expect(fullName).toBe(USERS.admin.fullName);
    await page.waitForURL(/\/admin/);
    await expect(page.getByText('Лікарі')).toBeVisible();
    await expect(page.getByText('Медсестри')).toBeVisible();
    await expect(page.getByText('Олександр Мельник')).toBeVisible();
  });

  test('TC-ADM-03 admin table shows all columns', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.admin.login, USERS.admin.password);
    await page.waitForURL(/\/admin/);
    await expect(page.getByText('ПІБ').first()).toBeVisible();
    await expect(page.getByText('Логін').first()).toBeVisible();
    await expect(page.getByText('Роль').first()).toBeVisible();
    await expect(page.getByText('Email').first()).toBeVisible();
  });

  test('TC-ADM-04 non-admin cannot reach /admin', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.doctor1.login, USERS.doctor1.password);
    await page.goto(FRONTEND_BASE + '/admin');
    await expect(page).not.toHaveURL(/\/admin/);
  });

  test('TC-ADM-05 admin user menu accessible', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.admin.login, USERS.admin.password);
    await page.waitForURL(/\/admin/);
    await page.getByRole('button', { name: 'Меню користувача' }).click();
    await expect(page.getByText('Вийти')).toBeVisible();
  });
});
