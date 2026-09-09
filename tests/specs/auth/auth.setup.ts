import { test as setup } from '@playwright/test';
import { mkdirSync, existsSync } from 'fs';
import {
  AD_ROLE_MATRIX,
  adCredentials,
  apiLoginEnsureRole,
} from '../../helpers/ad-auth';
import { testUser } from '../../helpers/test-users';

const AUTH_BASE = '.auth';
if (!existsSync(AUTH_BASE)) mkdirSync(AUTH_BASE, { recursive: true });

const USERS = [
  { ...testUser(1), file: 'doctor.json' },
  { ...testUser(3), file: 'nurse.json' },
  { ...testUser(5), file: 'hod.json' },
  { ...testUser(6), file: 'admin.json' },
  { ...testUser(7), file: 'prosthetist.json' },
  { ...testUser(9), file: 'prosthetics_admin.json' },
];

for (const user of USERS) {
  setup(`authenticate as ${user.login}`, async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Логін').fill(user.login);
    await page.getByLabel('Пароль').fill(user.password);
    await page.getByRole('button', { name: 'Увійти' }).click();
    await page.waitForURL(/\/(doctor|nurse|admin|select|prescriptions|prosthetics)/, { timeout: 60000 });
    await page.context().storageState({ path: `${AUTH_BASE}/${user.file}` });
  });
}

// ---- Active Directory identities (issue #250, local-only) ----
// Runs only when APP_TEST_USERNAME*/PASSWORD* are configured (corporate
// network); in CI the loop below registers zero tests and the seed states
// above remain the whole auth infrastructure. API-based login keeps
// credentials out of UI fills, and the setup project runs with trace off,
// so passwords never land in trace files either.
AD_ROLE_MATRIX.forEach(({ role }, index) => {
  const creds = adCredentials(index + 1);
  if (!creds) return;
  const { login, password } = creds;
  setup(`authenticate AD identity ${index + 1} as ${role}`, async ({ page, request }) => {
    const jwt = await apiLoginEnsureRole(request, login, password, role);
    // Session flag is required alongside the cookie (AuthContext restores
    // sessions only when it is present); it is stored into the saved state.
    await page.addInitScript(() => {
      window.localStorage.setItem('auth:session', '1');
    });
    await page.context().addCookies([
      { name: 'jwt', value: jwt, domain: 'localhost', path: '/' },
    ]);
    await page.goto('/select');
    await page.waitForURL(/\/select/, { timeout: 60000 });
    const file = AD_ROLE_MATRIX[index].state;
    await page.context().storageState({ path: `${AUTH_BASE}/${file}` });
  });
});
