import { test, expect, type Locator } from '@playwright/test';
import { testUser } from '../../helpers/test-users';

// Responsive UI Phase 6 (issue #165): on touch (coarse-pointer) contexts every
// primary CTA must be at least 44x44 px — enforced via pointer-coarse:min-h-11 /
// pointer-coarse:size-11 on ui Button and explicit min-h-[44px] elsewhere.

const API = 'http://localhost:8085/api';

async function getToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: testUser(1).login, password: testUser(1).password },
  });
  return (await res.json()).token as string;
}

// create-card's PatientSearch searches the dept-19 ICU roster from real MIS, so the
// CTA must be revealed by a real patient (no hardcoded mock name).
async function firstIcuPatient(request: any): Promise<{ fullName: string; query: string }> {
  const token = await getToken(request);
  const res = await request.get(`${API}/patients?module=icu`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  const patients = await res.json();
  const p = patients.find((x: any) => typeof x?.fullName === 'string' && x.fullName.trim().length >= 2);
  if (!p) {
    throw new Error('No ICU (dept 19) patient with a ≥2-char full name available from real MIS');
  }
  return { fullName: p.fullName.trim(), query: p.fullName.trim().slice(0, 4) };
}

// PatientSearch dropdown options are `div` wrappers whose first line is the full name.
function patientOption(page: any, fullName: string) {
  return page.locator('div:has(> p.font-semibold)').filter({ hasText: fullName }).first();
}

async function expectTouchTarget(locator: Locator, label: string) {
  const box = await locator.boundingBox();
  expect(box, `${label}: element has no bounding box`).not.toBeNull();
  // 0.5px tolerance absorbs subpixel rendering rounding (43.99998… on some runs)
  expect(box!.width, `${label}: width ${box!.width}px < 44`).toBeGreaterThanOrEqual(43.5);
  expect(box!.height, `${label}: height ${box!.height}px < 44`).toBeGreaterThanOrEqual(43.5);
}

test.describe('touch targets — doctor', () => {
  test('dashboard and create-card CTAs are at least 44px', async ({ page, request }) => {
    await page.goto('/icu/doctor');
    await expectTouchTarget(page.getByRole('button', { name: 'Нова карта' }), 'Нова карта');

    // The create-card form (and its submit button) renders after a patient is
    // picked; drive the PatientSearch with a real dept-19 patient from MIS.
    const patient = await firstIcuPatient(request);
    await page.goto('/icu/doctor/create-card');
    await page.getByLabel('ПІБ, телефон або № медкарти').fill(patient.query);
    const option = patientOption(page, patient.fullName);
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();
    await expect(
      page.getByRole('button', { name: 'Створити карту' }),
      'create-card form did not render after patient selection',
    ).toBeVisible();
    await expectTouchTarget(
      page.getByRole('button', { name: 'Створити карту' }),
      'Створити карту',
    );
  });

  test('patient-panel toggle on the episode page is at least 44px', async ({ page }) => {
    await page.goto('/icu/doctor/episode/a3333333-3333-3333-3333-333333333333');
    const panelButton = page.getByRole('button', { name: 'Панель пацієнта' });
    await expect(panelButton).toBeVisible({ timeout: 15000 });
    await expectTouchTarget(panelButton, 'Панель пацієнта');
  });
});

test.describe('touch targets — nurse', () => {
  test.use({ storageState: '.auth/nurse.json' });

  test('episode row action is at least 44px', async ({ page }) => {
    await page.goto('/icu/nurse');
    const open = page.getByRole('button', { name: 'Відкрити' }).first();
    await expect(open).toBeVisible();
    await expectTouchTarget(open, 'Відкрити');
  });
});

test.describe('touch targets — prosthetist', () => {
  test.use({ storageState: '.auth/prosthetist.json' });

  test('dashboard and setup CTAs are at least 44px', async ({ page }) => {
    await page.goto('/prosthetics');
    await expectTouchTarget(page.getByRole('button', { name: 'Новий процес' }), 'Новий процес');

    await page.goto('/prosthetics/new/select-patient');
    await expectTouchTarget(page.getByRole('button', { name: 'Далі' }), 'Далі');
  });
});
