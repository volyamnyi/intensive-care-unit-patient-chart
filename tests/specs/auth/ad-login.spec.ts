import { test, expect } from '../../fixtures/index';
import type { APIRequestContext, Page } from '@playwright/test';
import {
  adCredentials,
  apiLogin,
  hasAdEnv,
  jwtFromLogin,
  seedAdminHeaders,
  setUserRole,
} from '../../helpers/ad-auth';

// Local-only AD suite (issue #250): corporate AD is unreachable from CI,
// so the whole file skips without APP_TEST_USERNAME1/APP_TEST_PASSWORD1.
// No passwords in traces: tracing is off for this file (UI fills and API
// payloads stay out of trace files); failure screenshots show masked dots.
test.use({ trace: 'off' });

/** API login + browser session (no UI fill, no password in DOM). */
async function apiSession(page: Page, request: APIRequestContext, login: string, password: string) {
  const res = await apiLogin(request, login, password);
  expect(res.status()).toBe(200);
  const jwt = jwtFromLogin(res);
  expect(jwt).toBeTruthy();
  // The app restores sessions only with the session flag present (AuthContext);
  // the cookie alone is not enough and the app would bounce back to /login.
  await page.addInitScript(() => {
    window.localStorage.setItem('auth:session', '1');
  });
  await page.context().addCookies([{ name: 'jwt', value: jwt as string, domain: 'localhost', path: '/' }]);
}

function credsOrThrow(i: number): { login: string; password: string } {
  const creds = adCredentials(i);
  if (!creds) throw new Error(`APP_TEST_USERNAME${i}/APP_TEST_PASSWORD${i} are required`);
  return creds;
}

test.describe.serial('AD authentication (corporate directory, local-only)', () => {
  test.skip(!hasAdEnv(), 'Requires APP_TEST_USERNAME1/APP_TEST_PASSWORD1 (corporate AD)');

  test('successful AD login via UI reaches the app selector with modules', async ({ page }) => {
    const { login, password } = credsOrThrow(1);
    await page.goto('/login');
    await page.getByLabel('Логін').fill(login);
    await page.getByLabel('Пароль').fill(password);
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page).toHaveURL(/\/select/);
    // The module title also appears in the sidebar, so assert via the card's
    // unique subtitle and prove role plumbing by navigating through the card.
    await expect(page.getByText(/не має доступу до жодного модуля/)).not.toBeVisible();
    await page.getByText('Відділення анестезіології та інтенсивної терапії').click();
    await expect(page).toHaveURL(/\/icu\/doctor/);
  });

  test('invalid password shows the generic error', async ({ page }) => {
    // Single attempt: a typo-strength failure cannot trip AD lockout policies.
    const { login } = credsOrThrow(1);
    await page.goto('/login');
    await page.getByLabel('Логін').fill(login);
    await page.getByLabel('Пароль').fill('definitely-wrong');
    await page.getByRole('button', { name: 'Увійти' }).click();
    await expect(page.getByText('Невірний логін або пароль')).toBeVisible();
    await expect(page).toHaveURL('/login');
  });

  test('unknown login throttles to 429 without touching any AD account', async ({ request }) => {
    // Fixed nonexistent login: failures never map to a real AD account, so no
    // corporate lockout risk; the 429 comes from the backend throttle alone.
    // Lockout windows grow exponentially, so consecutive attempts observe a
    // block deterministically without sleeps.
    const probe = 'ad-unknown-probe-1';
    let blocked = false;
    for (let i = 0; i < 10 && !blocked; i++) {
      const res = await apiLogin(request, probe, 'definitely-wrong');
      expect([401, 429]).toContain(res.status());
      blocked = res.status() === 429;
    }
    expect(blocked).toBe(true);
  });

  test('demoted identity behaves as GUEST with zero modules', async ({ page, request }) => {
    const { login, password } = credsOrThrow(9);
    const adminHeaders = await seedAdminHeaders(request);
    await setUserRole(request, adminHeaders, login, 'GUEST');

    await apiSession(page, request, login, password);
    await page.goto('/select');
    await expect(page.getByText(/не має доступу до жодного модуля/)).toBeVisible();
    await expect(page.getByText('Відділення анестезіології та інтенсивної терапії')).not.toBeVisible();

    await page.goto('/icu/doctor');
    await expect(page).toHaveURL(/\/select/);

    const perms = await request.get('http://localhost:8085/api/users/me/permissions', {
      headers: { Cookie: `jwt=${(await page.context().cookies()).find((c) => c.name === 'jwt')?.value}` },
    });
    expect(perms.status()).toBe(200);
    expect(await perms.json()).toEqual([]);
  });

  test('doctor session reaches the ICU module and survives reload', async ({ page, request }) => {
    const { login, password } = credsOrThrow(1);
    await apiSession(page, request, login, password);
    await page.goto('/icu/doctor');
    await expect(page).toHaveURL(/\/icu\/doctor/);
    await page.reload();
    await expect(page).toHaveURL(/\/icu\/doctor/);
  });

  test('nurse session reaches the prescriptions module', async ({ page, request }) => {
    const { login, password } = credsOrThrow(3);
    await apiSession(page, request, login, password);
    await page.goto('/prescriptions/nurse');
    await expect(page).toHaveURL(/\/prescriptions\/nurse/);
  });

  test('admin session reaches the admin module', async ({ page, request }) => {
    const { login, password } = credsOrThrow(6);
    await apiSession(page, request, login, password);
    await page.goto('/admin');
    await expect(page).toHaveURL(/\/admin/);
  });

  test('prosthetist session reaches the prosthetics module', async ({ page, request }) => {
    const { login, password } = credsOrThrow(7);
    await apiSession(page, request, login, password);
    await page.goto('/prosthetics');
    await expect(page).toHaveURL(/\/prosthetics/);
  });

  test('logout invalidates the AD session', async ({ page, request }) => {
    const { login, password } = credsOrThrow(1);
    await apiSession(page, request, login, password);
    await page.goto('/select');
    await expect(page).toHaveURL(/\/select/);
    await page.getByRole('button', { name: 'Меню користувача' }).click();
    await page.getByRole('menuitem', { name: 'Вийти' }).click();
    await expect(page).toHaveURL('/login');
    await page.goto('/icu/doctor');
    await expect(page).toHaveURL('/login');
  });

  test('no credentials leak into web storage', async ({ page, request }) => {
    const { login, password } = credsOrThrow(1);
    await apiSession(page, request, login, password);
    await page.goto('/select');
    // Structural assertions only (patterns, never values): a failure cannot
    // print a password or token into the report. The app legitimately keeps
    // UI preferences (e.g. themeMode) alongside the session flag.
    const keys = await page.evaluate(() => ({
      local: Object.keys(localStorage),
      session: Object.keys(sessionStorage),
    }));
    expect(keys.local).toContain('auth:session');
    expect(keys.local.every((k) => !/token|jwt|password|secret|credential/i.test(k))).toBe(true);
    expect(keys.session).toEqual([]);
  });
});
