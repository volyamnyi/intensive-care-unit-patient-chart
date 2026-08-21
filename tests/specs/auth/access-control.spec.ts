import { test, expect } from '../../fixtures/index';

// RBAC route-guard coverage. The Guard component redirects an authenticated user
// to '/' (→ RoleRedirect → /select) when the route is not allowed for their role,
// and unauthenticated visitors to /login. Per-test storageState keeps the login
// form out of the picture — it is covered by login.spec.ts/logout.spec.ts.
//
// Redirect target verified against App.tsx: Guard.navigate('/') + RoleRedirect
// → /select. Module permissions never defeat role-scoped sibling exclusions
// (excludeRoles) — asserted in the DOCTOR vs /icu/nurse test.

test.describe('Access Control', () => {
  test.describe('ADMINISTRATOR', () => {
    test.use({ storageState: '.auth/admin.json' });

    test('admin can open /admin', async ({ page }) => {
      await page.goto('/admin');
      await expect(page.getByRole('tab', { name: 'Користувачі' })).toBeVisible();
    });

    test('admin is redirected from /icu/nurse to /select', async ({ page }) => {
      await page.goto('/icu/nurse');
      await expect(page).toHaveURL(/\/select/);
    });

    test('admin is redirected from /icu/doctor to /select', async ({ page }) => {
      await page.goto('/icu/doctor');
      await expect(page).toHaveURL(/\/select/);
    });
  });

  test.describe('DOCTOR', () => {
    test.use({ storageState: '.auth/doctor.json' });

    test('doctor can open /icu/doctor/create-card directly', async ({ page }) => {
      await page.goto('/icu/doctor/create-card');
      await expect(page.getByText('Нова карта інтенсивної терапії')).toBeVisible();
    });

    test('doctor is redirected from /icu/nurse even with the module permission', async ({ page }) => {
      // DOCTOR holds MODULE_ICU_ACCESS by default, but the nurse route excludes
      // DOCTOR — the role-scoped sibling stays exclusive.
      await page.goto('/icu/nurse');
      await expect(page).toHaveURL(/\/select/);
    });
  });

  test.describe('NURSE', () => {
    test.use({ storageState: '.auth/nurse.json' });

    test('nurse can open /icu/nurse/episode/:id directly', async ({ page }) => {
      await page.goto('/icu/nurse/episode/a3333333-3333-3333-3333-333333333333');
      await expect(page.getByText('Показник / година')).toBeVisible();
    });

    test('nurse is redirected from /icu/doctor to /select', async ({ page }) => {
      await page.goto('/icu/doctor');
      await expect(page).toHaveURL(/\/select/);
    });
  });
});