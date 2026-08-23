import { test, expect } from '../../fixtures/index';

// Phase C direct-URL authorization pins (issue #172), run in login-chromium
// (no storageState — each test performs its own UI login). Proves that the
// route guards honor both the role list and the dynamic matrix:
//  - ADMINISTRATOR reaches /admin by ROLE,
//  - NURSE is bounced from the doctor ICU view (role-scoped exclusivity),
//  - PROSTHETIST is bounced from /admin.
//
// AUDITOR direct-URL behavior is covered server-side in
// ClinicalRbacIntegrationTest (auditor1 exists only in integration seeds).
//
// Guard rejections navigate to '/', which RoleRedirect immediately turns
// into '/select' — so a bounce always lands on the app chooser.

async function uiLogin(page: import('@playwright/test').Page, login: string, password: string) {
  await page.goto('/login');
  await page.getByLabel('Логін').fill(login);
  await page.getByLabel('Пароль').fill(password);
  await page.getByRole('button', { name: 'Увійти' }).click();
  await expect(page).toHaveURL(/\/select/, { timeout: 15000 });
}

test.describe('Direct-URL redirects per role', () => {
  test('ADMINISTRATOR can open /admin directly', async ({ page }) => {
    await uiLogin(page, 'admin', 'admin123');
    await page.goto('/admin');
    await expect(page).toHaveURL(/\/admin/);
  });

  test('NURSE is redirected away from /icu/doctor', async ({ page }) => {
    await uiLogin(page, 'nurse1', 'nurse123');
    await page.goto('/icu/doctor');
    await expect(page).toHaveURL(/\/select/, { timeout: 10000 });
  });

  test('PROSTHETIST is redirected away from /admin', async ({ page }) => {
    await uiLogin(page, 'prosthetist1', 'doctor123');
    await page.goto('/admin');
    await expect(page).toHaveURL(/\/select/, { timeout: 10000 });
  });
});
