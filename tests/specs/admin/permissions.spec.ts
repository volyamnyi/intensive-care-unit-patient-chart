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

async function setDoctorModuleAccess(request: APIRequestContext, headers: Record<string, string>, granted: boolean) {
  const res = await request.put('/api/admin/permissions', {
    headers,
    data: { role: 'DOCTOR', permissionCode: 'MODULE_PROSTHETICS_ACCESS', granted },
  });
  expect(res.ok()).toBeTruthy();
}

async function setAdminModuleAccess(request: APIRequestContext, headers: Record<string, string>, code: string, granted: boolean) {
  const res = await request.put('/api/admin/permissions', {
    headers,
    data: { role: 'ADMINISTRATOR', permissionCode: code, granted },
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

    // Module-navigation rows: default matrix grants prosthetics module to prosthetics roles only
    await expect(page.getByText('Модуль: Виробництво протезів')).toBeVisible();
    await expect(page.getByRole('checkbox', { name: 'Модуль: Виробництво протезів — Протезист', exact: true })).toBeChecked();
    await expect(page.getByRole('checkbox', { name: 'Модуль: Виробництво протезів — Лікар', exact: true })).not.toBeChecked();
    await expect(page.getByRole('checkbox', { name: 'Модуль: Адміністрування — Адміністратор', exact: true })).toBeChecked();

    // Default matrix: clinical modules are NOT granted to ADMINISTRATOR — the
    // checkbox in the matrix is exactly what opens them (module-visit permission).
    await expect(page.getByRole('checkbox', { name: 'Модуль: Карта інтенсивної терапії — Адміністратор', exact: true })).not.toBeChecked();
    await expect(page.getByRole('checkbox', { name: 'Модуль: Листок лікарських призначень — Адміністратор', exact: true })).not.toBeChecked();

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

    // Retry-safe cleanup: an ACTIVE episode for the patient (left by any
    // earlier attempt) makes every create 422 EpisodeAlreadyActive regardless
    // of the matrix. Close leftovers before and after the grant. Uses DOCTOR
    // credentials — /api/episodes reads are clinical-core-gated and admins
    // hold neither the role nor MODULE_ICU_ACCESS by default.
    const doctorHeaders = await login(request, { login: 'doctor1', password: 'doctor123' });
    const closeActiveEpisodes = async () => {
      const list = await request.get('/api/episodes?patientId=1&status=ACTIVE', {
        headers: doctorHeaders,
      });
      if (!list.ok()) return;
      for (const ep of (await list.json()) as Array<{ id: string; version: number }>) {
        await request.post(`/api/episodes/${ep.id}/close`, {
          headers: doctorHeaders,
          data: { dischargeDate: new Date().toISOString().slice(0, 19), version: ep.version },
        });
      }
    };
    await closeActiveEpisodes();

    // Ensure a clean baseline (idempotent)
    await setNurseEpisodeCreate(request, adminHeaders, false);

    // Valid request body: argument validation runs before method security, so an
    // invalid body would yield 400 (validation) instead of 403 (denied).
    const createBody = { patientId: 1, admissionDate: new Date().toISOString().slice(0, 19) };

    // Baseline: nurse is blocked by the matrix
    const before = await request.post('/api/episodes', { headers: nurseHeaders, data: createBody });
    expect(before.status()).toBe(403);

    // Grant EPISODE_CREATE to NURSE via the admin matrix API
    await setNurseEpisodeCreate(request, adminHeaders, true);

    // The request now passes security end to end
    const granted = await request.post('/api/episodes', { headers: nurseHeaders, data: createBody });
    if (granted.status() !== 201) {
      if (granted.status() !== 201) {
      throw new Error(
        'episode create returned ' + granted.status() + ': ' + (await granted.text()),
      );
    }
    }
    await closeActiveEpisodes();

    // Revoke again → blocked
    await setNurseEpisodeCreate(request, adminHeaders, false);
    const after = await request.post('/api/episodes', { headers: nurseHeaders, data: createBody });
    expect(after.status()).toBe(403);
  });

  test('granting the prosthetics module permission to DOCTOR reveals the module in the sidebar', async ({ request, browser }) => {
    const adminHeaders = await login(request, ADMIN);
    await setDoctorModuleAccess(request, adminHeaders, true);

    // Fresh doctor context: permissions are re-fetched on page load, so the grant applies.
    const ctx = await browser.newContext({ storageState: '.auth/doctor.json' });
    const page = await ctx.newPage();
    try {
      await page.goto('/select');
      const sidebarLink = page.getByRole('link', { name: 'Виробництво протезів' });
      await expect(sidebarLink).toBeVisible({ timeout: 10000 });

      // Navigation into the module works end to end (route guard + read APIs).
      await sidebarLink.click();
      await expect(page.getByRole('heading', { name: 'Виробництво протезів' })).toBeVisible({ timeout: 10000 });
    } finally {
      await ctx.close();
      await setDoctorModuleAccess(request, adminHeaders, false);
    }
  });

  test('granting the ICU module permission to ADMINISTRATOR reveals the module and allows navigation', async ({ request, browser }) => {
    const adminHeaders = await login(request, ADMIN);
    await setAdminModuleAccess(request, adminHeaders, 'MODULE_ICU_ACCESS', true);

    // Fresh admin context: permissions are re-fetched on page load, so the grant applies.
    const ctx = await browser.newContext({ storageState: '.auth/admin.json' });
    const page = await ctx.newPage();
    try {
      await page.goto('/select');
      const sidebarLink = page.getByRole('link', { name: 'Карта інтенсивної терапії' });
      await expect(sidebarLink).toBeVisible({ timeout: 10000 });

      // The admin lands on the doctor view of the module (route guard + read APIs).
      await sidebarLink.click();
      await expect(page.getByRole('heading', { name: 'Активні пацієнти' })).toBeVisible({ timeout: 10000 });
    } finally {
      await ctx.close();
      await setAdminModuleAccess(request, adminHeaders, 'MODULE_ICU_ACCESS', false);
    }
  });

  test('granting the medication module permission to ADMINISTRATOR allows navigation into it', async ({ request, browser }) => {
    const adminHeaders = await login(request, ADMIN);
    await setAdminModuleAccess(request, adminHeaders, 'MODULE_MEDICATION_ACCESS', true);

    const ctx = await browser.newContext({ storageState: '.auth/admin.json' });
    const page = await ctx.newPage();
    try {
      await page.goto('/select');
      const sidebarLink = page.getByRole('link', { name: 'Листок лікарських призначень' });
      await expect(sidebarLink).toBeVisible({ timeout: 10000 });

      await sidebarLink.click();
      await expect(page.getByRole('heading', { name: 'Листок лікарських призначень' })).toBeVisible({ timeout: 10000 });
    } finally {
      await ctx.close();
      await setAdminModuleAccess(request, adminHeaders, 'MODULE_MEDICATION_ACCESS', false);
    }
  });

  test('UI matrix editor cycle on a non-MODULE code persists and flips enforcement', async ({ page, request }) => {
    const nurseHeaders = await login(request, NURSE);

    // Enforcement probe: clinical-note creation on the seeded OPEN day.
    // Notes carry no uniqueness constraints and stay writable on
    // NURSE_SIGNED days, so parallel projects writing hourly records on
    // shared days can never collide with this probe. The gate family is the
    // same: hasAny(SCALE_*, VITALS_ENTER).
    let noteSeq = 0;
    const postNote = async () => {
      const res = request.post('/api/clinical-days/b3333333-3333-3333-3333-333333333333/notes', {
        headers: nurseHeaders,
        data: { noteType: 'rbac-cycle', text: `probe-${Date.now()}-${noteSeq++}` },
      });
      return res;
    };

    // Baseline: NURSE holds SCALE_CAMICU_BRADEN_RASS / VITALS_ENTER by default.
    expect((await postNote()).status()).toBe(201);

    // Drive the UI matrix editor: uncheck «Введення показників — Медсестра» and save.
    await page.goto('/admin');
    await page.getByRole('tab', { name: 'Доступи та ролі' }).click();
    const row = page.getByRole('checkbox', { name: 'Введення показників — Медсестра', exact: true });
    await expect(row).toBeChecked({ timeout: 10000 });
    await row.uncheck();
    await page.getByRole('button', { name: 'Зберегти зміни' }).click();
    await expect(page.getByText(/Збережено змін/)).toBeVisible({ timeout: 10000 });

    // Reload proves persistence through the matrix table itself.
    await page.reload();
    await page.getByRole('tab', { name: 'Доступи та ролі' }).click();
    await expect(page.getByRole('checkbox', { name: 'Введення показників — Медсестра', exact: true }))
      .not.toBeChecked({ timeout: 10000 });

    // Enforcement flipped immediately for the same nurse credentials.
    expect((await postNote()).status()).toBe(403);

    // Re-check via the UI, save, enforcement restored.
    await row.check();
    await page.getByRole('button', { name: 'Зберегти зміни' }).click();
    await expect(page.getByText(/Збережено змін/)).toBeVisible({ timeout: 10000 });
    expect((await postNote()).status()).toBe(201);

    // The audit trail shows both sides of the cycle.
    await page.getByRole('tab', { name: 'Журнал аудиту' }).click();
    await expect(page.getByText(/PERMISSION_REVOKE/).first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(/PERMISSION_GRANT/).first()).toBeVisible({ timeout: 10000 });
  });

  test('doctor with prosthetics module access is denied instance creation (read-only split)', async ({ request }) => {
    const adminHeaders = await login(request, ADMIN);
    await setDoctorModuleAccess(request, adminHeaders, true);
    try {
      const doctorHeaders = await login(request, { login: 'doctor1', password: 'doctor123' });

      // Read surface stays open…
      const list = await request.get('/api/prosthesis-manufacturing/instances', { headers: doctorHeaders });
      expect(list.ok()).toBeTruthy();

      // …while the write path is denied even with a fully valid body
      // (seeded order ПВ-26-0413 + ACTIVE template TP-UL-01).
      const created = await request.post('/api/prosthesis-manufacturing/instances', {
        headers: doctorHeaders,
        data: {
          orderId: '20000000-0000-4000-8000-000000000001',
          templateId: 'c0000001-0000-0000-0000-000000000001',
        },
      });
      expect(created.status()).toBe(403);
    } finally {
      await setDoctorModuleAccess(request, adminHeaders, false);
    }
  });

  test.afterEach(async ({ request }) => {
    // Restore the default matrix even if the test failed mid-way
    const adminHeaders = await login(request, ADMIN);
    await setNurseEpisodeCreate(request, adminHeaders, false);
    await setAdminModuleAccess(request, adminHeaders, 'MODULE_ICU_ACCESS', false);
    await setAdminModuleAccess(request, adminHeaders, 'MODULE_MEDICATION_ACCESS', false);
  });
});
