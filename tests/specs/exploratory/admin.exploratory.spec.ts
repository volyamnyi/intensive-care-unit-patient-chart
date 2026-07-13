import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/LoginPage';
import { AdminPage } from '../../pages/AdminPage';

test.describe('Admin: Exploratory Testing', () => {

  test('ADM-LOGIN-001: Login as admin with valid credentials', async ({ page }) => {
    const login = new LoginPage(page);
    const admin = new AdminPage(page);
    await login.navigate();
    await login.expectLoginFormVisible();
    await login.loginAs('admin', 'admin123');
    await admin.expectUrl(/\/admin/);
    await expect(admin.appBarTitle).toBeVisible({ timeout: 10000 });
    await expect(admin.sectionTitle).toBeVisible();
  });

  test('ADM-LOGIN-002: Login with invalid credentials shows error', async ({ page }) => {
    const login = new LoginPage(page);
    await login.navigate();
    await login.loginAs('admin', 'wrongpassword');
    await login.expectLoginError();
    await expect(page).toHaveURL('/login');
  });

  test('ADM-LOGIN-003: Login with empty password stays on login page', async ({ page }) => {
    const login = new LoginPage(page);
    await login.navigate();
    await login.fillLogin('admin');
    await login.clickSubmit();
    await expect(page).toHaveURL('/login');
    await expect(login.submitButton).toBeVisible();
  });

  test('ADM-NAV-001: Direct URL access to /doctor redirects admin', async ({ page }) => {
    const login = new LoginPage(page);
    const admin = new AdminPage(page);
    await login.navigate();
    await login.loginAs('admin', 'admin123');
    await admin.expectUrl(/\/admin/);
    await page.goto('/doctor');
    await page.waitForLoadState('networkidle');
    await expect(admin.appBarTitle).toBeVisible({ timeout: 10000 });
  });

  test('ADM-TABLE-001: Doctors table shows all doctors', async ({ page }) => {
    const login = new LoginPage(page);
    const admin = new AdminPage(page);
    await login.navigate();
    await login.loginAs('admin', 'admin123');
    await admin.expectUrl(/\/admin/);
    await expect(admin.doctorsTable).toBeVisible({ timeout: 10000 });
    await admin.expectUserInTable(admin.doctorsTable, 'doctor1');
    await admin.expectUserInTable(admin.doctorsTable, 'doctor2');
    await admin.expectUserInTable(admin.doctorsTable, 'Олександр Мельник');
    await admin.expectUserInTable(admin.doctorsTable, 'Наталія Бойко');
  });

  test('ADM-TABLE-002: Nurses table shows all nurses', async ({ page }) => {
    const login = new LoginPage(page);
    const admin = new AdminPage(page);
    await login.navigate();
    await login.loginAs('admin', 'admin123');
    await admin.expectUrl(/\/admin/);
    await expect(admin.nursesTable).toBeVisible({ timeout: 10000 });
    await admin.expectUserInTable(admin.nursesTable, 'nurse1');
    await admin.expectUserInTable(admin.nursesTable, 'nurse2');
    await admin.expectUserInTable(admin.nursesTable, 'Олена Ткаченко');
    await admin.expectUserInTable(admin.nursesTable, 'Марія Кравчук');
  });

  test('ADM-MENU-001: User menu shows profile and logout', async ({ page }) => {
    const login = new LoginPage(page);
    const admin = new AdminPage(page);
    await login.navigate();
    await login.loginAs('admin', 'admin123');
    await admin.expectUrl(/\/admin/);
    await admin.openUserMenu();
    await expect(admin.userNameMenuItem).toBeVisible();
    await expect(admin.logoutMenuItem).toBeVisible();
  });

  test('ADM-LOGOUT-001: Logout redirects to login page', async ({ page }) => {
    const login = new LoginPage(page);
    const admin = new AdminPage(page);
    await login.navigate();
    await login.loginAs('admin', 'admin123');
    await admin.expectUrl(/\/admin/);
    await admin.clickLogout();
    await expect(page).toHaveURL('/login');
    await login.expectLoginFormVisible();
  });
});
