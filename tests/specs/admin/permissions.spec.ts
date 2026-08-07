import { test, expect } from '../../fixtures/index';
import type { APIRequestContext } from '@playwright/test';

// Admin role & permission management (dynamic RBAC matrix).
// Runs in the admin-chromium project (storageState = admin).
// Serialized: the grant/revoke test mutates the matrix and must not race with the read-only UI test.

test.describe.configure({ mode: 'serial' });

const ADMIN = { login: 'admin', password: 'admin123' };
const NURSE = { login: 'nurse1', password: 'nurse123' };

async function login(request: APIRequestContext, creds: { login: string; password: string }) {
  const res = await request.post('/api/auth/login', { data: creds });
  expect(res.ok()).toBeTruthy();
  return { Authorization: `Bearer ${(await res.json()).token}` };
}

async function setNurseEpisodeCreate(request: APIRequestContext, headers: Record<string, string>, granted: boolean) {
  const res = await request.put('/api/admin/permissions', {
    headers,
    data: { role: 'NURSE', permissionCode: 'EPISODE_CREATE', granted },
  });
  expect(res.ok()).toBeTruthy();
}

test.describe('Role & permission management', () => {
  test('admin can view the access matrix and the defaults match the specification', async ({ page }) => {
    await page.goto('/admin');
    await page.getByRole('tab', { name: 'Доступи та ролі' }).click();

    await expect(page.getByText('Матриця доступів')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('columnheader', { name: 'Лікар' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Адміністратор протезування' })).toBeVisible();

    // Operation rows from the specification
    await expect(page.getByText('Створення епізоду')).toBeVisible();
    await expect(page.getByText('Створення клінічного дня')).toBeVisible();
    await expect(page.getByText('Підпис медсестрою')).toBeVisible();
    await expect(page.getByText('Рішення quality gate')).toBeVisible();
    await expect(page.getByText('Керування шаблонами')).toBeVisible();

    // Default matrix: DOCTOR may create episodes, NURSE may not
    await expect(page.getByRole('checkbox', { name: 'Створення епізоду — Лікар', exact: true })).toBeChecked();
    await expect(page.getByRole('checkbox', { name: 'Створення епізоду — Медсестра', exact: true })).not.toBeChecked();

    // Admin cannot create episodes (matrix) and has audit access
    await expect(page.getByRole('checkbox', { name: 'Створення епізоду — Адміністратор', exact: true })).not.toBeChecked();
    await expect(page.getByRole('checkbox', { name: 'Журнал аудиту — Адміністратор', exact: true })).toBeChecked();

    // Save button disabled while nothing is dirty
    await expect(page.getByRole('button', { name: 'Зберегти зміни' })).toBeDisabled();
  });

  test('grant and revoke of a permission changes backend enforcement', async ({ request }) => {
    const adminHeaders = await login(request, ADMIN);
    const nurseHeaders = await login(request, NURSE);

    // Ensure a clean baseline (idempotent)
    await setNurseEpisodeCreate(request, adminHeaders, false);

    // Valid request body: argument validation runs before method security, so an
    // invalid body would yield 400 (validation) instead of 403 (denied).
    const createBody = { patientId: 1, admissionDate: '2026-08-07T10:00:00' };

    // Baseline: nurse is blocked by the matrix
    const before = await request.post('/api/episodes', { headers: nurseHeaders, data: createBody });
    expect(before.status()).toBe(403);

    // Grant EPISODE_CREATE to NURSE via the admin matrix API
    await setNurseEpisodeCreate(request, adminHeaders, true);

    // The request now passes security end to end
    const granted = await request.post('/api/episodes', { headers: nurseHeaders, data: createBody });
    expect(granted.status()).toBe(201);

    // Revoke again → blocked
    await setNurseEpisodeCreate(request, adminHeaders, false);
    const after = await request.post('/api/episodes', { headers: nurseHeaders, data: createBody });
    expect(after.status()).toBe(403);
  });

  test.afterEach(async ({ request }) => {
    // Restore the default matrix even if the test failed mid-way
    const adminHeaders = await login(request, ADMIN);
    await setNurseEpisodeCreate(request, adminHeaders, false);
  });
});
