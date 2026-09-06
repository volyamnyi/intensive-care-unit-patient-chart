import { test as setup } from '@playwright/test';
import { mkdirSync, existsSync } from 'fs';
import {
  AD_ROLE_MATRIX,
  adCredentials,
  apiLoginEnsureRole,
} from '../../helpers/ad-auth';

const AUTH_BASE = '.auth';
if (!existsSync(AUTH_BASE)) mkdirSync(AUTH_BASE, { recursive: true });

const USERS = [
  { login: 'doctor1', password: 'doctor123', file: 'doctor.json' },
  { login: 'nurse1', password: 'nurse123', file: 'nurse.json' },
  { login: 'head1', password: 'head123', file: 'hod.json' },
  { login: 'admin', password: 'admin123', file: 'admin.json' },
  { login: 'prosthetist1', password: 'doctor123', file: 'prosthetist.json' },
  { login: 'prosthetics_admin1', password: 'doctor123', file: 'prosthetics_admin.json' },
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
